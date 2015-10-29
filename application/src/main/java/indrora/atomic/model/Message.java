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
package indrora.atomic.model;

import indrora.atomic.App;
import indrora.atomic.utils.MircColors;
import indrora.atomic.utils.Smilies;

import java.util.Date;
import java.util.Locale;


import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.text.style.ImageSpan;
import android.widget.TextView;

/**
 * A channel or server message
 *
 * @author Sebastian Kaspari <sebastian@yaaic.org>
 */
public class Message implements Parcelable {

  protected Message(Parcel in) {
    text = in.readString();
    sender = in.readString();
    timestamp = in.readLong();
    type = in.readInt();
    icon = in.readInt();
  }

  public static final Creator<Message> CREATOR = new Creator<Message>() {
    @Override
    public Message createFromParcel(Parcel in) {
      return new Message(in);
    }

    @Override
    public Message[] newArray(int size) {
      return new Message[size];
    }
  };

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(Parcel parcel, int i) {
    parcel.writeString(text);
    parcel.writeString(sender);
    parcel.writeLong(timestamp);
    parcel.writeInt(type);
    parcel.writeInt(icon);
  }

  public enum MessageColor {
    USER_EVENT,
    CHANNEL_EVENT,
    SERVER_EVENT,
    TOPIC,
    HIGHLIGHT,
    ERROR,
    DEFAULT,
    NO_COLOR
  }

  private static Settings settings;

  /* normal message, this is the default */
  public static final int TYPE_MESSAGE = 0;
  public static final int TYPE_ACTION = 2;
  public static final int TYPE_SERVER = 3;

  /* join, part or quit */
  public static final int TYPE_MISC = 1;

  public static final int NO_ICON = -1;
  public static final int NO_TYPE = -1;
  public static final int NO_COLOR = -1;

  private final String text;
  private final String sender;
  private long timestamp;

  private MessageColor color = MessageColor.DEFAULT;
  private int type = NO_TYPE;
  private int icon = NO_ICON;

  /**
   * Create a new message without an icon defaulting to TYPE_MESSAGE
   *
   * @param text
   */
  public Message(String text) {
    this(text, null, TYPE_MESSAGE);
  }

  /**
   * Create a new message without an icon with a specific type
   *
   * @param text
   * @param type Message type
   */
  public Message(String text, int type) {
    this(text, null, type);
  }

  /**
   * Create a new message sent by a user, without an icon,
   * defaulting to TYPE_MESSAGE
   *
   * @param text
   * @param sender
   */
  public Message(String text, String sender) {
    this(text, sender, TYPE_MESSAGE);
  }

  /**
   * Create a new message sent by a user without an icon
   *
   * @param text
   * @param sender
   * @param type   Message type
   */
  public Message(String text, String sender, int type) {
    this(text, sender, type, new Date().getTime());

  }

  public Message(String text, String sender, int type, long time) {
    this.text = text;
    this.sender = sender;
    this.timestamp = time;
    this.type = type;
    if( settings == null ) {
      settings = App.getSettings();
    }

  }


  /**
   * Set the message's icon
   */
  public void setIcon(int icon) {
    this.icon = icon;
  }

  /**
   * Get the message's icon
   *
   * @return
   */
  public int getIcon() {
    return icon;
  }

  /**
   * Get the text of this message
   *
   * @return
   */
  public String getText() {
    return text;
  }

  /**
   * Get the type of this message
   *
   * @return One of Message.TYPE_*
   */
  public int getType() {
    return type;
  }

  public void setType(int t) {
    this.type = t;
  }

  /**
   * Set the color of this message
   */
  public void setColor(MessageColor color) {
    this.color = color;
  }

  private static int translateColor(ColorScheme scheme, MessageColor c) {
    switch ( c ) {
      case CHANNEL_EVENT:
        return scheme.getChannelEvent();
      case DEFAULT:
        return scheme.getForeground();
      case ERROR:
        return scheme.getError();
      case HIGHLIGHT:
        return scheme.getHighlight();
      case SERVER_EVENT:
        return scheme.getServerEvent();
      case TOPIC:
        return scheme.getTopic();
      case USER_EVENT:
        return scheme.getUserEvent();
      default:
        return scheme.getForeground();
    }

  }

  /**
   * Set the timestamp of the message
   *
   * @param timestamp
   */
  public void setTimestamp(long timestamp) {
    this.timestamp = timestamp;
  }


  private int getSenderColor(ColorScheme scheme) {
    return getSenderColor(this.sender, scheme);
  }

  /**
   * Associate a color with a sender name
   *
   * @return a color hexa
   */
  public static int getSenderColor(String sender, ColorScheme scheme) {
    /* It might be worth to use some hash table here */
    if( sender == null ) {
      return scheme.getForeground();
    }

    if( !App.getSettings().showColorsNick() ) {
      return scheme.getForeground();
    }

    int color = 0;
    int variant = sender.charAt(0);

    for( int i = 0; i < sender.length(); i++ ) {
      char c = sender.charAt(i);
      if( c - 33 > 'Z' )
        variant += (c - 33) % 32;
      else
        variant -= (c - 33) % 32;
      color += c;
    }

    variant %= 20;
    // We don't want the color to be the background color.

    final int bg = scheme.getBackground();
    int tmpColor;// = _scheme.getMircColor(color);
    do {

      float[] hsv = new float[3];
      Color.colorToHSV(scheme.getMircColor(color++), hsv);
      hsv[0] += variant;

      tmpColor = Color.HSVToColor(hsv);
    } while ( likeness(bg, tmpColor) < 30 );

    return tmpColor; //colors[color];
  }

  /**
   * Calculates a likeness. This will return between 0-255
   * on the likeness of the color.
   *
   * @param back
   * @param fore
   * @return
   */
  private static int likeness(int back, int fore) {


    double gamma = 2.2; // Woo constants.
    double backL =
        0.2126 * Math.pow((float)Color.red(back) / 255.0, gamma)
            + 0.7152 * Math.pow((float)Color.green(back) / 255.0, gamma)
            + 0.0722 * Math.pow((float)Color.blue(back) / 255.0, gamma);
    double foreL =
        0.2126 * Math.pow((float)Color.red(fore) / 255.0, gamma)
            + 0.7152 * Math.pow((float)Color.green(fore) / 255.0, gamma)
            + 0.0722 * Math.pow((float)Color.blue(fore) / 255.0, gamma);
    int distance = (int)(255 * Math.abs(backL - foreL));
    return distance;
  }

  private SpannableString _cache = null;

  MessageRenderParams currentParams = new MessageRenderParams();

  Conversation _parent;

  protected void setConversation(Conversation p) {
    _parent = p;
  }

  public static SpannableString render(Message msg, MessageRenderParams renderParams) {

    ColorScheme renderedScheme = new ColorScheme(renderParams.colorScheme, renderParams.useDarkScheme);

    SpannableString nickSS;
    SpannableString timeSS;
    SpannableString messageSS;
    SpannableString prefixSS;

    // format the sender name
    if( msg.hasSender() ) {
      // The sender name spannable is just the name of the sender
      nickSS = new SpannableString(msg.sender);
      // Defaultly, the foreground color is used.
      int senderColor = renderedScheme.getForeground();


      if( renderParams.nickColors ) {
        // getSenderColor does a variant color from the color scheme options.
        senderColor = getSenderColor(msg.sender, renderedScheme); // msg.getSenderColor();
      }


      // We should now set the spannable's color.
      nickSS.setSpan(new ForegroundColorSpan(senderColor), 0, nickSS.length(), SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE);
      // and wrap it in our preferred <'s
      if( msg.type == TYPE_MESSAGE ) {
        nickSS = new SpannableString(TextUtils.concat("<", nickSS, ">"));
      }
    } else {
      // There's no sender, so we give it no sender.
      nickSS = new SpannableString("");
    }

    // Timestamps are handled in much the same way as the Sender, however they're much simpler.
    if( settings.showTimestamp() ) {
      timeSS = new SpannableString(msg.renderTimeStamp(renderParams.timestamp24Hour, renderParams.timestampSeconds));
    } else {
      timeSS = new SpannableString("");
    }

    // Prefix.
    // this is a space normally, however it becomes a *, then is filled with the appropriate
    // drawables.
    prefixSS = new SpannableString(" ");
    // If there is an icon,
    if( msg.hasIcon() ) {
      // We want to handle having an icon and not wanting to show icons.
      // this makes things a little nicer.
      prefixSS = new SpannableString("•");
      // If we really want to show icons...
      if( renderParams.icons ) {

        Drawable drawable = App.getSResources().getDrawable(msg.icon);

        float density = App.getSResources().getDisplayMetrics().density;
        // scale = wanted / actual
        // This call is < x,y, width,height>
        // SpaceWidth is going to be in raw pixels, so we need to multiply it by density.
        // Height is going to be the drawable intrinsic height * scale * density
        // We need y to be 1/2 the height of the line, minus one half the height of the scaled drawable.

        Paint p = new Paint();
        p.setTypeface(Typeface.MONOSPACE);
        p.setTextSize(settings.getFontSize()*density);
        int width = (int)(p.measureText(" "));

        int fontheight = settings.getFontSize();

        drawable.setBounds(0, (int)(-0.5*fontheight), (int)(width), (int)(width));
        // And now we do the magic.
        prefixSS.setSpan(new ImageSpan(drawable, ImageSpan.ALIGN_BASELINE), 0, 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

      }
    }

    // Now to the actual message.
    // How this works is that we're going to render it to a string,
    // if mIRC colors
    //   => highlight color?
    //      => blarg the spannable.
    //   => blarg mIRC colors into the spannable.
    // else
    //   => strip mIRC color tags from the text block
    //   => highlight color?
    //      => blarg the spannable.
    messageSS = new SpannableString(msg.text);

    if( renderParams.mircColors ) {
      if( msg.hasColor() && renderParams.messageColors ) {
        messageSS.setSpan(new ForegroundColorSpan(translateColor(renderedScheme, msg.color)), 0,
                messageSS.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
      }
      messageSS = MircColors.toSpannable(messageSS, renderedScheme);
    } else {
      messageSS = new SpannableString(MircColors.removeStyleAndColors(msg.text));
      if( msg.hasColor() && renderParams.messageColors ) {
        messageSS.setSpan(new ForegroundColorSpan(translateColor(renderedScheme, msg.color)), 0,
                messageSS.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
      }
    }
    // Smash smileys into this, but only if we're not a server message..
    if( renderParams.smileys && msg.type != TYPE_SERVER ) {
      messageSS = Smilies.toSpannable(messageSS, App.getAppContext());
    }

    return new SpannableString(TextUtils.concat(timeSS, prefixSS, nickSS, " ", messageSS));

  }

  /**
   * Render message as spannable string
   *
   * @return
   */
  public TextView render(TextView convertView) {

    // check if we just want to return ourselves already.

    if(!settings.getRenderParams().equals(currentParams)) {
      currentParams = settings.getRenderParams();
    }

    Message t = (Message)convertView.getTag();
    if(this.equals(t) && settings.getRenderParams().equals(t.currentParams)) {
      return convertView;
    }

    ColorScheme currentScheme = new ColorScheme(currentParams.colorScheme, currentParams.useDarkScheme);

    //_cache = new SpannableString(TextUtils.concat(timeSS, prefixSS, nickSS, " ", messageSS));

    convertView.setTextColor(currentScheme.getForeground());
    convertView.setLinkTextColor(currentScheme.getUrl());
    convertView.setText(Message.render(this, currentParams));
    convertView.setTag(this);

    currentParams = settings.getRenderParams();
    return convertView;

  }

  /**
   * Does this message have a sender?
   *
   * @return
   */
  public boolean hasSender() {
    return sender != null;
  }

  /**
   * Get the sender
   * @return the sender name
   */
  public String getSender() { return sender; }

  /**
   * Does this message have a color assigned?
   *
   * @return
   */
  private boolean hasColor() {
    return color != MessageColor.NO_COLOR;
  }

  /**
   * Does this message have an icon assigned?
   *
   * @return
   */
  private boolean hasIcon() {
    return icon != NO_ICON;
  }

  /**
   * Generate a timestamp
   *
   * @param use24hFormat
   * @return
   */
  public String renderTimeStamp(boolean use24hFormat, boolean includeSeconds) {

    Date date = new Date(timestamp);
    String format = "[";

    format += (use24hFormat ? "HH" : "hh");
    format += ":mm";

    if( includeSeconds ) {
      format += ":ss";
    }
    format += "]";

    return (String)(new java.text.SimpleDateFormat(format, Locale.US)).format(date);
  }

  @Override
  public boolean equals(Object o) {

    Message m = (Message)o;

    // If it's null, it's not us.
    if(m == null) return false;

    // if its icon is different, it's not us.
    if(this.hasIcon() != m.hasIcon()) return false;
    if(m.icon != this.icon) return false;

    // if the text doesn't match, it's not us.
    if(!this.text.equals(m.text)) return false;

    // if the sender isn't the same, it's not us.
    if(this.hasSender() != m.hasSender()) return false;
    if( this.sender != null && !this.sender.equals(m.sender) ) return false;

    // if the timestamp isn't the same, it's not us.
    if(this.timestamp != m.timestamp) return false;

    // if the type isn't the same, it's not us.
    if(this.type != m.type) return false;

    // if the message color isn't the same, it's not us.
    if(this.color != m.color) return false;

    // We're here, and that's fine.
    return true;

  }
}
