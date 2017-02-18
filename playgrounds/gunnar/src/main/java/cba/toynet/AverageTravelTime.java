package cba.toynet;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.router.util.TravelTime;
import org.matsim.vehicles.Vehicle;

import opdytsintegration.utils.TimeDiscretization;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
class AverageTravelTime implements TravelTime {

	// -------------------- CONSTANTS --------------------

	private final TimeDiscretization timeDiscr;

	private final TravelTime travelTime;

	// -------------------- CONSTRUCTION --------------------

	AverageTravelTime(final TimeDiscretization timeDiscr, final TravelTime travelTime) {
		this.timeDiscr = timeDiscr;
		this.travelTime = travelTime;
	}

	// -------------------- IMPLEMENTATION --------------------
	
	public double getAvgLinkTravelTime(final Link link, final Person person, final Vehicle vehicle) {
		double sum = 0.0;
		for (int bin = 0; bin < this.timeDiscr.getBinCnt(); bin++) {
			sum += this.travelTime.getLinkTravelTime(link, this.timeDiscr.getBinStartTime_s(bin), person, vehicle);
		}
		return (sum / this.timeDiscr.getBinCnt());		
	}

	// -------------------- IMPLEMENTATION OF TravelTime --------------------

	@Override
	public double getLinkTravelTime(final Link link, final double time, final Person person, final Vehicle vehicle) {
		return this.getAvgLinkTravelTime(link, person, vehicle);
	}

}
