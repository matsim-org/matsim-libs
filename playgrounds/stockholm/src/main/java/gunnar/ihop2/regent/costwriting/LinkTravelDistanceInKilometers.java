package gunnar.ihop2.regent.costwriting;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.vehicles.Vehicle;

import cadyts.utilities.misc.Units;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public class LinkTravelDistanceInKilometers implements TravelDisutility {

	public LinkTravelDistanceInKilometers() {
	}

	@Override
	public double getLinkTravelDisutility(Link link, double time,
			Person person, Vehicle vehicle) {
		return Units.KM_PER_M * link.getLength();
	}

	@Override
	public double getLinkMinimumTravelDisutility(Link link) {
		return Units.KM_PER_M * link.getLength();
	}

}
