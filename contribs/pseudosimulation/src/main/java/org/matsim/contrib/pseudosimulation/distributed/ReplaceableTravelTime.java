package org.matsim.contrib.pseudosimulation.distributed;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.router.util.TravelTime;
import org.matsim.vehicles.Vehicle;

/**
 * Created by fouriep on 16/7/16.
 */
class ReplaceableTravelTime implements TravelTime {
    private TravelTime delegate;

    @Override
    public double getLinkTravelTime(Link link, double time, Person person, Vehicle vehicle) {
        return this.delegate.getLinkTravelTime(link, time, person, vehicle);
    }

    public void setTravelTime(TravelTime linkTravelTimes) {
        this.delegate = linkTravelTimes;
    }
}
