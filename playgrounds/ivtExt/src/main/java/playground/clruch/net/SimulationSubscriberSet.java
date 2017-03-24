package playground.clruch.net;

import java.util.concurrent.CopyOnWriteArraySet;

public class SimulationSubscriberSet extends CopyOnWriteArraySet<SimulationSubscriber> {
    public static final SimulationSubscriberSet INSTANCE = new SimulationSubscriberSet();

    private SimulationSubscriberSet() {
    }

}
