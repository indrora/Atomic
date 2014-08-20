package indrora.atomic;

import indrora.atomic.model.ColorScheme;
import indrora.atomic.model.Settings;
import indrora.atomic.utils.LatchingValue;
import android.app.Application;
import android.content.Intent;

public class App extends Application {

  Atomic atomic;
  public App() {
    super();

    autoconnectComplete = new LatchingValue<Boolean>(true, false);
  }

  private static LatchingValue<Boolean> autoconnectComplete;

  private static ColorScheme _c;
  private static Settings _s;

  public static ColorScheme getColorScheme() {
    return _c;
  }

  public static Settings getSettings() {
    return _s;
  }
  
  public static Boolean doAutoconnect() {
    return autoconnectComplete.getValue();
  }
  @Override
  public void onCreate() {
    // Context exists here.
    Atomic.getInstance().loadServers(getApplicationContext());

    indrora.atomic.model.Settings _settings = new Settings(this);
    _s = _settings;
    // Release 16 changes things for colors.
    // This is a much more elegant solution than I had here. Be glad.
    if( _s.getLastRunVersion() < 16 ) {
      _settings.setColorScheme("default");
    }

    _c = new ColorScheme(getApplicationContext());

    if(_settings.getCurrentVersion() > _settings.getLastRunVersion()) {
      Intent runIntent = new Intent(this,FirstRunActivity.class);
      runIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
      this.startActivity(runIntent);
    }
    
    String ll = _settings.getDefaultNick();
    ll = ll.trim();
    
    super.onCreate();
  }
}
