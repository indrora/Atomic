/*
Yaaic - Yet Another Android IRC Client

Copyright 2009-2013 Sebastian Kaspari

This file is part of Yaaic.

Yaaic is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

Yaaic is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with Yaaic.  If not, see <http://www.gnu.org/licenses/>.
 */
package indrora.atomic.activity;

import indrora.atomic.App;
import indrora.atomic.Atomic;
import indrora.atomic.R;
import indrora.atomic.adapter.ConversationPagerAdapter;
import indrora.atomic.adapter.MessageListAdapter;
import indrora.atomic.command.CommandParser;
import indrora.atomic.indicator.ConversationIndicator;
import indrora.atomic.indicator.ConversationTitlePageIndicator.IndicatorStyle;
import indrora.atomic.irc.IRCBinder;
import indrora.atomic.irc.IRCConnection;
import indrora.atomic.irc.IRCService;
import indrora.atomic.listener.ConversationListener;
import indrora.atomic.listener.ServerListener;
import indrora.atomic.listener.SpeechClickListener;
import indrora.atomic.model.Broadcast;
import indrora.atomic.model.ColorScheme;
import indrora.atomic.model.Conversation;
import indrora.atomic.model.Extra;
import indrora.atomic.model.Message;
import indrora.atomic.model.Query;
import indrora.atomic.model.Scrollback;
import indrora.atomic.model.Server;
import indrora.atomic.model.ServerInfo;
import indrora.atomic.model.Settings;
import indrora.atomic.model.Status;
import indrora.atomic.receiver.ConversationReceiver;
import indrora.atomic.receiver.ServerReceiver;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

import org.jibble.pircbot.NickConstants;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.IBinder;
import android.speech.RecognizerIntent;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.text.InputType;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.method.TextKeyListener;
import android.text.style.ForegroundColorSpan;
import android.text.style.ImageSpan;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnKeyListener;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

/**
 * The server view with a scrollable list of all channels
 *
 * @author Sebastian Kaspari <sebastian@yaaic.org>
 */
public class ConversationActivity extends Activity implements
    ServiceConnection, ServerListener, ConversationListener,
    OnPageChangeListener {
  public static final int REQUEST_CODE_SPEECH = 99;

  private static final int REQUEST_CODE_JOIN = 1;
  @SuppressWarnings("unused")
  private static final int REQUEST_CODE_USERS = 2;
  @SuppressWarnings("unused")
  private static final int REQUEST_CODE_USER = 3;
  private static final int REQUEST_CODE_NICK_COMPLETION = 4;

  public static final String EXTRA_TARGET = "target";

  private static ColorScheme _scheme;

  public static ColorScheme getScheme() {
    return _scheme;
  }

  private int serverId;
  private Server server;
  private IRCBinder binder;
  private ConversationReceiver channelReceiver;
  private ServerReceiver serverReceiver;

  private ViewPager pager;
  private ConversationIndicator indicator;
  private ConversationPagerAdapter pagerAdapter;

  private Scrollback scrollback;

  // XXX: This is ugly. This is a buffer for a channel that should be joined
  // after showing the
  // JoinActivity. As onActivityResult() is called before onResume() a
  // "channel joined"
  // broadcast may get lost as the broadcast receivers are registered in
  // onResume() but the
  // join command would be called in onActivityResult(). joinChannelBuffer
  // will save the
  // channel name in onActivityResult() and run the join command in
  // onResume().
  private String joinChannelBuffer;

  private int historySize;

  private boolean reconnectDialogActive = false;

  private final OnKeyListener inputKeyListener = new OnKeyListener() {
    /**
     * On key pressed (input line)
     */
    @Override
    public boolean onKey(View view, int keyCode, KeyEvent event) {
      EditText input = (EditText)view;

      if( event.getAction() != KeyEvent.ACTION_DOWN ) {
        return false;
      }

      if( keyCode == KeyEvent.KEYCODE_DPAD_UP ) {
        String message = scrollback.goBack();
        if( message != null ) {
          input.setText(message);
        }
        return true;
      }

      if( keyCode == KeyEvent.KEYCODE_DPAD_DOWN ) {
        String message = scrollback.goForward();
        if( message != null ) {
          input.setText(message);
        }
        return true;
      }

      if( keyCode == KeyEvent.KEYCODE_ENTER ) {
        sendMessage(input.getText().toString());

        // Workaround for
        // a race
        // condition in
        // EditText
        // Instead of
        // calling
        // input.setText("");
        // See:
        // -
        // https://github.com/pocmo/Yaaic/issues/67
        // -
        // http://code.google.com/p/android/issues/detail?id=17508
        TextKeyListener.clear(input.getText());

        return true;
      }

      // Nick completion
      if( keyCode == KeyEvent.KEYCODE_SEARCH || keyCode == KeyEvent.KEYCODE_TAB ) {
        doNickCompletion(input);
        return true;
      }

      return false;
    }
  };

  Settings settings;

  /**
   * On create
   */
  @Override
  protected void onCreate(Bundle savedInstanceState) {

    _scheme = App.getColorScheme();

    serverId = getIntent().getExtras().getInt("serverId");
    server = Atomic.getInstance().getServerById(serverId);
    settings = App.getSettings();
    if( settings.tintActionbar() ) {
      setTheme(settings.getUseDarkColors() ? indrora.atomic.R.style.AppThemeDark
          : indrora.atomic.R.style.AppThemeLight);
    } else {
      setTheme(indrora.atomic.R.style.AppThemeDark);
    }
    super.onCreate(savedInstanceState);

    // Finish activity if server does not exist anymore - See #55
    if( server == null ) {
      this.finish();
    }

    ActionBar actionBar = getActionBar();
    actionBar.setDisplayHomeAsUpEnabled(true);

    setTitle(server.getTitle());

    setContentView(R.layout.conversations);

    boolean isLandscape = (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE);

    EditText input = (EditText)findViewById(R.id.input);
    input.setOnKeyListener(inputKeyListener);

    // Fix from https://groups.google.com/forum/#!topic/yaaic/Z4bXZXvW7UM

    input.setOnClickListener(new EditText.OnClickListener() {
      public void onClick(View v) {
        openSoftKeyboard(v);
      }
    });

    pager = (ViewPager)findViewById(R.id.pager);

    pagerAdapter = new ConversationPagerAdapter(this, server);
    pager.setAdapter(pagerAdapter);

    final float density = getResources().getDisplayMetrics().density;

    indicator = (ConversationIndicator)findViewById(R.id.titleIndicator);
    indicator.setServer(server);
    indicator.setTypeface(Typeface.MONOSPACE);
    indicator.setViewPager(pager);

    indicator.setFooterColor(_scheme.getForeground());
    indicator.setFooterLineHeight(1 * density);
    indicator.setFooterIndicatorHeight(3 * density);
    indicator.setFooterIndicatorStyle(IndicatorStyle.Underline);
    indicator.setSelectedColor(_scheme.getForeground());
    indicator.setSelectedBold(true);
    indicator.setBackgroundColor(_scheme.getBackground());

    indicator.setVisibility(View.GONE);

    indicator.setOnPageChangeListener(this);

    historySize = settings.getHistorySize();

    if( server.getStatus() == Status.PRE_CONNECTING ) {
      server.clearConversations();
      pagerAdapter.clearConversations();
      server.getConversation(ServerInfo.DEFAULT_NAME).setHistorySize(
          historySize);
    }

    float fontSize = settings.getFontSize();
    indicator.setTextSize(fontSize * density);

    input.setTextSize(settings.getFontSize());
    input.setTypeface(Typeface.MONOSPACE);

    // Optimization : cache field lookups
    Collection<Conversation> mConversations = server.getConversations();

    for( Conversation conversation : mConversations ) {
      // Only scroll to new conversation if it was selected before
      if( conversation.getStatus() == Conversation.STATUS_SELECTED ) {
        onNewConversation(conversation.getName());
      } else {
        createNewConversation(conversation.getName());
      }
    }

    int setInputTypeFlags = 0;

    setInputTypeFlags |= InputType.TYPE_TEXT_FLAG_AUTO_CORRECT;

    if( settings.autoCapSentences() ) {
      setInputTypeFlags |= InputType.TYPE_TEXT_FLAG_CAP_SENTENCES;
    }

    if( isLandscape && settings.imeExtract() ) {
      setInputTypeFlags |= InputType.TYPE_TEXT_VARIATION_SHORT_MESSAGE;
    }

    if( !settings.imeExtract() ) {
      input.setImeOptions(input.getImeOptions()
          | EditorInfo.IME_FLAG_NO_EXTRACT_UI);
    }

    input.setInputType(input.getInputType() | setInputTypeFlags);
    this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
    // Add handling for tab-completing from the input box.

    int tabCompleteDrawableResource = (settings.getUseDarkColors() ? R.drawable.ic_tabcomplete_light
        : R.drawable.ic_tabcomplete_dark);
    // The drawable that makes up the actual clicky
    final Drawable tabcompleteDrawable = getResources().getDrawable(
        tabCompleteDrawableResource);
    // Set the bounds to the Intrinsic width
    // We'll resize this later (well, the input box will handle that for us)
    tabcompleteDrawable.setBounds(0, 0,
        tabcompleteDrawable.getIntrinsicWidth(),
        tabcompleteDrawable.getIntrinsicWidth());
    // Set the input compound drawables.
    input.setCompoundDrawables(null, null, tabcompleteDrawable, null);
    // Magic.
    final EditText tt = input;
    final ConversationActivity cv = this;
    input.setOnTouchListener(new View.OnTouchListener() {

      @Override
      public boolean onTouch(View v, MotionEvent event) {
        // This is where we handle some things.
        boolean tappedX = event.getX() > (tt.getWidth() - tt.getPaddingRight() - tabcompleteDrawable
            .getIntrinsicWidth());

        if( event.getAction() == MotionEvent.ACTION_UP && tappedX ) {
          cv.doNickCompletion(tt);
        } else {
          // Blarrarharhguhaguhaguhaeguahguh STFU linter.
          // :3
        }
        return false;
      }
    });

    setupColors();
    setupIndicator();

    // Create a new scrollback history
    scrollback = new Scrollback();

    if( getIntent().getExtras().containsKey(EXTRA_TARGET) ) {
      ShuffleToHighlight(getIntent());
    }
  }

  @Override
  protected void onNewIntent(Intent intent) {
    // Debugging: Blarg our new intent.
    for( String s : intent.getExtras().keySet() ) {
      Log.d("ConversationActivty",
          String.format("k=%s v=\"%s\"", s, intent.getExtras().get(s)));
    }

    // If we are not the intended server, we should swap to the intended server.
    if( intent.getExtras().getInt("serverId") != serverId ) {
      // Set the flag that lets us clear the top activity (killing ourselves,
      // but resurrecting after the jump)
      intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
      // Start the activity
      startActivity(intent);
      // And commit seppuku
      finish();
    }

    super.onNewIntent(intent);
    // If our new intent is to change to a target, do it.
    if( intent.getExtras().containsKey(ConversationActivity.EXTRA_TARGET) ) {
      ShuffleToHighlight(intent);
    }
  }

  private void ShuffleToHighlight(Intent inten) {
    // Try and find the conversation given in the intent.
    String convo = inten.getExtras().getString(
        ConversationActivity.EXTRA_TARGET);
    Log.d("ConversationActivity", "Trying to change to conversation " + convo);

    if( convo == null ) {
      Log.d("ConversationActivity", "Conversation given was NULL, jump invalid");
      return;
    }

    int convCount = pagerAdapter.getCount();

    for( int idx = 0; idx < convCount; idx++ ) {
      if( pagerAdapter.getItem(idx) == null )
        continue;
      String tConvo = pagerAdapter.getItem(idx).getName();
      Log.d("ConversationActivity", "is it " + tConvo + "?");
      if( tConvo.toLowerCase(Locale.US).equals(convo.toLowerCase(Locale.US)) ) {
        pager.setCurrentItem(idx, false);
        Log.d("ConversationActivity", "Found conversation " + tConvo);
        return;
      }
    }
    Log.d("ConversationActivity", "Didn't find conversation!?!?!!?");
  }

  private void setupColors() {
    if( settings.tintActionbar() ) {
      // the ActionBar can be tinted. This is really cool.
      // Get the ActionBar
      ActionBar ab = getActionBar();
      // Make its background drawable a ColorDrawable
      ab.setBackgroundDrawable(new ColorDrawable(App.getColorScheme()
          .getBackground()));
      // Create a SpannableString from the current server.
      SpannableString st = new SpannableString(server.getTitle());
      // Make its forground color (through a ForgroundColorSpan) to be the
      // foreground of the scheme.
      // This is because you can't guarantee that the ActionBar text color and
      // actionbar color aren't going to be the same.
      st.setSpan(new ForegroundColorSpan(App.getColorScheme().getForeground()),
          0, st.length(), SpannableString.SPAN_INCLUSIVE_INCLUSIVE);
      // Now, set our spannable to be the ActionBar title.
      ab.setTitle(st);
    } else {
      (getActionBar()).setTitle(server.getTitle());
    }
    EditText input = (EditText)findViewById(R.id.input);
    LinearLayout lll = (LinearLayout)(input.getParent());
    lll.setBackgroundColor(_scheme.getBackground());

    input.setTextColor(_scheme.getForeground());

  }

  /**
   * On resume
   */
  @SuppressLint("InlinedApi")
  @Override
  public void onResume() {
    // register the receivers as early as possible, otherwise we may drop a
    // broadcast message
    channelReceiver = new ConversationReceiver(server.getId(), this);
    registerReceiver(channelReceiver, new IntentFilter(
        Broadcast.CONVERSATION_MESSAGE));
    registerReceiver(channelReceiver, new IntentFilter(
        Broadcast.CONVERSATION_NEW));
    registerReceiver(channelReceiver, new IntentFilter(
        Broadcast.CONVERSATION_REMOVE));
    registerReceiver(channelReceiver, new IntentFilter(
        Broadcast.CONVERSATION_TOPIC));
    registerReceiver(channelReceiver, new IntentFilter(
        Broadcast.CONVERSATION_CLEAR));

    serverReceiver = new ServerReceiver(this);
    registerReceiver(serverReceiver, new IntentFilter(Broadcast.SERVER_UPDATE));

    super.onResume();

    // Start service
    Intent intent = new Intent(this, IRCService.class);
    intent.setAction(IRCService.ACTION_FOREGROUND);
    startService(intent);
    int flags = 0;
    if( android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH ) {
      flags |= Context.BIND_ABOVE_CLIENT;
      flags |= Context.BIND_IMPORTANT;
    }
    bindService(intent, this, flags);

    // Let's be explicit about this.
    ((EditText)findViewById(R.id.input)).setEnabled(true);

    // Optimization - cache field lookup
    Collection<Conversation> mConversations = server.getConversations();
    MessageListAdapter mAdapter;

    // Fill view with messages that have been buffered while paused
    for( Conversation conversation : mConversations ) {
      String name = conversation.getName();
      mAdapter = pagerAdapter.getItemAdapter(name);

      if( mAdapter != null ) {
        mAdapter.addBulkMessages(conversation.getBuffer());
        conversation.clearBuffer();
      } else {
        // Was conversation created while we were paused?
        if( pagerAdapter.getPositionByName(name) == -1 ) {
          onNewConversation(name);
        }
      }

      // Clear new message notifications for the selected conversation
      if( conversation.getStatus() == Conversation.STATUS_SELECTED
          && conversation.getNewMentions() > 0 ) {
        Intent ackIntent = new Intent(this, IRCService.class);
        ackIntent.setAction(IRCService.ACTION_ACK_NEW_MENTIONS);
        ackIntent.putExtra(IRCService.EXTRA_ACK_SERVERID, serverId);
        ackIntent.putExtra(IRCService.EXTRA_ACK_CONVTITLE, name);
        startService(ackIntent);
      }
    }

    // Remove views for conversations that ended while we were paused
    int numViews = pagerAdapter.getCount();
    if( numViews > mConversations.size() ) {
      for( int i = 0; i < numViews; ++i ) {
        if( !mConversations.contains(pagerAdapter.getItem(i)) ) {
          pagerAdapter.removeConversation(i--);
          --numViews;
        }
      }
    }

    // Join channel that has been selected in JoinActivity
    // (onActivityResult())
    if( joinChannelBuffer != null ) {
      new Thread() {
        @Override
        public void run() {
          binder.getService().getConnection(serverId)
              .joinChannel(joinChannelBuffer);
          joinChannelBuffer = null;
        }
      }.start();
    }

    setupColors();
    setupIndicator();

    server.setIsForeground(true);

    if( this.getIntent().hasExtra(ConversationActivity.EXTRA_TARGET) ) {
      Log.d("ConversationActivity",
          "onResume: " + (this.getIntent().getStringExtra(EXTRA_TARGET)));
    }

  }

  /**
   * On Pause
   */
  @Override
  public void onPause() {
    super.onPause();

    // Mark the current visible line.

    server.setIsForeground(false);

    if( binder != null && binder.getService() != null ) {
      binder.getService().checkServiceStatus();
    }

    unbindService(this);
    unregisterReceiver(channelReceiver);
    unregisterReceiver(serverReceiver);

    // Force the OSK to go away
    // This makes it so that if the implicit keyboard doesn't work, the explicit
    // one
    // will force close.
    EditText input = (EditText)findViewById(R.id.input);
    ((InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE))
        .hideSoftInputFromWindow(input.getWindowToken(), 0);
  }

  /**
   * On service connected
   */
  @Override
  public void onServiceConnected(ComponentName name, IBinder service) {
    this.binder = (IRCBinder)service;

    // connect to irc server if connect has been requested
    if( server.getStatus() == Status.PRE_CONNECTING
        && getIntent().hasExtra("connect") ) {
      server.setStatus(Status.CONNECTING);
      binder.connect(server);
    } else {
      onStatusUpdate();
    }
  }

  /**
   * On service disconnected
   */
  @Override
  public void onServiceDisconnected(ComponentName name) {
    this.binder = null;
  }

  /**
   * On options menu requested
   */
  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    super.onCreateOptionsMenu(menu);

    // inflate from xml
    MenuInflater inflater = new MenuInflater(this);
    inflater.inflate(R.menu.conversations, menu);

    return true;
  }

  /**
   * On menu item selected
   */
  @Override
  public boolean onMenuItemSelected(int featureId, MenuItem item) {
    switch ( item.getItemId() ) {
      case android.R.id.home:
        finish();
        break;

      case R.id.disconnect:
        server.setStatus(Status.DISCONNECTED);
        server.setMayReconnect(false);
        binder.getService().getConnection(serverId).quitServer();
        server.clearConversations();
        setResult(RESULT_OK);
        finish();
        break;

      case R.id.close:
        Conversation conversationToClose = pagerAdapter.getItem(pager
            .getCurrentItem());
        // Make sure we part a channel when closing the channel conversation
        if( conversationToClose.getType() == Conversation.TYPE_CHANNEL ) {
          IRCConnection conn = binder.getService().getConnection(serverId);
          conn.partChannel(conversationToClose.getName());
          // server.removeConversation(conversationToClose.getName());
          // onRemoveConversation(conversationToClose.getName());

        } else if( conversationToClose.getType() == Conversation.TYPE_QUERY ) {
          server.removeConversation(conversationToClose.getName());
          onRemoveConversation(conversationToClose.getName());
        } else {
          Toast.makeText(this,
              getResources().getString(R.string.close_server_window),
              Toast.LENGTH_SHORT).show();
        }
        break;

      case R.id.join:
        startActivityForResult(new Intent(this, JoinActivity.class),
            REQUEST_CODE_JOIN);
        break;

      /* Get users in the channel. */
      case R.id.users:
        Conversation conversationForUserList = pagerAdapter.getItem(pager
            .getCurrentItem());
        if( conversationForUserList.getType() == Conversation.TYPE_CHANNEL ) {

          final String[] nicks = binder.getService()
              .getConnection(server.getId())
              .getUsersAsStringArray(conversationForUserList.getName());

          final Context _tContext = (Context)this;

          AlertDialog.Builder userlistBuilder = new AlertDialog.Builder(
              _tContext);

          userlistBuilder.setTitle("Users: " + nicks.length);

          OnClickListener NickSelectorListener = new OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
              final String nick = nicks[which];
              // This is the OnClickListener to actually do something.

              OnClickListener NickActionListener = new OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int which) {

                  /* ********************* */

                  String nicknameWithoutPrefix = removeStatusChar(nick);

                  final IRCConnection connection = binder.getService()
                      .getConnection(server.getId());
                  final String conversation = server.getSelectedConversation();

                  switch ( which ) {
                    case 0:
                      final String replyText = nicknameWithoutPrefix + ": ";
                      /*
                       * handler.post(new Runnable() {
                       *
                       * @Override public void run() {
                       */
                      EditText input = (EditText)findViewById(R.id.input);
                      input.setText(replyText);
                      input.setSelection(replyText.length());
                      openSoftKeyboard(input);
                      input.requestFocus();

                      // }
                      // });
                      break;
                    case 1:
                      Conversation query = server
                          .getConversation(nicknameWithoutPrefix);
                      if( query == null ) {
                        // Open a query if there's none yet
                        query = new Query(nicknameWithoutPrefix);
                        query.setHistorySize(binder.getService().getSettings()
                            .getHistorySize());
                        server.addConversation(query);

                        Intent intent = Broadcast.createConversationIntent(
                            Broadcast.CONVERSATION_NEW, server.getId(),
                            nicknameWithoutPrefix);
                        binder.getService().sendBroadcast(intent);
                      }
                      break;
                    case 2:
                      connection.op(conversation, nicknameWithoutPrefix);
                      break;
                    case 3:
                      connection.deOp(conversation, nicknameWithoutPrefix);
                      break;
                    case 4:
                      connection.halfOp(conversation, nicknameWithoutPrefix);
                      break;
                    case 5:
                      connection.deHalfOp(conversation, nicknameWithoutPrefix);
                      break;
                    case 6:
                      connection.voice(conversation, nicknameWithoutPrefix);
                      break;
                    case 7:
                      connection.deVoice(conversation, nicknameWithoutPrefix);
                      break;
                    case 8:
                      connection.ban(conversation, nicknameWithoutPrefix
                          + "!*@*");
                      break;
                    case 9:
                      connection.kick(conversation, nicknameWithoutPrefix);
                      break;

                  }

                  /* ********************* */

                }
              }; // <-- Thats all for the actions listener.

              AlertDialog.Builder ActionMenuBuilder = new Builder(_tContext);

              ActionMenuBuilder.setItems(R.array.user_actions,
                  NickActionListener);

              ActionMenuBuilder.show();

            }
          };

          ArrayList<CharSequence> coloredNicks = new ArrayList<CharSequence>();
          for( String nick : nicks ) {
            SpannableString ss = new SpannableString(nick);
            if( NickConstants.nickPrefixes.contains(nick.charAt(0)) ) {
              int drawableRes = R.drawable.user;
              switch ( nick.charAt(0) ) {
                case '~':
                case '&':
                case '@':
                  drawableRes = R.drawable.op;
                  break;
                case '+':
                  drawableRes = R.drawable.voice;
                  break;
                default:
                  drawableRes = R.drawable.user;
                  break;

              }
              Drawable d = getResources().getDrawable(drawableRes);
              d.setBounds(0, 0, d.getIntrinsicWidth(), d.getIntrinsicHeight());
              ss.setSpan(new ImageSpan(d), 0, 1,
                  SpannableString.SPAN_INCLUSIVE_EXCLUSIVE);
            }
            ss.setSpan(new ForegroundColorSpan(Message.getSenderColor(nick)),
                0, nick.length(), SpannableString.SPAN_INCLUSIVE_INCLUSIVE);
            coloredNicks.add(ss);
          }

          userlistBuilder.setItems(
              coloredNicks.toArray(new CharSequence[coloredNicks.size()]),
              NickSelectorListener);
          userlistBuilder.show();

        } else {
          Toast.makeText(this,
              getResources().getString(R.string.only_usable_from_channel),
              Toast.LENGTH_SHORT).show();
        }
        break;
      /* Choose a conversation option. */
      case R.id.chooseConversation:
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Choose Conversation");
        CharSequence[] conversationsArr = new CharSequence[pagerAdapter
            .getCount()];
        for( int i = 0; i < pagerAdapter.getCount(); i++ ) {
          Conversation c = pagerAdapter.getItem(i);
          CharSequence title = (c.getName().equals("") ? server.getTitle() : c
              .getName());
          if( c.getNewMentions() > 0 ) {
            SpannableString unread = new SpannableString("("
                + c.getNewMentions() + ")");
            unread.setSpan(
                new ForegroundColorSpan(getResources().getColor(
                    android.R.color.secondary_text_dark_nodisable)), 0,
                unread.length(), SpannableString.SPAN_INCLUSIVE_INCLUSIVE);
            title = TextUtils.concat(title, " ", unread);
          }
          conversationsArr[i] = title;
        }

        OnClickListener listener = new OnClickListener() {

          @Override
          public void onClick(DialogInterface dialog, int which) {
            // Change the page here.
            pager.setCurrentItem(which);
          }
        };

        builder.setItems(conversationsArr, listener);
        builder.show();

        break;

    }

    return true;
  }

  /**
   * Get server object assigned to this activity
   *
   * @return the server object
   */
  public Server getServer() {
    return server;
  }

  /**
   * On conversation message
   */
  @Override
  public void onConversationMessage(String target) {
    Conversation conversation = server.getConversation(target);

    if( conversation == null ) {
      // In an early state it can happen that the conversation object
      // is not created yet.
      return;
    }

    MessageListAdapter adapter = pagerAdapter.getItemAdapter(target);

    while ( conversation.hasBufferedMessages() ) {
      Message message = conversation.pollBufferedMessage();

      if( adapter != null && message != null ) {
        adapter.addMessage(message);
        int status;

        switch ( message.getType() ) {
          case Message.TYPE_MISC:
            status = Conversation.STATUS_MISC;
            break;

          default:
            status = Conversation.STATUS_MESSAGE;
            break;
        }
        conversation.setStatus(status);
      }
    }

    indicator.updateStateColors();
  }

  /**
   * On new conversation
   */
  @Override
  public synchronized void onNewConversation(String target) {
    createNewConversation(target);
    pager.setCurrentItem(pagerAdapter.getPositionByName(target));
  }

  @Override
  public synchronized void onClearConversation(String target) {

    pagerAdapter.getItem(pagerAdapter.getPositionByName(target)).clearHistory();
    pagerAdapter.getItemAdapter(target).clear();
    Log.d("ConversationActivity", "Cleared conversation " + target);
  }

  /**
   * Create a new conversation in the pager adapter for the given target
   * conversation.
   *
   * @param target
   */
  public void createNewConversation(String target) {
    Conversation cv = server.getConversation(target);
    if( cv == null )
      return; // Hack!
    // The above stops a bug with ZNC.
    pagerAdapter.addConversation(server.getConversation(target));
  }

  /**
   * On conversation remove
   */
  @Override
  public void onRemoveConversation(String target) {
    int position = pagerAdapter.getPositionByName(target);

    if( position != -1 ) {
      pagerAdapter.removeConversation(position);
      pager.setCurrentItem(position - 1);

    }
  }

  /**
   * On topic change
   */
  @Override
  public void onTopicChanged(String target) {
    // No implementation

  }

  /**
   * On server status update
   */
  @Override
  public void onStatusUpdate() {
    // An issue in the tracker relates to this.
    // It's way too late to figure out which one.
    // EditText input = (EditText) findViewById(R.id.input);

    if( server.isConnected() ) {
      // input.setEnabled(true);
    } else {
      // input.setEnabled(false);

      /*
       *
       * If we are disconnected, we should have three times where we don't care
       * to pop up the dialog:
       *
       * * Total network loss has occurred and we're working on reconnecting a
       * server (it happens!) * The network is transient and we're waiting on
       * the network to become not-transient. * The server is in the
       * preconnecting phases
       */

      if( server.getStatus() == Status.DISCONNECTED
          && ((settings.reconnectLoss() && binder.getService().isReconnecting(
          server.getId())) || (settings.reconnectTransient() && binder
          .getService().isNetworkTransient()))
          || server.getStatus() == Status.CONNECTING ) {
        return;
      }

      // Service is not connected or initialized yet - See #54
      if( binder == null || binder.getService() == null
          || binder.getService().getSettings() == null ) {
        return;
      }

      if( !binder.getService().getSettings().isReconnectEnabled()
          && !reconnectDialogActive
          && !binder.getService().isReconnecting(serverId) ) {

        reconnectDialogActive = true;
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder
            .setMessage(
                getResources().getString(R.string.reconnect_after_disconnect,
                    server.getTitle()))
            .setCancelable(false)
            .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
              @Override
              public void onClick(DialogInterface dialog, int id) {
                if( !server.isDisconnected() ) {
                  reconnectDialogActive = false;
                  return;
                }
                binder.getService().getConnection(server.getId())
                    .setAutojoinChannels(server.getCurrentChannelNames());
                server.setStatus(Status.CONNECTING);
                binder.connect(server);
                reconnectDialogActive = false;
              }
            })
            .setNegativeButton(getString(R.string.negative_button),
                new DialogInterface.OnClickListener() {
                  @Override
                  public void onClick(DialogInterface dialog, int id) {
                    server.setMayReconnect(false);
                    reconnectDialogActive = false;
                    dialog.cancel();
                  }
                });
        AlertDialog alert = builder.create();
        alert.show();
      }
    }
  }

  /**
   * On activity result
   */
  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    if( resultCode != RESULT_OK ) {
      // ignore other result codes
      return;
    }

    switch ( requestCode ) {
      case REQUEST_CODE_SPEECH:
        ArrayList<String> matches = data
            .getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
        if( matches.size() > 0 ) {
          ((EditText)findViewById(R.id.input)).setText(matches.get(0));
        }
        break;
      case REQUEST_CODE_JOIN:
        joinChannelBuffer = data.getExtras().getString("channel");
        break;
      case REQUEST_CODE_NICK_COMPLETION:
        insertNickCompletion((EditText)findViewById(R.id.input), data
            .getExtras().getString(Extra.USER));
        break;
    }
  }

  private static final int MAX_MESSAGE_LENGTH = 200;

  /**
   * Send a message in this conversation
   *
   * @param text The text of the message
   */
  private void sendMessage(String text) {
    if( text.equals("") ) {
      // ignore empty messages
      return;
    }
    // If we've gotten a multiline message,
    else if( text.contains("\n") ) {
      // Split the multiline message into chunks
      String lines[] = text.split("\\r?\\n");
      // And send each line at at time.
      for( String line : lines ) {
        sendMessage(line);
      }
      // Since we don't want to send things twice, return.
      return;
    }
    // The line handed to us is > 200 chars (an arbitrary limit)
    else if( text.length() > MAX_MESSAGE_LENGTH + 2 ) { // arbitrary limit.
      for( int idx = 0; idx < text.length(); idx += MAX_MESSAGE_LENGTH ) {

        String real_line = text.substring(idx,
            Math.min(idx + MAX_MESSAGE_LENGTH, text.length()));
        if( idx == 0 || idx + MAX_MESSAGE_LENGTH < text.length() ) {
          real_line += "\u2026";
        }
        if( idx > 0 ) {
          real_line = "\u2026" + real_line;
        }
        sendMessage(real_line);
      }
      return;
    }

    if( !server.isConnected() ) {
      Message message = new Message(getString(R.string.message_not_connected));
      message.setColor(Message.MessageColor.ERROR);
      message.setIcon(R.drawable.error);
      server.getConversation(server.getSelectedConversation()).addMessage(
          message);
      onConversationMessage(server.getSelectedConversation());
    }

    scrollback.addMessage(text);

    Conversation conversation = pagerAdapter.getItem(pager.getCurrentItem());

    if( conversation != null ) {
      if( !text.startsWith("/") || text.startsWith("//")) {
        if( conversation.getType() != Conversation.TYPE_SERVER ) {
          String nickname = binder.getService().getConnection(serverId)
              .getNick();
          // conversation.addMessage(new Message("<" + nickname + "> "
          // + text));
          conversation.addMessage(new Message(text, nickname));
          binder.getService().getConnection(serverId)
              .sendMessage(conversation.getName(), text);
        } else {
          Message message = new Message(
              getString(R.string.chat_only_form_channel));
          message.setColor(Message.MessageColor.TOPIC);
          message.setIcon(R.drawable.warning);
          conversation.addMessage(message);
        }
        onConversationMessage(conversation.getName());
      } else {
        CommandParser.getInstance().parse(text, server, conversation,
            binder.getService());
      }
    }
  }

  /**
   * Complete a nick in the input line
   */
  private void doNickCompletion(EditText input) {
    String text = input.getText().toString();

    if( text.length() <= 0 ) {
      return;
    }

    String[] tokens = text.split("[\\s,.-]+");

    if( tokens.length <= 0 ) {
      return;
    }

    String word = tokens[tokens.length - 1].toLowerCase(Locale.US);
    tokens[tokens.length - 1] = null;

    int begin = input.getSelectionStart();
    int end = input.getSelectionEnd();
    int cursor = Math.min(begin, end);
    int sel_end = Math.max(begin, end);

    boolean in_selection = (cursor != sel_end);

    if( in_selection ) {
      word = text.substring(cursor, sel_end);
    } else {
      // use the word at the curent cursor position
      while ( true ) {
        cursor -= 1;
        if( cursor <= 0 || text.charAt(cursor) == ' ' ) {
          break;
        }
      }

      if( cursor < 0 ) {
        cursor = 0;
      }

      if( text.charAt(cursor) == ' ' ) {
        cursor += 1;
      }

      sel_end = text.indexOf(' ', cursor);

      if( sel_end == -1 ) {
        sel_end = text.length();
      }

      word = text.substring(cursor, sel_end);
    }
    // Log.d("Yaaic", "Trying to complete nick: " + word);

    Conversation conversationForUserList = pagerAdapter.getItem(pager
        .getCurrentItem());

    String[] users = null;

    if( conversationForUserList.getType() == Conversation.TYPE_CHANNEL ) {
      users = binder.getService().getConnection(server.getId())
          .getUsersAsStringArray(conversationForUserList.getName());
    }

    // go through users and add matches
    if( users != null ) {
      final List<Integer> result = new ArrayList<Integer>();

      for( int i = 0; i < users.length; i++ ) {
        String nick = removeStatusChar(users[i].toLowerCase(Locale.US));
        if( nick.startsWith(word.toLowerCase(Locale.US)) ) {
          result.add(Integer.valueOf(i));
        }
      }

      if( result.size() == 1 ) {
        input.setSelection(cursor, sel_end);
        insertNickCompletion(input, users[result.get(0).intValue()]);
      } else if( result.size() > 0 ) {
        // There was an ambiguity. Choose who wins.
        // in yaaic, this was 80% handled by an external intent.
        // I find that inelegant, since we can handle it here and win on
        // low-resource
        // devices (e.g. the Moto Triumph).
        // This uses more of the android native resources, being a
        // little less break-y.
        final EditText finput = input;
        final int fCursor = cursor;
        final int fSelEnd = sel_end;
        AlertDialog.Builder b = new Builder(this);
        b.setTitle("Disambiguation");

        // Get the possible users.
        final String[] extra = new String[result.size()];
        int i = 0;
        for( Integer n : result ) {
          extra[i++] = users[n.intValue()];
        }
        // Now, take that list of possible user and let someone choose
        // who wins.
        b.setItems(extra, new OnClickListener() {

          @Override
          public void onClick(DialogInterface dialog, int which) {
            finput.setSelection(fCursor, fSelEnd);
            insertNickCompletion((EditText)findViewById(R.id.input),
                extra[which]);

          }
        });
        // And show that selection.
        b.show();
      }
    }
  }

  /**
   * Insert a given nick completion into the input line
   *
   * @param input The input line widget, with the incomplete nick selected
   * @param nick  The completed nick
   */
  private void insertNickCompletion(EditText input, String nick) {
    int start = input.getSelectionStart();
    int end = input.getSelectionEnd();
    nick = removeStatusChar(nick);

    if( start == 0 ) {
      nick += ":";
    }

    nick += " ";
    input.getText().replace(start, end, nick, 0, nick.length());
    // put cursor after inserted text
    input.setSelection(start + nick.length());
    input.clearComposingText();
    input.post(new Runnable() {
      @Override
      public void run() {
        // make the softkeyboard come up again (only if no hw keyboard
        // is attached)
        EditText input = (EditText)findViewById(R.id.input);
        openSoftKeyboard(input);
      }
    });

    input.requestFocus();
  }

  /**
   * Open the soft keyboard (helper function)
   */
  private void openSoftKeyboard(View view) {
    ((InputMethodManager)getSystemService(INPUT_METHOD_SERVICE))
        .showSoftInput(view, InputMethodManager.SHOW_FORCED);
  }

  /**
   * Remove the status char off the front of a nick if one is present
   *
   * @param nick
   * @return nick without statuschar
   */
  private static String removeStatusChar(String nick) {
    int idx = 0;
    while ( NickConstants.nickPrefixes.contains(nick.charAt(idx)) ) {
      idx++;
    }
    return nick.substring(idx);
  }

  @Override
  public void onPageScrollStateChanged(int arg0) {

  }

  private void hideSubtitle() {
    ActionBar ab = getActionBar();
    CharSequence t = ab.getTitle();
    ab.setDisplayShowTitleEnabled(false);
    ab.setSubtitle(null);
    ab.setDisplayShowTitleEnabled(true);
    ab.setTitle(t);
  }

  private void showSubtitle() {
    ActionBar ab = getActionBar();
    ab.setTitle(server.getTitle());

    Conversation c = pagerAdapter.getItem(pager.getCurrentItem());
    String newName = c.getName();
    switch ( c.getType() ) {
      case Conversation.TYPE_SERVER:
        newName = getString(R.string.subtitle_server);
        break;
      case Conversation.TYPE_CHANNEL:
        newName = c.getName();
        break;
      case Conversation.TYPE_QUERY:
        newName = String
            .format(getString(R.string.subtitle_query), c.getName());
        break;
      default:
        newName = c.getName();
    }

    ab.setSubtitle(newName);

  }

  private void setupIndicator() {
    // This either:
    // * Hides the pager indicator (by setting its visibility to GONE)
    // * Hides the subtitle (by calling hideSubtitle() )

    if( settings.showChannelBar() ) {
      indicator.setVisibility(View.VISIBLE);
      hideSubtitle();
    } else {
      indicator.setVisibility(View.GONE);
      showSubtitle();
    }

  }

  @Override
  public void onPageScrolled(int arg0, float arg1, int arg2) {

  }

  @Override
  public void onPageSelected(int arg0) {
    if( settings.showChannelBar() == false ) {
      showSubtitle();
    }
  }
}
