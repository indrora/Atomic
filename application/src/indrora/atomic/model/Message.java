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
package indrora.atomic.model;

import indrora.atomic.App;
import indrora.atomic.utils.MircColors;
import indrora.atomic.utils.Smilies;

import java.util.Date;
import java.util.Locale;







import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.method.DateTimeKeyListener;
import android.text.style.ForegroundColorSpan;
import android.text.style.ImageSpan;
import android.text.util.Linkify;
import android.util.Log;
import android.widget.TextView;

/**
 * A channel or server message
 *
 * @author Sebastian Kaspari <sebastian@yaaic.org>
 */
public class Message {
  public enum MessageColor {
    USER_EVENT,
    CHANNEL_EVENT,
    SERVER_EVENT,
    TOPIC,
    HIGHLIGHT,
    ERROR,
    DEFAULT,
    NO_COLOR
  }

  private static Settings settings;
  
  
  ColorScheme _scheme;

  /* normal message, this is the default */
  public static final int TYPE_MESSAGE = 0;
  public static final int TYPE_ACTION = 2;
  public static final int TYPE_SERVER = 3;
  
  /* join, part or quit */
  public static final int TYPE_MISC    = 1;

  public static final int NO_ICON  = -1;
  public static final int NO_TYPE  = -1;
  public static final int NO_COLOR = -1;

  private final String text;
  private final String sender;
  private long timestamp;

  private MessageColor color = MessageColor.DEFAULT;
  private int type  = NO_TYPE;
  private int icon  = NO_ICON;

  /**
   * Create a new message without an icon defaulting to TYPE_MESSAGE
   *
   * @param text
   */
  public Message(String text) {
    this(text, null, TYPE_MESSAGE);
  }

  /**
   * Create a new message without an icon with a specific type
   *
   * @param text
   * @param type Message type
   */
  public Message(String text, int type) {
    this(text, null, type);
  }

  /**
   * Create a new message sent by a user, without an icon,
   * defaulting to TYPE_MESSAGE
   *
   * @param text
   * @param sender
   */
  public Message(String text, String sender) {
    this(text, sender, TYPE_MESSAGE);
  }

  /**
   * Create a new message sent by a user without an icon
   *
   * @param text
   * @param sender
   * @param type Message type
   */
  public Message(String text, String sender, int type) {
    this(text,sender,type,new Date().getTime());

  }
  public Message(String text, String sender, int type, long time) {
    this.text = text;
    this.sender = sender;
    this.timestamp = time;
    this.type = type;
    if(settings == null) {
      settings = App.getSettings();
    }
    _scheme = App.getColorScheme();

  }

  
  
  /**
   * Set the message's icon
   */
  public void setIcon(int icon) {
    this.icon = icon;
  }

  /**
   * Get the message's icon
   *
   * @return
   */
  public int getIcon() {
    return icon;
  }

  /**
   * Get the text of this message
   *
   * @return
   */
  public String getText() {
    return text;
  }

  /**
   * Get the type of this message
   *
   * @return One of Message.TYPE_*
   */
  public int getType() {
    return type;
  }

  public void setType(int t) {
    this.type = t;
  }
  
  /**
   * Set the color of this message
   */
  public void setColor(MessageColor color) {
    this.color = color;
  }

  private int translateColor(MessageColor c) {
    switch(c) {
    case CHANNEL_EVENT:
      return _scheme.getChannelEvent();
    case DEFAULT:
      return _scheme.getForeground();
    case ERROR:
      return _scheme.getError();
    case HIGHLIGHT:
      return _scheme.getHighlight();
    case SERVER_EVENT:
      return _scheme.getServerEvent();
    case TOPIC:
      return _scheme.getTopic();
    case USER_EVENT:
      return _scheme.getUserEvent();
    default:
      return _scheme.getForeground();
    }

  }

  /**
   * Set the timestamp of the message
   *
   * @param timestamp
   */
  public void setTimestamp(long timestamp) {
    this.timestamp = timestamp;
  }


  private int getSenderColor() { 
    return getSenderColor(this.sender);
  }
  
  /**
   * Associate a color with a sender name
   *
   * @return a color hexa
   */
  public static int getSenderColor(String sender) {
    /* It might be worth to use some hash table here */
    if (sender == null) {
      return App.getColorScheme().getForeground();
    }

    if(!App.getSettings().showColorsNick()) {
      return App.getColorScheme().getForeground();
    }
    
    int color = 0;
    int variant = sender.charAt(0);

    for(int i = 0; i < sender.length(); i++) {
      char c = sender.charAt(i);
      if(c-33 >'Z')  variant +=(c-33)%32;
      else variant -=(c-33)%32;
      color += c;
    }

    variant %= 20;
    // We don't want the color to be the background color.

    final int bg = App.getColorScheme().getBackground();
    int tmpColor;// = _scheme.getMircColor(color);
    do {

      float[] hsv = new float[3];
      Color.colorToHSV(App.getColorScheme().getMircColor(color++), hsv);
      hsv[0] += variant;

      tmpColor = Color.HSVToColor(hsv);
    } while(likeness(bg, tmpColor) < 30);

    return tmpColor; //colors[color];
  }

  /**
   * Calculates a likeness. This will return between 0-255
   * on the likeness of the color.
   * @param back
   * @param fore
   * @return
   */
  private static int likeness(int back, int fore) {


    double gamma = 2.2; // Woo constants.
    double backL =
      0.2126 * Math.pow( (float)Color.red(back)/255.0,    gamma )
      + 0.7152 * Math.pow( (float)Color.green(back)/255.0,  gamma )
      + 0.0722 * Math.pow( (float)Color.blue(back)/255.0,   gamma );
    double foreL =
      0.2126 * Math.pow( (float)Color.red(fore)/255.0,    gamma )
      + 0.7152 * Math.pow( (float)Color.green(fore)/255.0,  gamma )
      + 0.0722 * Math.pow( (float)Color.blue(fore)/255.0,   gamma );
    int distance = (int) (255 * Math.abs(backL-foreL));
    return distance;
  }

  private SpannableString _cache = null;

  MessageRenderParams currentParams = new MessageRenderParams();

  Conversation _parent;
  protected void setConversation(Conversation p)
  {
    _parent = p;
  }
  
  /**
   * Render message as spannable string
   *
   * @return
   */
  public SpannableString render() {

    
    // An optimization starts here:
    // RenderParams defines a "render snapshot".
    // If RenderParams changes, we should invalidate the cache and set our new rendering parameters.
    
    if(_cache != null ){
      if(settings.getRenderParams().equals(currentParams)) {
        return _cache;
      }
      
    }
    
    _scheme = App.getColorScheme();

    SpannableString nickSS;
    SpannableString timeSS;
    SpannableString messageSS;
    SpannableString prefixSS;
    
    // format the sender name
    if(hasSender()) {
      // The sender name spannable is just the name of the sender
      nickSS = new SpannableString(sender);
      // Defaultly, the foreground color is used.
      int senderColor = _scheme.getForeground();
      
      
      if(settings.showColorsNick()) {
          // getSenderColor does a variant color from the color scheme options.
          senderColor = getSenderColor();
        }
      

      // We should now set the spannable's color.
      nickSS.setSpan(new ForegroundColorSpan(senderColor), 0, nickSS.length(), SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE);
      // and wrap it in our preferred <'s
      if(type == TYPE_MESSAGE) {
    	nickSS = new SpannableString(TextUtils.concat("<", nickSS, ">"));
      }
    }
    else
    {
      // There's no sender, so we give it no sender.
      nickSS = new SpannableString("");
    }
    
    // Timestamps are handled in much the same way as the Sender, however they're much simpler.
    if(settings.showTimestamp()) {
      timeSS = new SpannableString(renderTimeStamp(settings.use24hFormat(), settings.includeSeconds()));
    }
    else {
      timeSS = new SpannableString("");
    }
    
    // Prefix.
    // this is a space normally, however it becomes a *, then is filled with the appropriate
    // drawables.
    prefixSS = new SpannableString(" ");
    // If there is an icon,
    if(hasIcon()) {
      // We want to handle having an icon and not wanting to show icons.
      // this makes things a little nicer.
      prefixSS = new SpannableString("*");
      // If we really want to show icons...
      if(settings.showIcons()) {
        // the Paint object here lets us get the width of a monospaced space.
        Paint p = new Paint();
        p.setTypeface(Typeface.MONOSPACE);
        float spaceWidth = p.measureText("  ");
        // The drawable here is our icon. Internally, the icon is seriously just a reference into the
        // resources block
        
        Drawable drawable = App.getSResources().getDrawable(icon);
        
        float density = App.getSResources().getDisplayMetrics().density;
        // scale = wanted / actual
        float scale = spaceWidth / (float)(drawable.getMinimumWidth());
        // This call is < x,y, width,height>
        // SpaceWidth is going to be in raw pixels, so we need to multiply it by density.
        // Height is going to be the drawable intrinsic height * scale * density
        drawable.setBounds(0, 0, (int)(spaceWidth * density), (int)(drawable.getIntrinsicHeight() * scale * density));
        // And now we do the magic.
        prefixSS.setSpan(new ImageSpan(drawable, ImageSpan.ALIGN_BASELINE), 0, 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        
      }
    }

    // Now to the actual message.
    // How this works is that we're going to render it to a string,
    // if mIRC colors
    //   => highlight color?
    //      => blarg the spannable.
    //   => blarg mIRC colors into the spannable.
    // else
    //   => strip mIRC color tags from the text block
    //   => highlight color?
    //      => blarg the spannable.
    messageSS = new SpannableString(text);

    if (settings.showMircColors()) {
      if (hasColor() && settings.showColors()) {
        messageSS.setSpan(new ForegroundColorSpan(translateColor(color)), 0,
            messageSS.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
      }
      messageSS = MircColors.toSpannable(messageSS);
    } else {
      messageSS = new SpannableString(MircColors.removeStyleAndColors(text));
      if (hasColor() && settings.showColors()) {
        messageSS.setSpan(new ForegroundColorSpan(translateColor(color)), 0,
            messageSS.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
      }
    }
    // Smash smileys into this, but only if we're not a server message..
    if (settings.showGraphicalSmilies() && this.type != TYPE_SERVER) {
      messageSS = Smilies.toSpannable(messageSS, App.getAppContext());
    }

    // An optimization is finished here:
    // If we don't cache what we print, we begin to have an O(2N) problem at best,
    // but if we set _cache (which is owned by us) to what we will now return,
    // when we come back, we can return our cached version
    
    _cache =  new SpannableString(TextUtils.concat( timeSS, prefixSS, nickSS, " ", messageSS ));
    currentParams = settings.getRenderParams();
    return _cache;
    
  }

  /**
   * Does this message have a sender?
   *
   * @return
   */
  private boolean hasSender() {
    return sender != null;
  }

  /**
   * Does this message have a color assigned?
   *
   * @return
   */
  private boolean hasColor() {
    return color != MessageColor.NO_COLOR;
  }

  /**
   * Does this message have an icon assigned?
   *
   * @return
   */
  private boolean hasIcon() {
    return icon != NO_ICON;
  }

  /**
   * Generate a timestamp
   *
   * @param use24hFormat
   * @return
   */
  public String renderTimeStamp(boolean use24hFormat, boolean includeSeconds) {

    Date date = new Date(timestamp);
    String format = "[";

    format += (use24hFormat?"HH":"hh");
    format += ":mm";

    if (includeSeconds) {
      format += ":ss";
    }
    format += "]";

    return (String) ( new java.text.SimpleDateFormat(format, Locale.US)).format(date);
  }
}
