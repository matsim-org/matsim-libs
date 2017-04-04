package playground.clruch.net;

import java.util.concurrent.CopyOnWriteArraySet;

public class SimulationClientSet extends CopyOnWriteArraySet<SimulationSubscriber> {
    public static final SimulationClientSet INSTANCE = new SimulationClientSet();

    private SimulationClientSet() {
    }

}
