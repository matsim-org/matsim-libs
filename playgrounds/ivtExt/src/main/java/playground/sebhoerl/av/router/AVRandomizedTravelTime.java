package playground.sebhoerl.av.router;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.router.util.TravelTime;
import org.matsim.vehicles.Vehicle;

public class AVRandomizedTravelTime implements AVTravelTime {
    final private AVLinkSpeedMutator mutator;
    
    public AVRandomizedTravelTime(AVLinkSpeedMutator mutator) {
        this.mutator = mutator;
    }
    
    @Override
    public double getLinkTravelTime(Link link, double time, Person person, Vehicle vehicle) {
        return link.getLength() / mutator.mutateLinkSpeed(link.getFreespeed(), time);
    }
}
