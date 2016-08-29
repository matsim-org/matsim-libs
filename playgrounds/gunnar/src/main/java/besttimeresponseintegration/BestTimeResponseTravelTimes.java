package besttimeresponseintegration;

import java.util.Map;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
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
class BestTimeResponseTravelTimes implements TripTravelTimes, TravelTime, TravelDisutility {

	// -------------------- MEMBERS --------------------

	private final TimeDiscretization timeDiscr;

	private final TravelTime carTT;

	private final Dijkstra router;

	// -------------------- CONSTRUCTION --------------------

	BestTimeResponseTravelTimes(final TimeDiscretization timeDiscr, final Map<String, TravelTime> mode2tt,
			final Network network) {
		this.timeDiscr = timeDiscr;
		this.carTT = mode2tt.get("car");
		this.router = new Dijkstra(network, this, this);
	}

	// --------------- IMPLEMENTATION OF TripTravelTimes ---------------

	@Override
	public synchronized double getTravelTime_s(final Object origin, final Object destination, final double dptTime_s,
			final Object mode) {
		final Path path = this.getCarPath(origin, destination, dptTime_s);
		if ("car".equals(mode)) {
			return path.travelTime;
		} else if ("pt".equals(mode)) {
			return 2.0 * path.travelTime;
		} else {
			throw new UnsupportedOperationException("Unsupported mode: " + mode);
		}
	}

	Path getCarPath(final Object origin, final Object destination, final double dptTime_s) {
		return this.router.calcLeastCostPath((Node) origin, (Node) destination, dptTime_s, null, null);
	}

	// -------------------- IMPLEMENTATION OF TravelTime --------------------

	@Override
	public double getLinkTravelTime(final Link link, final double entryTime_s, final Person person,
			final Vehicle vehicle) {
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
				+ (1.0 - weight)
						* this.carTT.getLinkTravelTime(link, this.timeDiscr.getBinCenterTime_s(leftBin), null, null);
	}

	// --------------- IMPLEMENTATION OF TravelDisutility ---------------

	@Override
	public double getLinkTravelDisutility(final Link link, final double entryTime_s, final Person person,
			final Vehicle vehicle) {
		return this.getLinkTravelTime(link, entryTime_s, person, vehicle);
	}

	@Override
	public double getLinkMinimumTravelDisutility(final Link link) {
		return link.getLength() / link.getFreespeed();
	}
}
