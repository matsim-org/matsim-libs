package org.matsim.contrib.bicycle;

import com.google.inject.Inject;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.router.util.TravelTime;
import org.matsim.vehicles.Vehicle;

public class EbikeTravelTime implements TravelTime {
	@Inject
	private EbikeLinkSpeedCalculator linkSpeedCalculator;

    @Inject
    private EbikeTravelTime() {
    }

	@Override
	public double getLinkTravelTime(Link link, double time, Person person, Vehicle vehicle) {

		return link.getLength() / linkSpeedCalculator.getMaximumVelocityForLink(link, vehicle);
}
