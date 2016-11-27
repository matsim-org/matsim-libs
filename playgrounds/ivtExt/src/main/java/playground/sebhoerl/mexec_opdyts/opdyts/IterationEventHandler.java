package playground.sebhoerl.mexec_opdyts.opdyts;

import org.matsim.core.events.handler.EventHandler;

public interface IterationEventHandler extends EventHandler {
    double getValue();
}
