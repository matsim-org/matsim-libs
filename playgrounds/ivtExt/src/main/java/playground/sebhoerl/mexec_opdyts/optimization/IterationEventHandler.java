package playground.sebhoerl.mexec_opdyts.optimization;

import org.matsim.core.events.handler.EventHandler;

public interface IterationEventHandler extends EventHandler {
    double getValue();
}
