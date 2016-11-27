package playground.sebhoerl.mexec_opdyts.opdyts;

import org.apache.commons.lang3.event.EventUtils;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsReaderXMLv1;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.handler.EventHandler;
import playground.sebhoerl.mexec.Simulation;

public class EventsBasedObjectiveFunction implements IterationObjectiveFunction {
    final private IterationEventHandler handler;

    public EventsBasedObjectiveFunction(IterationEventHandler handler) {
        this.handler = handler;
    }

    @Override
    public double compute(Simulation simulation, Long iteration) {
        handler.reset(iteration.intValue());

        EventsManager events = EventsUtils.createEventsManager();
        events.addHandler(handler);

        EventsReaderXMLv1 reader = new EventsReaderXMLv1(events);
        reader.parse(simulation.getEvents(iteration));

        return handler.getValue();
    }
}
