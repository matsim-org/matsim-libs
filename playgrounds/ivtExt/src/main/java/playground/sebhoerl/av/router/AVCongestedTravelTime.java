package playground.sebhoerl.av.router;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.router.util.TravelTime;
import org.matsim.vehicles.Vehicle;

public class AVCongestedTravelTime implements AVTravelTime {
    final private AVCongestionTracker tracker;
    
    public AVCongestedTravelTime(AVCongestionTracker tracker) {
        this.tracker = tracker;
    }
    
    @Override
    public double getLinkTravelTime(Link link, double time, Person person, Vehicle vehicle) {
        return tracker.getLinkTime(link.getId(), time);
    }
}
