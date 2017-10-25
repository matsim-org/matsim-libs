package playground.clruch.demo;

import ch.ethz.idsc.queuey.view.util.net.ObjectClient;
import ch.ethz.idsc.queuey.view.util.net.ObjectHandler;
import playground.clruch.net.SimulationObject;
import playground.clruch.net.SimulationServer;

/** demo that shows how to implement {@link ObjectHandler}
 * to receive {@link SimulationObject} from server. */
class SimulationSubscriberDemo {

    public static void main(String[] args) {
        try {
            ObjectClient client = //
                    new ObjectClient("localhost", SimulationServer.OBJECT_SERVER_PORT, //
                            new ObjectHandler() {
                                @Override
                                public void handle(Object object) {
//                                    System.out.println("object for simtime: " + simulationObject.now);

                                }
                            });
            while (client.isOpen()) {
                Thread.sleep(100);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
