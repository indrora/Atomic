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

import indrora.atomic.model.Authentication;
import indrora.atomic.model.Extra;

import indrora.atomic.R;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.view.LayoutInflaterCompat;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.StackView;
import android.widget.TextView;
import android.widget.CompoundButton.OnCheckedChangeListener;

/**
 * Authentication activity for entering nickserv / sasl usernames and password
 * for a given server.
 *
 * @author Sebastian Kaspari <sebastian@yaaic.org>
 */
public class AuthenticationView extends LinearLayout implements OnCheckedChangeListener {
  private CheckBox nickservCheckbox;
  private TextView nickservPasswordLabel;
  private EditText nickservPasswordEditText;

  private CheckBox saslCheckbox;
  private TextView saslUsernameLabel;
  private EditText saslUsernameEditText;
  private TextView saslPasswordLabel;
  private EditText saslPasswordEditText;

  String _saslUsername;
  String _saslPassword;
  String _nickservPassword;

  public String getSaslUsername() { return saslUsernameEditText.getText().toString(); }
  public String getSaslPassword() { return saslPasswordEditText.getText().toString(); }
  public String getNickservPassword() { return nickservPasswordEditText.getText().toString(); }

  public AuthenticationView(Context context, Authentication auth) {
    super(context);

    LayoutInflater.from(context).inflate(R.layout.authentication, this);

    nickservCheckbox = (CheckBox)(this.findViewById(R.id.nickserv_checkbox));
    nickservPasswordLabel = (TextView)(this.findViewById(R.id.nickserv_label_password));
    nickservPasswordEditText = (EditText)(this.findViewById(R.id.nickserv_password));

    saslCheckbox = (CheckBox)(this.findViewById(R.id.sasl_checkbox));
    saslUsernameLabel = (TextView)(this.findViewById(R.id.sasl_label_username));
    saslUsernameEditText = (EditText)(this.findViewById(R.id.sasl_username));
    saslPasswordLabel = (TextView)(this.findViewById(R.id.sasl_label_password));
    saslPasswordEditText = (EditText)(this.findViewById(R.id.sasl_password));

    nickservCheckbox.setOnCheckedChangeListener(this);
    saslCheckbox.setOnCheckedChangeListener(this);

    _nickservPassword = auth.getNickservPassword();
    _saslUsername = auth.getSaslUsername();
    _saslPassword = auth.getSaslPassword();


    if( _nickservPassword != null && _nickservPassword.length() > 0 ) {
      nickservCheckbox.setChecked(true);
      nickservPasswordEditText.setText(_nickservPassword);
    }


    if( _saslUsername != null && _saslUsername.length() > 0 ) {
      saslCheckbox.setChecked(true);
      saslUsernameEditText.setText(_saslUsername);
      saslPasswordEditText.setText(_saslPassword);
    }
    // Now, collapse ourselves if needed.
    findViewById(R.id.nickservHolder).setVisibility(nickservCheckbox.isChecked()? View.VISIBLE:View.GONE);
    findViewById(R.id.saslHolder).setVisibility(saslCheckbox.isChecked()? View.VISIBLE:View.GONE);

    // And layout ourselves.
    requestLayout();

  }

  /**
   * On checkbox changed
   */
  @Override
  public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
    switch ( buttonView.getId() ) {
      case R.id.nickserv_checkbox:
        findViewById(R.id.nickservHolder).setVisibility(isChecked? View.VISIBLE:View.GONE);
        nickservPasswordLabel.setEnabled(isChecked);
        nickservPasswordEditText.setEnabled(isChecked);

        if( !isChecked ) {
          nickservPasswordEditText.setText("");
        }

        break;

      case R.id.sasl_checkbox:
        findViewById(R.id.saslHolder).setVisibility(isChecked? View.VISIBLE:View.GONE);
        saslUsernameLabel.setEnabled(isChecked);
        saslUsernameEditText.setEnabled(isChecked);
        saslPasswordLabel.setEnabled(isChecked);
        saslPasswordEditText.setEnabled(isChecked);

        if( !isChecked ) {
          saslUsernameEditText.setText("");
          saslPasswordEditText.setText("");
        }

        break;
    }
  }
}
