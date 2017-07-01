package playground.clruch.trb18.traveltime;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.router.util.TravelTime;
import org.matsim.vehicles.Vehicle;

public class TRBTravelTime implements TravelTime {
    final private TRBTravelTimeTracker tracker;

    public TRBTravelTime(TRBTravelTimeTracker tracker) {
        this.tracker = tracker;
    }

    @Override
    public double getLinkTravelTime(Link link, double time, Person person, Vehicle vehicle) {
        return tracker.getTravelTime(link, time);
    }
}
