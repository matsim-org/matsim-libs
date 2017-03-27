package playground.clruch.net;

import java.util.concurrent.CopyOnWriteArraySet;

import playground.sebhoerl.avtaxi.passenger.AVRequest;

public class SimulationSubscriberSet extends CopyOnWriteArraySet<SimulationSubscriber> {
    public static final SimulationSubscriberSet INSTANCE = new SimulationSubscriberSet();

    private SimulationSubscriberSet() {
    }

}
