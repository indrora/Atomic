package indrora.atomic;

import indrora.atomic.model.ColorScheme;
import indrora.atomic.utils.LatchingValue;
import android.app.Application;

public class App extends Application {

	Atomic atomic;
	public App()
	{
		super();
		
		autoconnectComplete = new LatchingValue<Boolean>(true, false);
	}
	
	private static LatchingValue<Boolean> autoconnectComplete;
	
	public static Boolean doAutoconnect()
	{
		return autoconnectComplete.getValue();
	}
	
@Override
	public void onCreate() {
		// Context exists here.
		ColorScheme _c = new ColorScheme(getApplicationContext());
		Atomic.getInstance().loadServers(getApplicationContext());
		super.onCreate();
	}

}
