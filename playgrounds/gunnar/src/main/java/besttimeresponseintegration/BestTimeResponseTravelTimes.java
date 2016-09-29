package besttimeresponseintegration;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.router.Dijkstra;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.vehicles.Vehicle;

import besttimeresponse.TripTravelTimes;
import opdytsintegration.utils.TimeDiscretization;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public class BestTimeResponseTravelTimes implements TripTravelTimes<Link, String>, TravelTime, TravelDisutility {

	// -------------------- MEMBERS --------------------

	private final TimeDiscretization timeDiscr;

	private final TravelTime carTT;

	private final Dijkstra router;

	private final boolean interpolate;

	private TravelTimeCache<Link, String> cache = null;

	// -------------------- CONSTRUCTION --------------------

	public BestTimeResponseTravelTimes(final TimeDiscretization timeDiscr, final TravelTime carTT,
			final Network network, final boolean interpolate) {
		this.timeDiscr = timeDiscr;
		this.carTT = carTT;
		this.router = new Dijkstra(network, this, this);
		this.interpolate = interpolate;
	}

	public void setCaching(final boolean cache) {
		if (cache) {
			if (this.cache == null) {
				this.cache = new TravelTimeCache<Link, String>();
			}
		} else {
			this.cache = null;
		}
	}

	// --------------- IMPLEMENTATION OF TripTravelTimes ---------------

	@Override
	public synchronized double getTravelTime_s(final Link origin, final Link destination, final double dptTime_s,
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
		if (this.interpolate) {
			final int rightBin = leftBin + 1;
			final double weight = (dptTime_s - this.timeDiscr.getBinCenterTime_s(leftBin))
					/ this.timeDiscr.getBinSize_s();
			final double rightTT_s = this.computeOrGetTravelTime_s(rightBin, origin, destination, mode);
			return (1.0 - weight) * leftTT_s + weight * rightTT_s;
		} else {
			return leftTT_s;
		}
	}

	private double computeOrGetTravelTime_s(final int bin, final Link origin, final Link destination,
			final String mode) {
		Double tt_s = null;
		if (this.cache != null) {
			tt_s = this.cache.getTT_s(bin, origin, destination, mode);			
			
//			if (tt_s == null) {
//				System.out.print("X");
//			} else {
//				System.out.print(".");
//			}
			
		}
		if (tt_s == null) {			
			final Path path = this.getCarPath(origin, destination, this.timeDiscr.getBinStartTime_s(bin));
			if ("car".equals(mode)) {
				tt_s = path.travelTime;
			} else if ("pt".equals(mode)) {
				tt_s = 2.0 * path.travelTime;
			} else {
				throw new UnsupportedOperationException("Unsupported mode: " + mode);
			}
			if (this.cache != null) {
				this.cache.putTT_s(bin, origin, destination, mode, tt_s);
				// System.out.println(">>>>> CACHE SIZE = " + this.cache.size());
			}
		}
		return tt_s;
	}

	// >>>>> OLD STUFF BELOW >>>>>

	// public synchronized double getTravelTime_s(final Link origin, final Link
	// destination, final double dptTime_s,
	// final String mode) {
	// final Path path = this.getCarPath(origin, destination, dptTime_s);
	// if ("car".equals(mode)) {
	// return path.travelTime;
	// } else if ("pt".equals(mode)) {
	// return 2.0 * path.travelTime;
	// } else {
	// throw new UnsupportedOperationException("Unsupported mode: " + mode);
	// }
	// }

	public Path getCarPath(final Link origin, final Link destination, final double dptTime_s) {
		return this.router.calcLeastCostPath(origin.getToNode(), destination.getFromNode(), dptTime_s, null, null);
	}

	// --------------- IMPLEMENTATION OF TravelDisutility ---------------

	/*
	 * TODO This is likely to be inconsistent with what the actual re-planning
	 * perceives as travel disutility because it only uses travel time...
	 */

	@Override
	public double getLinkTravelDisutility(final Link link, final double entryTime_s, final Person person,
			final Vehicle vehicle) {
		return this.getLinkTravelTime(link, entryTime_s, person, vehicle);
	}

	@Override
	public double getLinkMinimumTravelDisutility(final Link link) {
		return link.getLength() / link.getFreespeed();
	}

	// -------------------- IMPLEMENTATION OF TravelTime --------------------

	@Override
	public double getLinkTravelTime(final Link link, final double entryTime_s, final Person person,
			final Vehicle vehicle) {

		// if (this.interpolate) {
		//
		// final int leftBin;
		// final int rightBin;
		// {
		// final int bin = this.timeDiscr.getBin(entryTime_s);
		// if (entryTime_s < this.timeDiscr.getBinCenterTime_s(bin)) {
		// // interpolate to the left (temporally downwards)
		// leftBin = bin - 1;
		// rightBin = bin;
		// } else {
		// // interpolate to the right
		// leftBin = bin;
		// rightBin = bin + 1;
		// }
		// }
		// final double weight = (entryTime_s -
		// this.timeDiscr.getBinCenterTime_s(leftBin))
		// / this.timeDiscr.getBinSize_s();
		// return weight * this.carTT.getLinkTravelTime(link,
		// this.timeDiscr.getBinCenterTime_s(rightBin), null, null)
		// + (1.0 - weight) * this.carTT.getLinkTravelTime(link,
		// this.timeDiscr.getBinCenterTime_s(leftBin),
		// null, null);
		// } else {

		return this.carTT.getLinkTravelTime(link, Math.max(entryTime_s, 0), null, null);

		// }
	}
}
