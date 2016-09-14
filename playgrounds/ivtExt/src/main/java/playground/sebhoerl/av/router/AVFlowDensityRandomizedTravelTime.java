package playground.sebhoerl.av.router;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.vehicles.Vehicle;

public class AVFlowDensityRandomizedTravelTime implements AVTravelTime {
    final private AVLinkSpeedMutator mutator;
    final private AVFlowDensityTracker tracker;
    
    public AVFlowDensityRandomizedTravelTime(AVFlowDensityTracker tracker, AVLinkSpeedMutator mutator) {
        this.mutator = mutator;
        this.tracker = tracker;
    }
    
    @Override
    public double getLinkTravelTime(Link link, double time, Person person, Vehicle vehicle) {
        return link.getLength() / mutator.mutateLinkSpeed(tracker.getSpeed(link.getId()), time);
    }
}
