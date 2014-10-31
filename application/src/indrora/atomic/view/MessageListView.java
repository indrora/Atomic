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
package indrora.atomic.view;

import indrora.atomic.App;
import indrora.atomic.activity.ConversationActivity;
import indrora.atomic.adapter.MessageListAdapter;
import indrora.atomic.listener.MessageClickListener;
import indrora.atomic.model.ColorScheme;
import android.content.Context;
import android.widget.ListView;

/**
 * A customized ListView for Messages
 *
 * @author Sebastian Kaspari <sebastian@yaaic.org>
 */
public class MessageListView extends ListView {

  ColorScheme _scheme;

  /**
   * Create a new MessageListView
   *
   * @param context
   */
  public MessageListView(Context context) {
    super(context);

    _scheme = App.getColorScheme();

    setOnItemClickListener(MessageClickListener.getInstance());

    setDivider(null);

    setFastScrollEnabled(true);
    
    setCacheColorHint(_scheme.getBackground());
    setVerticalFadingEdgeEnabled(false);
    setScrollBarStyle(SCROLLBARS_INSIDE_OVERLAY);

    setBackgroundColor(_scheme.getBackground());

    // Scale padding by screen density
    float density = context.getResources().getDisplayMetrics().density;
    int padding = (int) (5 * density);
    setPadding(padding, padding, padding, padding);

    setTranscriptMode(TRANSCRIPT_MODE_NORMAL);
  }

  /**
   * Get the adapter of this MessageListView
   * (Helper to avoid casting)
   *
   * @return The MessageListAdapter
   */
  @Override
  public MessageListAdapter getAdapter() {
    return (MessageListAdapter) super.getAdapter();
  }
}
