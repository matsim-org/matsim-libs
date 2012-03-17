package org.matsim.contrib.matsim4opus.matsim4urbansim.costcalculators;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.router.util.TravelDisutility;

/**
 * cost calculator for travel distances
 * @author thomas
 *
 */
public class TravelDistanceCostCalculator implements TravelDisutility{
	private static final Logger log = Logger.getLogger(TravelDistanceCostCalculator.class);
	
	@Override
	public double getLinkTravelDisutility(final Link link, final double time) {
		if(link != null)
			return link.getLength();
		log.warn("Link is null. Returned 0 as link length.");
		return 0.;
	}
}
