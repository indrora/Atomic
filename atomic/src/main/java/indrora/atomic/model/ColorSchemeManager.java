package indrora.atomic.model;

import indrora.atomic.App;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;

public class ColorSchemeManager implements OnSharedPreferenceChangeListener {

  Context _ctx;
  Settings _settings;
  ColorScheme _currentColorScheme;
  
  public ColorSchemeManager()  {
    _settings = App.getSettings();
    _currentColorScheme = new ColorScheme(_settings.getColorScheme(), _settings.getUseDarkColors());
  }
  
  public ColorScheme getCurrentScheme() {
    return _currentColorScheme;
  }

  @Override
  public void onSharedPreferenceChanged(SharedPreferences arg0, String arg1) {
    if(_settings.getColorScheme() != _currentColorScheme.getName()) {
      _currentColorScheme.loadScheme(_settings.getColorScheme(), _settings.getUseDarkColors());
    }
    
  }
  
}
