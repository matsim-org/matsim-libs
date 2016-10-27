package playground.sebhoerl.av.router;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.vehicles.Vehicle;

public class AVFlowDensityTravelTime implements AVTravelTime {
    final private AVFlowDensityTracker tracker;
    
    public AVFlowDensityTravelTime(AVFlowDensityTracker tracker) {
        this.tracker = tracker;
    }
    
    @Override
    public double getLinkTravelTime(Link link, double time, Person person, Vehicle vehicle) {
        return link.getLength() / tracker.getSpeed(link.getId());
    }
}
