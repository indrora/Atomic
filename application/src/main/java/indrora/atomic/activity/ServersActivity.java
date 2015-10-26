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
import indrora.atomic.FirstRunActivity;
import indrora.atomic.adapter.ServerListAdapter;
import indrora.atomic.db.Database;
import indrora.atomic.irc.IRCBinder;
import indrora.atomic.irc.IRCService;
import indrora.atomic.listener.ServerListener;
import indrora.atomic.model.Broadcast;
import indrora.atomic.model.Extra;
import indrora.atomic.model.Server;
import indrora.atomic.model.Status;
import indrora.atomic.receiver.ServerReceiver;
import indrora.atomic.utils.LatchingValue;

import java.util.ArrayList;

import indrora.atomic.R;

import android.support.v7.app.AppCompatActivity;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.text.format.Time;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ListView;
import android.widget.Toast;


/**
 * List of servers
 *
 * @author Sebastian Kaspari <sebastian@yaaic.org>
 */
public class ServersActivity extends AppCompatActivity implements ServiceConnection, ServerListener, OnItemClickListener, OnItemLongClickListener {
  private IRCBinder binder;
  private ServerReceiver receiver;
  private ServerListAdapter adapter;
  private ListView list;
  private static int instanceCount = 0;

  static LatchingValue<Boolean> doAutoconnect = new LatchingValue<Boolean>(true, false);

  /**
   * On create
   */
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    /*
     * With activity:launchMode = standard, we get duplicated activities
     * depending on the task the app was started in. In order to avoid
     * stacking up of this duplicated activities we keep a count of this
     * root activity and let it finish if it already exists
     *
     * Launching the app via the notification icon creates a new task,
     * and there doesn't seem to be a way around this so this is needed
     */
    if( instanceCount > 0 ) {
      finish();
    }

    instanceCount++;
    setContentView(R.layout.servers);

    setSupportActionBar((android.support.v7.widget.Toolbar)findViewById(R.id.toolbar));

    adapter = new ServerListAdapter();

    list = (ListView)findViewById(android.R.id.list);
    list.setAdapter(adapter);
    list.setOnItemClickListener(this);
    list.setOnItemLongClickListener(this);
  }

  /**
   * On Destroy
   */
  @Override
  public void onDestroy() {
    super.onDestroy();
    instanceCount--;
  }

  /**
   * On resume
   */
  @Override
  public void onResume() {
    super.onResume();

    // Start and connect to service
    Intent intent = new Intent(this, IRCService.class);
    intent.setAction(IRCService.ACTION_BACKGROUND);
    startService(intent);
    int flags = 0;
    if( android.os.Build.VERSION.SDK_INT >= 14 ) {
      flags |= Context.BIND_ABOVE_CLIENT;
    }
    bindService(intent, this, flags);

    receiver = new ServerReceiver(this);
    registerReceiver(receiver, new IntentFilter(Broadcast.SERVER_UPDATE));

    adapter.loadServers();
  }

  /**
   * On pause
   */
  @Override
  public void onPause() {
    super.onPause();

    if( binder != null && binder.getService() != null ) {
      binder.getService().checkServiceStatus();
    }

    unbindService(this);
    unregisterReceiver(receiver);
  }

  /**
   * Service connected to Activity
   */
  @Override
  public void onServiceConnected(ComponentName name, IBinder service) {
    binder = (IRCBinder)service;

    // This is kinda cute
    // We'll always keep the service in the foreground, just to remind people that it's there.

    Intent intent = new Intent(this, IRCService.class);
    intent.setAction(IRCService.ACTION_FOREGROUND);
    startService(intent);
    // Autoconnect is done via a Latching Value. There's no real reason to have it
    // a latchingValue but it lets us later on reset the Autoconnect fields.
    autoconnect();

  }

  /**
   * Do the autoconnect stuff
   */
  private void autoconnect() {
    // If we don't have any servers to go with.
    if( Atomic.getInstance().getServersAsArrayList().size() < 1 )
      return;
    // Or we've done this already
    if( !doAutoconnect.getValue() )
      return;
    // We don't need to get this far.

    // Are we connected to the greater wide not-us?
    NetworkInfo ninf = ((ConnectivityManager)(this.getSystemService(Service.CONNECTIVITY_SERVICE))).getActiveNetworkInfo();
    // If there's no way out, or we aren't actually connected,
    if( ninf == null || ninf.getState() != NetworkInfo.State.CONNECTED ) {
      // We don't need to bother, but we should say something.
      Toast.makeText(this, "Autoconnect skipped due to network outage", Toast.LENGTH_LONG).show();
      return;
    }
    // Some slime...
    Log.d("ServerList", "Doing autoconnect");
    for( int idx = 0; idx < adapter.getCount(); idx++ ) {
      Server s = adapter.getItem(idx);
      if( s.getAutoconnect() && s.getStatus() == Status.DISCONNECTED ) {
        ConnectServer(s);
      }
    }


  }

  /**
   * Service disconnected from Activity
   */
  @Override
  public void onServiceDisconnected(ComponentName name) {
    binder = null;
  }

  /**
   * On server selected
   */
  @Override
  public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
    Server server = adapter.getItem(position);

    if( server == null ) {
      // "Add server" was selected
      startActivityForResult(new Intent(this, AddServerActivity.class), 0);
      return;
    }

    Intent intent = new Intent(this, ConversationActivity.class);

    if( server.getStatus() == Status.DISCONNECTED && !server.mayReconnect() ) {
      server.setStatus(Status.PRE_CONNECTING);
      intent.putExtra("connect", true);
    }

    intent.putExtra("serverId", server.getId());
    startActivity(intent);
  }

  private void ConnectServer(Server s) {
    if( s.getStatus() == Status.DISCONNECTED ) {
      binder.connect(s);
      s.setStatus(Status.CONNECTING);
      adapter.notifyDataSetChanged();
    }

  }

  private void DisconnectServer(Server server) {
    if( server.getStatus() == Status.DISCONNECTED ) {
      return;
    }

    server.clearConversations();
    server.setStatus(Status.DISCONNECTED);
    server.setMayReconnect(false);

    binder.getService().removeReconnection(server.getId());
    binder.getService().getConnection(server.getId()).disconnect();

  }

  /**
   * On long click
   */
  @Override
  public boolean onItemLongClick(AdapterView<?> l, View v, int position, long id) {
    final Server server = adapter.getItem(position);

    if( server == null ) {
      // "Add server" view selected
      return true;
    }

    // This lets us change if we're going to CONNECT or DISCONNECT from a server from the long-press menu.
    int mangleString = R.string.connect;

    if( server.getStatus() != Status.DISCONNECTED ) {
      mangleString = R.string.disconnect;
    }

    final int fMangleString = mangleString;

    final CharSequence[] items = {
        getString(fMangleString),
        getString(R.string.edit),
        getString(R.string.duplicate_server),
        getString(R.string.delete)
    };


    AlertDialog.Builder builder = new AlertDialog.Builder(this);
    builder.setTitle(server.getTitle());
    builder.setItems(items, new DialogInterface.OnClickListener() {
      @Override
      public void onClick(DialogInterface dialog, int item) {
        switch ( item ) {
          case 0: // Connect/Disconnect
            if( fMangleString == R.string.connect ) {
              ConnectServer(server);
            } else if( fMangleString == R.string.disconnect ) {
              DisconnectServer(server);
            }
            break;
          case 1: // Edit
            editServer(server.getId());
            break;
          case 2:
            duplicateServer(server.getId());
            break;
          case 3: // Delete
            binder.getService().getConnection(server.getId()).quitServer();
            deleteServer(server.getId());
            break;
        }
      }
    });
    AlertDialog alert = builder.create();
    alert.show();
    return true;
  }

  /**
   * Start activity to edit server with given id
   *
   * @param serverId The id of the server
   */
  private void editServer(int serverId) {
    Server server = Atomic.getInstance().getServerById(serverId);

    if( server.getStatus() != Status.DISCONNECTED ) {
      Toast.makeText(this, getResources().getString(R.string.disconnect_before_editing), Toast.LENGTH_SHORT).show();
    } else {
      Intent intent = new Intent(this, AddServerActivity.class);
      intent.setAction(AddServerActivity.ACTION_EDIT_SERVER);
      intent.putExtra(Extra.SERVER, serverId);
      startActivityForResult(intent, 0);
    }
  }

  private void duplicateServer(int serverId) {
    Intent intent = new Intent(this, AddServerActivity.class);
    intent.setAction(AddServerActivity.ACTION_DUPE_SERVER);
    intent.putExtra(Extra.SERVER, serverId);
    startActivityForResult(intent, 0);
  }

  /**
   * Options Menu (Menu Button pressed)
   */
  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    super.onCreateOptionsMenu(menu);

    // inflate from xml
    MenuInflater inflater = new MenuInflater(this);
    inflater.inflate(R.menu.servers, menu);

    return true;
  }

  /**
   * On menu item selected
   */
  @Override
  public boolean onOptionsItemSelected( MenuItem item) {
    switch ( item.getItemId() ) {
      case R.id.add:
        startActivityForResult(new Intent(this, AddServerActivity.class), 0);
        break;
      case R.id.about:
        startActivity(new Intent(this, AboutActivity.class));
        break;
      case R.id.settings:
        startActivity(new Intent(this, SettingsActivity.class));
        break;
      case R.id.disconnect_all:
        ArrayList<Server> mServers = Atomic.getInstance().getServersAsArrayList();
        binder.getService().clearReconnectList();
        for( Server server : mServers ) {
          DisconnectServer(server);
        }
        // ugly
        // binder.getService().stopForegroundCompat(R.string.app_name);
    }

    return true;
  }

  /**
   * On activity result
   */
  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    if( resultCode == RESULT_OK ) {
      // Refresh list from database
      adapter.loadServers();
    }
  }

  /**
   * Delete server
   *
   * @param serverId
   */
  public void deleteServer(int serverId) {
    Database db = new Database(this);
    db.removeServerById(serverId);
    db.close();
    // make sure we don't accidentally reconnect it
    binder.getService().removeReconnection(serverId);
    Atomic.getInstance().removeServerById(serverId);
    adapter.loadServers();
  }

  /**
   * On server status update
   */
  @Override
  public void onStatusUpdate() {
    adapter.loadServers();
  }

  long lastBackPress = 0;

  @Override
  public void onBackPressed() {
    if( lastBackPress + 2000 > System.currentTimeMillis() ) {
      ArrayList<Server> mServers = Atomic.getInstance().getServersAsArrayList();
      for( Server server : mServers ) {
        if( binder.getService().hasConnection(server.getId()) ) {
          DisconnectServer(server);
        }
      }
      binder.getService().stopForegroundCompat(R.string.app_name);
      System.exit(0);
    } else {
      Toast.makeText(this, R.string.back_twice_exit, Toast.LENGTH_LONG).show();
      lastBackPress = System.currentTimeMillis();
    }
  }

}
