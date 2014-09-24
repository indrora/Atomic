package indrora.atomic.command.handler;

import java.util.Locale;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import indrora.atomic.command.BaseHandler;
import indrora.atomic.exception.CommandException;
import indrora.atomic.irc.IRCService;
import indrora.atomic.model.Broadcast;
import indrora.atomic.model.Conversation;
import indrora.atomic.model.Message;
import indrora.atomic.model.Server;
import indrora.atomic.model.Message.MessageColor;

public class ClearHandler extends BaseHandler {

  @Override
  public void execute(String[] params, Server server,
      Conversation conversation, IRCService service) throws CommandException {

    
    // If we aren't passed any actual arguments, close the current conversation.
    if (params.length == 1) {
      Log.d("ClearHandler", "Clearing conversation " + conversation.getName());
      service
          .sendBroadcast(Broadcast.createConversationIntent(
              Broadcast.CONVERSATION_CLEAR, server.getId(),
              conversation.getName()));
      return;
    }
    // If one argument is given and it's value is "all" or "*",
    else if(params.length == 2 && ( params[1].toLowerCase(Locale.getDefault()).equals("all") || params[1].equals("*") ) )
    {
      // Clear all the channels.
      for(Conversation c : server.getConversations() ) {
        service.sendBroadcast( Broadcast.createConversationIntent(
            Broadcast.CONVERSATION_CLEAR, server.getId(),
            c.getName()) );
      }
      return;
    }
    // Otherwise, there are more than 1 argument 
    else
    {
      // For each of the actual arguments
      for(int i = 1; i < params.length; i++) {
        // Get the conversation name
        Conversation cc = server.getConversation(params[i]);
        // So long as the conversation exists and isn't the server window.
        if(cc != null && cc.getType()!= Conversation.TYPE_SERVER ) {
          service.sendBroadcast(Broadcast.createConversationIntent(Broadcast.CONVERSATION_CLEAR, server.getId(), cc.getName()));
        }
        // Otherwise, if the conversation doesn't actually exist
        else if(cc == null) {
          // Fling a message that says it doesn't exist
          Message m = new Message("Unkown conversation "+params[i]);
          m.setColor(MessageColor.ERROR);
          m.setType(Message.TYPE_SERVER);
          m.setIcon(indrora.atomic.R.drawable.error);
          server.getConversation(null).addMessage(m);
          service.sendBroadcast(Broadcast.createConversationIntent(Broadcast.CONVERSATION_MESSAGE, server.getId(), null));
        }
      }
    }

  }

  @Override
  public String getUsage() {
    // TODO Auto-generated method stub
    return "/clear [(all|*)|channel [channel ...]]";
  }

  @Override
  public String getDescription(Context context) {
    // TODO Auto-generated method stub
    return "Clear the scroll buffer of the current channel or given set of channels. Use \"all\" or \"*\" for all channels (including server window!)";
  }

}
