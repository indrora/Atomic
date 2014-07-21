package indrora.atomic.model;

import indrora.atomic.R;
import indrora.atomic.R.raw;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Properties;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.graphics.Color;
import android.preference.PreferenceManager;
import android.util.Log;

public class ColorScheme implements OnSharedPreferenceChangeListener {

  static Context _context;
  static Settings _settings;

  private static HashMap<String, Integer> scheme_colors;

  private static Properties themeProps;
  private static int[] colors = new int[16];
  private static String lastScheme = "";
  private static boolean isDarkTheme = true;

  public ColorScheme(Context ctx) {
    // initialize ourselves.
    if (ctx != null) {
      _context = ctx;
    }

    if (_settings == null) {
      _settings = new Settings(_context);
      refreshColorScheme();

    }
    PreferenceManager.getDefaultSharedPreferences(_context)
    .registerOnSharedPreferenceChangeListener(this);
  }

  private synchronized void loadScheme(String scheme, boolean useDarkTheme) {
    // Load the given scheme.
    boolean needsReload = false;
    if( !scheme.equals(lastScheme) ) {
      needsReload = true;
    }
    if( useDarkTheme != isDarkTheme ) {
      needsReload = true;
    }

    if(!needsReload) {
      return;
    }

    lastScheme = scheme;
    isDarkTheme = useDarkTheme;

    Log.d("ColorScheme", "Reloading ColorScheme");

    synchronized (colors) {
      // Clean up the properties file
      themeProps = new Properties();
      // Load the default theme; This will later be set to whatever theme
      // we
      // really want to load later.
      int id = R.raw.theme_default;
      // Attempt to load the default...
      try {
        themeProps.load(_context.getResources().openRawResource(id));
      } catch (Throwable e) {
        Log.e("ColorScheme",
              "Failure loading default theme: " + e.toString());
      }
      // Now, find the real scheme.
      Class<raw> raw_resources = R.raw.class;
      try {
        // get the id of the given scheme.
        Field scheme_field = raw_resources.getField("theme_" + scheme);
        id = scheme_field.getInt(null);
      } catch (Throwable e) {
        // If the given theme was wrong, use the default theme (which
        // means
        // we'll skip over the next bit)
        Log.e("ColorScheme","Failure loading theme in preferences: "+e.toString());
        id = R.raw.theme_default;
      }

      // If we are really loading a theme that isn't the default,
      if (id != R.raw.theme_default) {
        // Get the real theme.
        InputStream themeStream = _context.getResources()
                                  .openRawResource(id);
        try {
          themeProps.load(themeStream);
        } catch (Throwable e) {
          Log.d("ColorScheme", e.toString());
        }
      }

      // Now, clean up the colors.
      String[] colors_tmp = themeProps.getProperty("mirc").split(";");

      colors = new int[colors_tmp.length];

      //Log.d("ColorScheme", themeProps.getProperty("mirc"));

      for (int i = 0; i < colors_tmp.length; i++) {
        if(colors_tmp[i].equals("")) continue;
        int c = Color.parseColor(colors_tmp[i]);
        colors[i] = c;
      }



      if (scheme_colors == null) {
        scheme_colors = new HashMap<String, Integer>();
      }

      scheme_colors.clear();
      // Pre-seed the color cache for the light/dark colors.
      scheme_colors.put("foreground", Color.parseColor(themeProps.getProperty("foreground."+(useDarkTheme?"dark":"light"))));
      scheme_colors.put("background", Color.parseColor(themeProps.getProperty("background."+(useDarkTheme?"dark":"light"))));

    }

  }

  private synchronized static int getColorCached(String name) {
    if(!scheme_colors.containsKey(name)) {
      scheme_colors.put(name, Color.parseColor(themeProps.getProperty(name)));
    }
    int c = scheme_colors.get(name);
    return c;
  }

  private synchronized void refreshColorScheme() {

    String scheme = _settings.getColorScheme();
    boolean darkTheme = _settings.getUseDarkColors();
    loadScheme(scheme, darkTheme);

  }

  public int getMircColor(int idx) {
    return colors[idx % colors.length];
  }

  public int getForeground() {
    return getColorCached("foreground");
  }

  public int getBackground() {
    return getColorCached("background");
  }

  public int getError() {
    return getColorCached("error");
  }

  public int getTopic() {
    return getColorCached("topic");
  }

  public int getChannelEvent() {
    return getColorCached("channelevent");
  }

  public int getUserEvent() {
    return getColorCached("userevent");
  }

  public int getServerEvent() {
    return getColorCached("serverevent");
  }

  public int getHighlight() {
    return getColorCached("highlight");
  }

  public int getUrl() {
    return getColorCached("url");
  }

  @Override
  public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
                                        String key) {
    if (key.equals(_context.getString(R.string.key_colorscheme)) || key.equals(_context.getString(R.string.key_colorscheme_dark))) {
      synchronized (colors) {
        refreshColorScheme();
      }
    }
  }

}
