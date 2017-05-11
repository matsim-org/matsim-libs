package playground.clruch.gfx;

import playground.clruch.net.ObjectClient;
import playground.clruch.net.SimulationObject;
import playground.clruch.net.SimulationSubscriber;

@Deprecated
class Test {

    public static void main(String[] args) {
        // SFtoWGS84 asd = new SFtoWGS84();
        // Coord res;
        // res = asd.transform(new Coord(678253.4, 4831005.));
        // System.out.println(res);

        // WGS84toSiouxFalls asd = new WGS84toSiouxFalls();
        // Coord res;
        // res = asd.transform(new Coord(43.50030884149,-96.68137192726));
        // System.out.println(res);

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
