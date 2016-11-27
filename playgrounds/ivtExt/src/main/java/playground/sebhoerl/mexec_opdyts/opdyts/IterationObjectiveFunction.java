package playground.sebhoerl.mexec_opdyts.opdyts;

import org.matsim.core.events.handler.EventHandler;
import playground.sebhoerl.mexec.Simulation;

public interface IterationObjectiveFunction {
    double compute(Simulation simulation, Long iteration);
}
