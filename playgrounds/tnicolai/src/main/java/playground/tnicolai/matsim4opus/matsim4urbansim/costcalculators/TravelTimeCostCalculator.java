package playground.tnicolai.matsim4opus.matsim4urbansim.costcalculators;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelMinDisutility;
import org.matsim.core.router.util.TravelTime;

public class TravelTimeCostCalculator implements TravelMinDisutility, TravelDisutility {

	private static final Logger log = Logger.getLogger(TravelTimeCostCalculator.class);
	
	protected final TravelTime timeCalculator;
	
	/**
	 * constructor
	 * 
	 * @param timeCalculator
	 * @param cnScoringGroup
	 */
	public TravelTimeCostCalculator(final TravelTime timeCalculator){
		this.timeCalculator = timeCalculator;
	}
	
	@Override
	public double getLinkTravelDisutility(Link link, double time) {
		if(link != null){
			double travelTime = this.timeCalculator.getLinkTravelTime(link, time);
			return travelTime;
		}
		log.warn("Link is null. Returned 0 as car time.");
		return 0.;
	}
	
	@Override
	public double getLinkMinimumTravelDisutility(final Link link) {
		if(link != null)
			return (link.getLength() / link.getFreespeed());
		log.warn("Link is null. Returned 0 as walk time.");
		return 0.;
	}
}
