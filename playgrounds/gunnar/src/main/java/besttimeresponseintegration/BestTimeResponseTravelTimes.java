package besttimeresponseintegration;

import java.util.Map;

import org.matsim.core.router.Dijkstra;
import org.matsim.core.router.util.TravelTime;

import besttimeresponse.TravelTimes;
import opdytsintegration.utils.TimeDiscretization;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
class BestTimeResponseTravelTimes implements TravelTimes {

	private final TimeDiscretization timeDiscr;
	
	private final Map<String, TravelTime> mode2tt;

	BestTimeResponseTravelTimes(final TimeDiscretization timeDiscr, Map<String, TravelTime> mode2tt) {
		this.timeDiscr = timeDiscr;
		this.mode2tt = mode2tt;
	}

	
	
	@Override
	public double getTravelTime_s(Object origin, Object destination, double dptTime_s, Object mode) {
		final TravelTime tt = this.mode2tt.get(mode);
	
		
		return 0;
	}

}
