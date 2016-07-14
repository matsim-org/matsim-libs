package gunnar.ihop2.regent.costwriting;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.utils.misc.Time;
import org.matsim.vehicles.Vehicle;

import floetteroed.utilities.Units;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public class LinkTravelTimeInMinutes implements TravelDisutility {

	private final TravelTime travelTimeInSeconds;

	public LinkTravelTimeInMinutes(final TravelTime travelTimeInSeconds) {
		this.travelTimeInSeconds = travelTimeInSeconds;
	}

	@Override
	public double getLinkTravelDisutility(final Link link, final double time,
			final Person person, final Vehicle vehicle) {
		return Units.MIN_PER_S
				* this.travelTimeInSeconds.getLinkTravelTime(link, time,
						person, vehicle);
	}

	@Override
	public double getLinkMinimumTravelDisutility(Link link) {
		return Units.MIN_PER_S
				* this.travelTimeInSeconds.getLinkTravelTime(link,
						Time.UNDEFINED_TIME, null, null);
	}
}
