package playground.clruch.demo;

import playground.clruch.net.ObjectClient;
import playground.clruch.net.SimulationObject;
import playground.clruch.net.SimulationSubscriber;

/**
 * demo that shows how to implement {@link SimulationSubscriber} 
 * to receive {@link SimulationObject} from server.
 */
class SimulationSubscriberDemo {

    public static void main(String[] args) {
        try {
            ObjectClient client = new ObjectClient("localhost", new SimulationSubscriber() {
                @Override
                public void handle(SimulationObject simulationObject) {
                    System.out.println("object for simtime: " + simulationObject.now);

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
