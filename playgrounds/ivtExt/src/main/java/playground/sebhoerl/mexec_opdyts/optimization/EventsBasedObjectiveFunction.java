package playground.sebhoerl.mexec_opdyts.optimization;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.EventsReaderXMLv1;
import org.matsim.core.events.EventsUtils;
import playground.sebhoerl.mexec.Simulation;
import playground.sebhoerl.mexec.SimulationUtils;

public class EventsBasedObjectiveFunction implements IterationObjectiveFunction {
    final private IterationEventHandler handler;

    public EventsBasedObjectiveFunction(IterationEventHandler handler) {
        this.handler = handler;
    }

    @Override
    public double compute(Simulation simulation, IterationState iteration) {
        handler.reset((int) iteration.getIteration());

        Logger.getLogger(EventsManagerImpl.class).setLevel(Level.OFF);

        EventsManager events = EventsUtils.createEventsManager();
        events.addHandler(handler);

        SimulationUtils.processEvents(events, simulation, iteration.getIteration());

        return handler.getValue();
    }
}
