package besttimeresponseintegration;

import java.util.List;

import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.router.TripRouter;
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

	private final TimeDiscretization timeDiscr;

	private final TripRouter tripRouter;

	private final Person person;

	private TravelTimeCache<Facility, String> cache = null;

	// -------------------- CONSTRUCTION --------------------

	public BestTimeResponseTravelTimes(final TimeDiscretization timeDiscr, final TripRouter tripRouter,
			final Person person) {
		this.timeDiscr = timeDiscr;
		this.tripRouter = tripRouter;
		this.person = person;
	}

	public void setCache(final TravelTimeCache<Facility, String> cache) {
		this.cache = cache;
	}

	// --------------- IMPLEMENTATION OF TripTravelTimes ---------------

	@Override
	public double getTravelTime_s(final Facility origin, final Facility destination, final double dptTime_s,
			final String mode) {

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

			final Leg lastLeg = (Leg) tripSequence.get(tripSequence.size() - 1);
			tt_s = (lastLeg.getDepartureTime() + lastLeg.getTravelTime()) - tripDptTime_s;
			if (this.cache != null) {
				this.cache.putTT_s(bin, origin, destination, mode, tt_s);
			}
		}
		return tt_s;
	}
}
