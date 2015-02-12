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

import java.text.SimpleDateFormat;
import java.util.TimeZone;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;


import indrora.atomic.R;
import android.app.Activity;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.text.Html;
import android.widget.TextView;

/**
 * About activity
 *
 * @author Sebastian Kaspari <sebastian@yaaic.org>
 */
public class AboutActivity extends Activity {
  /**
   * On activity getting created.
   */
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.about);

    getActionBar().setDisplayHomeAsUpEnabled(true);

    try {
      
      PackageInfo pi = getPackageManager().getPackageInfo(getPackageName(), 0);
      
      String info = String.format("Version %1$s (r%2$d)", pi.versionName,pi.versionCode);

      try{
        ApplicationInfo ai = getPackageManager().getApplicationInfo(getPackageName(), 0);
        ZipFile zf = new ZipFile(ai.sourceDir);
        ZipEntry ze = zf.getEntry("META-INF/MANIFEST.MF");
        long time = ze.getTime();
        SimpleDateFormat formatter = (SimpleDateFormat) SimpleDateFormat.getInstance();
        formatter.setTimeZone(TimeZone.getTimeZone("gmt"));
        String s = formatter.format(new java.util.Date(time));
        info += "\nBuilt on: "+s;
        zf.close();
     }catch(Exception e){
       System.out.println("That was odd: I couldn't get the date I was built.");
       System.out.println(e.toString());
     }

      ((TextView)findViewById(R.id.version_label)).setText(
          info
       );
    } catch (NameNotFoundException e) {
      ((TextView)findViewById(R.id.version_label)).setText("Dev release???");
    }

    TextView licenseDetails = (TextView) findViewById(R.id.about_license_info);
    licenseDetails.setText(Html.fromHtml(getString(R.string.licence_info)));


  }
}
