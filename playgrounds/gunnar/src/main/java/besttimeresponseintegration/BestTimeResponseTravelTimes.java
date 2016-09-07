package besttimeresponseintegration;

import java.util.Map;

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
class BestTimeResponseTravelTimes implements TripTravelTimes<Link, String>, TravelTime, TravelDisutility {

	// -------------------- MEMBERS --------------------

	private final TimeDiscretization timeDiscr;

	private final TravelTime carTT;

	private final Dijkstra router;

	private final boolean interpolate;

	// -------------------- CONSTRUCTION --------------------

	BestTimeResponseTravelTimes(final TimeDiscretization timeDiscr, final Map<String, TravelTime> mode2tt,
			final Network network, final boolean interpolate) {
		this.timeDiscr = timeDiscr;
		this.carTT = mode2tt.get("car");
		this.router = new Dijkstra(network, this, this);
		this.interpolate = interpolate;
	}

	// --------------- IMPLEMENTATION OF TripTravelTimes ---------------

	@Override
	public synchronized double getTravelTime_s(final Link origin, final Link destination, final double dptTime_s,
			final String mode) {
		final Path path = this.getCarPath(origin, destination, dptTime_s);
		if ("car".equals(mode)) {
			return path.travelTime;
		} else if ("pt".equals(mode)) {
			return 2.0 * path.travelTime;
		} else {
			throw new UnsupportedOperationException("Unsupported mode: " + mode);
		}
	}

	public Path getCarPath(final Link origin, final Link destination, final double dptTime_s) {
		return this.router.calcLeastCostPath(origin.getToNode(), destination.getFromNode(), dptTime_s, null, null);
	}

	// --------------- IMPLEMENTATION OF TravelDisutility ---------------

	/*
	 * TODO This is likely to be inconsistent with what the actual re-planning
	 * perceives as travel disutility.
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

		if (this.interpolate) {

			final int leftBin;
			final int rightBin;
			{
				final int bin = this.timeDiscr.getBin(entryTime_s);
				if (entryTime_s < this.timeDiscr.getBinCenterTime_s(bin)) {
					// interpolate to the left (temporally downwards)
					leftBin = bin - 1;
					rightBin = bin;
				} else {
					// interpolate to the right
					leftBin = bin;
					rightBin = bin + 1;
				}
			}
			final double weight = (entryTime_s - this.timeDiscr.getBinCenterTime_s(leftBin))
					/ this.timeDiscr.getBinSize_s();
			return weight * this.carTT.getLinkTravelTime(link, this.timeDiscr.getBinCenterTime_s(rightBin), null, null)
					+ (1.0 - weight) * this.carTT.getLinkTravelTime(link, this.timeDiscr.getBinCenterTime_s(leftBin),
							null, null);
		} else {

			return this.carTT.getLinkTravelTime(link, entryTime_s, null, null);

		}
	}
}
