package besttimeresponseintegration;

import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.util.TravelTime;
import org.matsim.facilities.Facility;

import besttimeresponse.TripTravelTimes;
import opdytsintegration.utils.TimeDiscretization;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public class BestTimeResponseTravelTimes implements TripTravelTimes<Facility, String> {

	// -------------------- MEMBERS --------------------

	private final Network network;

	private final Map<String, TravelTime> mode2travelTime;

	private final TimeDiscretization timeDiscr;

	private final TripRouter tripRouter;

	private final Person person;

	private TravelTimeCache<Facility, String> cache = null;

	// -------------------- CONSTRUCTION --------------------

	public BestTimeResponseTravelTimes(final Network network, final TimeDiscretization timeDiscr,
			final TripRouter tripRouter, final Person person, final Map<String, TravelTime> mode2travelTime) {
		this.network = network;
		this.timeDiscr = timeDiscr;
		this.tripRouter = tripRouter;
		this.person = person;
		this.mode2travelTime = mode2travelTime;
	}

	public void setCache(final TravelTimeCache<Facility, String> cache) {
		this.cache = cache;
	}

	// --------------- IMPLEMENTATION OF TripTravelTimes ---------------

	@Override
	synchronized public double getTravelTime_s(final Facility origin, final Facility destination,
			final double dptTime_s, final String mode) {

		final int leftBin;
		{
			final int bin = this.timeDiscr.getBin(dptTime_s);
			if (dptTime_s < this.timeDiscr.getBinCenterTime_s(bin)) {
				// interpolate to the left (temporally downwards)
				leftBin = bin - 1;
			} else {
				// interpolate to the right
				leftBin = bin;
			}
		}
		final double leftTT_s = this.computeOrGetTravelTime_s(leftBin, origin, destination, mode);

		final int rightBin = leftBin + 1;
		final double rightTT_s = this.computeOrGetTravelTime_s(rightBin, origin, destination, mode);

		final double weight = (dptTime_s - this.timeDiscr.getBinCenterTime_s(leftBin)) / this.timeDiscr.getBinSize_s();
		return (1.0 - weight) * leftTT_s + weight * rightTT_s;
	}

	private double computeOrGetTravelTime_s(final int bin, final Facility origin, final Facility destination,
			final String mode) {
		Double tt_s = null;
		if (this.cache != null) {
			tt_s = this.cache.getTT_s(bin, origin, destination, mode);
		}
		if (tt_s == null) {

			final double tripDptTime_s = Math.max(this.timeDiscr.getBinStartTime_s(bin), 0);
			final List<? extends PlanElement> tripSequence = this.tripRouter.calcRoute(mode, origin, destination,
					tripDptTime_s, this.person);

			double startTime_s = tripDptTime_s;
			double time_s = tripDptTime_s;
			for (int i = 0; i < tripSequence.size(); i++) {
				if (tripSequence.get(i) instanceof Leg) {

					time_s += legTravelTime_s((Leg) tripSequence.get(i), this.network, this.mode2travelTime,
							this.person);

					// final Leg leg = (Leg) tripSequence.get(i);

				}
				// else if (tripSequence.get(i) instanceof Activity) {
				// final Activity act = (Activity) tripSequence.get(i);
				//
				// if (PtConstants.TRANSIT_ACTIVITY_TYPE.equals(act.getType())
				// && act.getStartTime()!= Double.NEGATIVE_INFINITY &&
				// act.getEndTime() != Double.NEGATIVE_INFINITY) {
				// time_s += act.getEndTime() - act.getStartTime();
				// }
				// }
			}
			tt_s = time_s - startTime_s;

			if (this.cache != null) {
				this.cache.putTT_s(bin, origin, destination, mode, tt_s);
			}
		}
		return tt_s;
	}

	// TODO clean this up
	private static double legTravelTime_s(final Leg leg, final Network network, Map<String, TravelTime> mode2travelTime,
			final Person person) {

		double time_s = 0.0;

		if (leg.getRoute() instanceof NetworkRoute) {
			final TravelTime travelTime = mode2travelTime.get(leg.getMode());
			final NetworkRoute route = (NetworkRoute) leg.getRoute();
			for (Id<Link> linkId : route.getLinkIds()) {
				final Link link = network.getLinks().get(linkId);
				final double linkTT_s = travelTime.getLinkTravelTime(link, time_s, person, null);
				time_s += linkTT_s + 1.0;
			}
			final Link endLink = network.getLinks().get(route.getEndLinkId());
			time_s += travelTime.getLinkTravelTime(endLink, time_s, person, null) + 1.0;
		} else {
			time_s += leg.getTravelTime();
		}
		// catch pt interaction activities -- always maximal
		// duration = 0s ! -> waiting time at pt stops is not
		// considered? should not be summed up?

		return time_s;
	}
}
