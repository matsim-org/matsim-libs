package contrib.baseline.calibration.location_choice;

import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.scoring.EventsToActivities;
import org.matsim.core.scoring.EventsToLegs;
import playground.sebhoerl.av_paper.EventsToTrips;
import playground.sebhoerl.mexec.Simulation;
import playground.sebhoerl.mexec_opdyts.optimization.IterationEventHandler;
import playground.sebhoerl.mexec_opdyts.optimization.IterationObjectiveFunction;
import playground.sebhoerl.mexec_opdyts.optimization.IterationState;

class DistanceObjective implements IterationObjectiveFunction {
    @Override
    public double compute(Simulation simulation, IterationState iteration) {
        EventsManager events = EventsUtils.createEventsManager();

        EventsToTrips events2trips = new EventsToTrips(new EventsToLegs(scenario), new EventsToActivities());

        new MatsimEventsReader(events).readStream(simulation.getEvents());
    }
}