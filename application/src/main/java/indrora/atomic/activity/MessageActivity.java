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
import indrora.atomic.model.Extra;

import indrora.atomic.R;
import indrora.atomic.model.Message;
import indrora.atomic.model.MessageRenderParams;

import android.app.Activity;
import android.content.ClipData;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.Window;
import android.widget.TextView;

/**
 * Activity for single message view
 *
 * @author Sebastian Kaspari <sebastian@yaaic.org>
 */
public class MessageActivity extends Activity implements Toolbar.OnMenuItemClickListener{

  Message viewedMessage = null;
  TextView messageView = null;

  /**
   * On create
   */
  @Override
  public void onCreate(Bundle savedInstanceState) {
    setTheme(R.style.AppDialogTheme);
    super.onCreate(savedInstanceState);
    setContentView(R.layout.message);

    MessageRenderParams tmpParams = App.getSettings().getRenderParams();
    tmpParams.messageColors = false;
    tmpParams.colorScheme = "default";
    tmpParams.smileys = false;
    tmpParams.icons = false;
    tmpParams.messageColors = false;
    tmpParams.useDarkScheme = true;


    viewedMessage = getIntent().getExtras().getParcelable(Extra.MESSAGE);

    Toolbar tb = (Toolbar)findViewById(R.id.toolbar);

    tb.inflateMenu(R.menu.messageops);

    tb.setOnMenuItemClickListener(this);

    messageView = (TextView)findViewById(R.id.message);
    messageView.setBackgroundColor(Color.BLACK);

    CharSequence msgSequence = Message.render(viewedMessage, tmpParams);

    messageView.setText(msgSequence);

  }

  /**
   * Handle toolbar menu clicks.
   *
   * @param item
   * @return
   */
  @Override
  public boolean onMenuItemClick(MenuItem item) {
    switch(item.getItemId()) {
      case R.id.message_copy:
        // copy the thing
        ClipData.newPlainText("IRC Message", messageView.getText().toString());
        break;
      case R.id.close:
        break;
      default:
        return false;
    }
    this.finish();
    return true;
  }
}
