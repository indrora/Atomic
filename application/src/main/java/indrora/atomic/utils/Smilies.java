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
package indrora.atomic.utils;

import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import indrora.atomic.R;
import indrora.atomic.model.Settings;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ImageSpan;
import android.util.Log;

/**
 * Class for handling graphical smilies in text messages.
 *
 * @author Liato
 */
public abstract class Smilies {
  private static final HashMap<String, Integer> mappings = new HashMap<String, Integer>();
  private static indrora.atomic.model.Settings _settings;

  static {
    // Putting these here saves time in smiley-heavy conversations.
    // It also preserves some amount of memory, as these live on the heap, not the stack.
    mappings.put(">:o", R.drawable.emoji_yell);
    mappings.put(">:-o", R.drawable.emoji_yell);
    mappings.put("O:)", R.drawable.emoji_innocent);
    mappings.put("O:-)", R.drawable.emoji_innocent);
    mappings.put(":)", R.drawable.emoji_smile);
    mappings.put(":-)", R.drawable.emoji_smile);
    mappings.put(":(", R.drawable.emoji_frown);
    mappings.put(":-(", R.drawable.emoji_frown);
    mappings.put(";)", R.drawable.emoji_wink);
    mappings.put(";-)", R.drawable.emoji_wink);
    mappings.put(":p", R.drawable.emoji_tongue_out);
    mappings.put(":-p", R.drawable.emoji_tongue_out);
    mappings.put(":P", R.drawable.emoji_tongue_out);
    mappings.put(":-P", R.drawable.emoji_tongue_out);
    mappings.put(":D", R.drawable.emoji_laughing);
    mappings.put(":-D", R.drawable.emoji_laughing);
    mappings.put(":[", R.drawable.emoji_embarassed);
    mappings.put(":-[", R.drawable.emoji_embarassed);
    mappings.put(":\\", R.drawable.emoji_undecided);
    mappings.put(":-\\", R.drawable.emoji_undecided);
    mappings.put(":o", R.drawable.emoji_surprised);
    mappings.put(":-o", R.drawable.emoji_surprised);
    mappings.put(":O", R.drawable.emoji_surprised);
    mappings.put(":-O", R.drawable.emoji_surprised);
    mappings.put(":*", R.drawable.emoji_kiss);
    mappings.put(":-*", R.drawable.emoji_kiss);
    mappings.put("8)", R.drawable.emoji_cool);
    mappings.put("8-)", R.drawable.emoji_cool);
    mappings.put(":!", R.drawable.emoji_foot_in_mouth);
    mappings.put(":-!", R.drawable.emoji_foot_in_mouth);
    mappings.put(":'(", R.drawable.emoji_cry);
    mappings.put(":'-(", R.drawable.emoji_cry);
    mappings.put(":X", R.drawable.emoji_sealed);
    mappings.put(":-X", R.drawable.emoji_sealed);
    mappings.put("o_O", R.drawable.emoji_wtf);
    mappings.put("O_o", R.drawable.emoji_wtf);
    mappings.put("XP", R.drawable.emoji_xp);
    mappings.put(";P", R.drawable.emoji_wink_tongue);
    mappings.put("-_-", R.drawable.emoji_null);
    mappings.put("X)", R.drawable.emoji_happy);
    mappings.put(":3", R.drawable.emoji_catface);
    mappings.put("o3o", R.drawable.emoji_catface_kiss);
    mappings.put(":'3", R.drawable.emoji_catface_cry);


  }

  /**
   * Converts all smilies in a string to graphical smilies.
   *
   * @param text A string with smilies.
   * @return A SpannableString with graphical smilies.
   */
  public static SpannableString toSpannable(SpannableString text, Context context) {
    if( _settings == null )
      _settings = new Settings(context.getApplicationContext());
    StringBuilder regex = new StringBuilder("(");
    String[] smilies = mappings.keySet().toArray(new String[mappings.size()]);

    for( int i = 0; i < smilies.length; i++ ) {
      regex.append(Pattern.quote(smilies[i]));
      regex.append("|");
    }

    regex.deleteCharAt(regex.length() - 1);
    regex.append(")");
    Pattern smiliematcher = Pattern.compile(regex.toString());
    Matcher m = smiliematcher.matcher(text);

    while ( m.find() ) {
      //Log.d("Smilies", "SID: "+mappings.get(m.group(1)).intValue());
      //Log.d("Smilies", "OID: "+R.drawable.emoji_smile);
      Drawable smilie = context.getResources().getDrawable(mappings.get(m.group(1)).intValue());

      // We should scale the image
      int height = _settings.getFontSize();
      float density = context.getResources().getDisplayMetrics().density;
      float scale = height / (float)(smilie.getMinimumHeight());
      smilie.setBounds(0, 0, (int)(smilie.getMinimumWidth() * scale * density), (int)(height * density));
      ImageSpan span = new ImageSpan(smilie, ImageSpan.ALIGN_BASELINE);
      text.setSpan(span, m.start(), m.end(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    return text;
  }

  /**
   * Converts all smilies in a string to graphical smilies.
   *
   * @param text A string with smilies.
   * @return A SpannableString with graphical smilies.
   */
  public static SpannableString toSpannable(String text, Context context) {
    return toSpannable(new SpannableString(text), context);
  }
}
