package de.ii.xtraplatform.cli.dev;

import de.ii.xtraplatform.cli.CommandHandler;
import de.ii.xtraplatform.cli.EntitiesHandler;
import de.ii.xtraplatform.cli.Progress;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.RemoteEndpoint;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpoint;
import java.io.IOException;

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

    Progress tracker =
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
