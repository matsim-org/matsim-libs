package playground.tnicolai.matsim4opus.matsim4urbansim.costcalculators;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.router.util.TravelDisutility;

public class TravelDistanceCalculator implements TravelDisutility{

	private static final Logger log = Logger.getLogger(TravelDistanceCalculator.class);
	
	@Override
	public double getLinkTravelDisutility(final Link link, final double time) {
		if(link != null)
			return link.getLength();	// travel distance in meter
		log.warn("Link is null. Returned 0 as distance.");
		return 0.;
	}
}
