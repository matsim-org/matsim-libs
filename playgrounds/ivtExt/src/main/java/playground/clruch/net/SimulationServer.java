// code by jph
package playground.clruch.net;

import java.net.Socket;
import java.util.Timer;

public class SimulationServer extends AbstractServer {
    public static final int OBJECT_SERVER_PORT = 9380;

    public static final SimulationServer INSTANCE = new SimulationServer();

    @Override
    protected int getPort() {
        return OBJECT_SERVER_PORT;
    }

    @Override
    protected void handleConnection(Socket socket, Timer timer) {
        new ObjectSocket(socket);
    }

}
