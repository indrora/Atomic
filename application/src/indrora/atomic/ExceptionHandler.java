package indrora.atomic;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;

import android.content.Context;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

public class ExceptionHandler implements Thread.UncaughtExceptionHandler {

	
	Thread.UncaughtExceptionHandler _default = null;
	
	public ExceptionHandler(Thread.UncaughtExceptionHandler parent)
	{
		_default = parent;
	}
	
	private static String NL= "\r\n";
	
	@Override
	public void uncaughtException(Thread arg0, Throwable arg1) {
		
		// We have no context or anything to ride on here, so what we're going to do now is just write out a log.
		
		String filename = Environment.getExternalStorageDirectory().getPath()+"/atomic-crash-"+(new Date().getTime())+".log";

		try {
			java.io.FileWriter fw = new FileWriter(filename);
			dumpException(new FileWriter(filename), arg1);
			Toast.makeText(App.getApp(), "Crash log written to "+filename, Toast.LENGTH_LONG).show();
		} catch (IOException e) {
			Log.e("Application", "Unable to open file for writing; File writer stack trace here:");
			e.printStackTrace();
			Log.e("Application", "Real stack trace follows...");
			arg1.printStackTrace();
		}
		
		_default.uncaughtException(arg0, arg1);
	}
	
	private void dumpException(final FileWriter fw, final Throwable th) throws IOException
	{
		fw.append("Start exception log: "+th.getClass().getName()+NL);
		
		fw.append("-- exception message --"+NL);
		fw.append(th.getMessage()+NL);
		
		fw.append("-- stack trace --"+NL);
		
		for(StackTraceElement trace_element : th.getStackTrace())
		{
			if(trace_element.isNativeMethod())
			{
				fw.append("(native) ");
			}
			fw.append(trace_element.getMethodName()+NL);
			fw.append("\t"+trace_element.getFileName()+":"+trace_element.getLineNumber()+NL);
		}
		
		fw.append(NL);
		if(th.getCause() != null)
		{
			dumpException(fw, th.getCause());
		}
		

	}
	

}
