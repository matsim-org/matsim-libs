package playground.sebhoerl.av.router;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.vehicles.Vehicle;

public class AVSpeedTravelTime implements AVTravelTime {
    final private AVSpeedTracker tracker;
    
    public AVSpeedTravelTime(AVSpeedTracker tracker) {
        this.tracker = tracker;
    }
    
    @Override
    public double getLinkTravelTime(Link link, double time, Person person, Vehicle vehicle) {
        System.err.println(String.format("%f %f", link.getFreespeed(), tracker.getSpeed(link.getId())));
        return link.getLength() / tracker.getSpeed(link.getId());
    }
}
