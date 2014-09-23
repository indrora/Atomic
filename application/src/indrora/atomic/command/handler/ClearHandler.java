package indrora.atomic.command.handler;

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

    if(params.length == 2) {
      Conversation tmp = server.getConversation(params[1]);
      if(tmp == null)
      {
        Message m = new Message("Unkown conversation", Message.TYPE_MISC);
        m.setColor(MessageColor.ERROR);
        m.setIcon(indrora.atomic.R.drawable.error);
        
        conversation.addMessage(m);
        service.sendBroadcast(Broadcast.createConversationIntent(Broadcast.CONVERSATION_MESSAGE, server.getId(), conversation.getName()));
        return;
      }
      else
      {
        conversation = tmp;
      }
    }
    
    Log.d("ClearHandler", "Clearing conversation " + conversation.getName());
    Intent intent = Broadcast.createConversationIntent(
        Broadcast.CONVERSATION_CLEAR, server.getId(),
        conversation.getName());
    service.sendBroadcast(intent);


  }

  @Override
  public String getUsage() {
    // TODO Auto-generated method stub
    return "/clear <channel>";
  }

  @Override
  public String getDescription(Context context) {
    // TODO Auto-generated method stub
    return "Clear the buffer of a channel";
  }

}
