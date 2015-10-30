package indrora.atomic.dialog;

import indrora.atomic.model.Extra;

import java.util.ArrayList;

import indrora.atomic.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;

public class AddAliasView extends LinearLayout implements OnClickListener, OnItemClickListener, TextWatcher {
  private EditText aliasInput;
  private ArrayAdapter<String> adapter;
  private ArrayList<String> aliases;
  private Button addButton;

  public AddAliasView(Context context, ArrayList<String> aliases) {
    super(context);
    this.aliases = (ArrayList<String>)(aliases.clone());

    LayoutInflater.from(context).inflate(R.layout.aliasadd, this, true);

    aliasInput = (EditText)findViewById(R.id.alias);
    aliasInput.addTextChangedListener(this);

    adapter = new ArrayAdapter<String>(this.getContext(), R.layout.aliasitem, this.aliases);

    ListView list = (ListView)findViewById(R.id.aliases);
    list.setAdapter(adapter);
    list.setOnItemClickListener(this);

    addButton = (Button)findViewById(R.id.add);
    addButton.setOnClickListener(this);
  }

  /**
   * On Click
   */
  @Override
  public void onClick(View v) {
    switch ( v.getId() ) {
      case R.id.add:
        String alias = aliasInput.getText().toString().trim();
        if(alias.length() == 0 ) {
          return;
        }
        aliases.add(alias);
        adapter.notifyDataSetChanged();
        aliasInput.setText("");
        break;
    }
  }

  public ArrayList<String> getAliases() {
    return aliases;
  }

  /**
   * On item clicked
   */
  @Override
  public void onItemClick(AdapterView<?> list, View item, int position, long id) {
    final String alias = adapter.getItem(position);

    AlertDialog.Builder builder = new AlertDialog.Builder(this.getContext());
    builder.setTitle(alias);

    builder.setPositiveButton(R.string.action_remove, new DialogInterface.OnClickListener() {
      @Override
      public void onClick(DialogInterface dialogInterface, int i) {
        aliases.remove(alias);
        adapter.notifyDataSetChanged();
      }
    });
    AlertDialog alert = builder.create();
    alert.show();
  }

  /**
   * On text changed
   */
  @Override
  public void onTextChanged(CharSequence s, int start, int before, int count) {
    addButton.setEnabled(aliasInput.getText().length() > 0);
  }

  /**
   * After text changed
   */
  @Override
  public void afterTextChanged(Editable s) {
    // Do nothing.
  }

  /**
   * Before text changed
   */
  @Override
  public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    // Do nothing.
  }
}
