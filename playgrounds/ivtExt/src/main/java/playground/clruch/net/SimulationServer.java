// code by jph
package playground.clruch.net;

import java.net.Socket;
import java.util.Timer;

import ch.ethz.idsc.queuey.view.util.net.AbstractServer;
import ch.ethz.idsc.queuey.view.util.net.ObjectSocket;

public class SimulationServer extends AbstractServer {
    public static final int OBJECT_SERVER_PORT = 9380;

    public static final SimulationServer INSTANCE = new SimulationServer();

    @Override
    protected int getPort() {
        return OBJECT_SERVER_PORT;
    }

    @Override
    protected void handleConnection(Socket socket, Timer timer) {
        ObjectSocket os = new ObjectSocket(socket) {
            @Override
            public void close() throws Exception {
                super.close();
                SimulationClientSet.INSTANCE.remove(this);
            }
        };
        SimulationClientSet.INSTANCE.add(os);
    }

}
