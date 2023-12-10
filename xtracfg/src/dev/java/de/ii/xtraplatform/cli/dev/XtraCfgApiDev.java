package de.ii.xtraplatform.cli.dev;

import de.ii.xtraplatform.cli.CommandHandler;
import de.ii.xtraplatform.cli.EntitiesHandler;
import java.io.IOException;
import java.util.function.Consumer;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.RemoteEndpoint;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

@ServerEndpoint("/sock")
public class XtraCfgApiDev {

  private final CommandHandler commandHandler;

  public XtraCfgApiDev() {
    this.commandHandler = new CommandHandler();
  }

  @OnOpen
  public void open(Session session) {
    session.setMaxIdleTimeout(0);
  }

  @OnMessage
  public void handleTextMessage(Session session, String message) throws IOException {
    RemoteEndpoint.Basic remote = session.getBasicRemote();

    Consumer<String> tracker =
        progress -> {
          try {
            remote.sendText(progress);
          } catch (IOException e) {
            // ignore
          }
        };

    // TODO?
    EntitiesHandler.DEV = true;

    String result = commandHandler.handleCommand(message, true, tracker);

    remote.sendText(result);
  }
}
