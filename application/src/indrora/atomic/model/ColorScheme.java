package indrora.atomic.model;

import indrora.atomic.R;
import indrora.atomic.R.xml;

import java.lang.reflect.Field;

import org.xmlpull.v1.XmlPullParser;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.XmlResourceParser;
import android.graphics.Color;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;


public class ColorScheme implements OnSharedPreferenceChangeListener {

	static Context _context;
	static Settings _settings;
	
	private int foreground;
	private int background;
	
	/*
	 * 
	 * 	public static final int COLOR_USER_EVENT     = 0xDEADBEEF;
    public static final int COLOR_ERROR          = 0xDEADCAFE;
    public static final int COLOR_CHANNEL_EVENT  = 0xBEEFBEEF;
    public static final int COLOR_TOPIC          = 0xCAFEBEEF;
    public static final int COLOR_SERVER_EVENT   = 0xBEEFCAFE;

	 * 
	 */
	
	
	
	private int user_event;
	private int error;
	private int channel_event;
	private int topic;
	private int server_event;
	private int highlight;
	
	private int url;
	
	private int[] colors = new int[16];
	
	
	public ColorScheme(Context ctx)
	{
		// initialize ourselves.
		if(ctx != null) {_context = ctx; }
		
		if(_settings == null) _settings = new Settings(_context);
		refreshColorScheme();
		
		PreferenceManager.getDefaultSharedPreferences(_context).registerOnSharedPreferenceChangeListener(this);
	
	}
	
	
	
	private synchronized void refreshColorScheme()
	{
		
		
		// things!
		int id = 0;
		String scheme = _settings.getColorScheme();
		Class<xml> xml_res = R.xml.class;
		try {
			Field scheme_field = xml_res.getField("theme_"+scheme);
			id = scheme_field.getInt(null);
		} catch (Throwable e) {
			id = R.xml.theme_default;
		}
		int color_idx = 0;
		XmlResourceParser xml_parser = _context.getResources().getXml(id);
		try {
			
			while(xml_parser.getEventType() != XmlPullParser.END_DOCUMENT)
			{
				if(xml_parser.getEventType() == XmlPullParser.START_TAG)
				{
					String TagName = xml_parser.getName();
					if(TagName.equals("colorscheme")) { /* Do do dooo */}
					// Foreground and background
					else if(TagName.equals("foreground")) {foreground = Color.parseColor(xml_parser.nextText());}
					else if(TagName.equals("background")){background = Color.parseColor(xml_parser.nextText());}
					// Events that can happen
				    else if(TagName.equals("userevent")){user_event = Color.parseColor(xml_parser.nextText());}
				    else if(TagName.equals("channelevent")){channel_event = Color.parseColor(xml_parser.nextText());}
				    else if(TagName.equals("serverevent")){server_event = Color.parseColor(xml_parser.nextText());}
				    else if(TagName.equals("error")) {error = Color.parseColor(xml_parser.nextText());}
				    else if(TagName.equals("topic")){topic = Color.parseColor(xml_parser.nextText());}
				    else if(TagName.equals("highlight")){highlight = Color.parseColor(xml_parser.nextText());}
					// URLs.
				    else if(TagName.equals("url")){url=Color.parseColor(xml_parser.nextText());}
					// The 16 standard colors.
					else if(TagName.equals("color")){
						colors[color_idx] = Color.parseColor(xml_parser.nextText());
						color_idx++;
					
					}
				}
				xml_parser.next();
			}
			xml_parser.close();
		} catch (Throwable e) {
			Toast.makeText(_context, "So, a bad thing happened.",Toast.LENGTH_LONG).show();
			Log.e("ColorScheme", e.toString());
			e.printStackTrace();
		}
		
	}

	public int getColor(int idx)
	{
		return colors[idx % colors.length];
	}
	
	public int getForeground()
	{
		return foreground;
	}
	public int getBackground()
	{
		return background;
	}
	
	public int getError() { return error; }
	public int getTopic() {return topic;  }
	public int getChannelEvent() {return channel_event; }
	public int getUserEvent() { return user_event; }
	public int getServerEvent() {return server_event; }
	public int getHighlight() { return highlight; }
	public int getUrl() {return url; }
	
	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {
		if(key == "colorscheme")
			refreshColorScheme();
		
	}
	
	
}
