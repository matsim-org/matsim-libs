package playground.mmoyo.Validators;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.router.util.TravelCost;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

import playground.mmoyo.utils.DataLoader;

/**
 * Identifies isolated TransitRoutes
 */
public class RepeatedStopFinder {
	private TransitSchedule transitSchedule;
	private static final Logger log = Logger.getLogger(RepeatedStopFinder.class);
	
	public RepeatedStopFinder(TransitSchedule transitSchedule){
		this.transitSchedule = transitSchedule;
	}

	public void run(){
		final String ERROR_LOG = " the stop already exist in transit route ";		
		for (TransitLine line : this.transitSchedule.getTransitLines().values()){
			for (TransitRoute route :line.getRoutes().values()){
				List<Id> stopIdList = new ArrayList<Id>();
				log.info(route.getId());
				for (TransitRouteStop stop:  route.getStops()){
					if (stopIdList.contains(stop.getStopFacility().getId())){
						log.error(stop.getStopFacility().getId() + ERROR_LOG +  route.getId() );
					}
					stopIdList.add(stop.getStopFacility().getId());
				}
			}
		}
	}
		
	/**
	 * Returns the minimal distance between two PTLines. This can help the decision of joining them with a Detached Transfer
	 */
	public double getMinimalDistance (final TransitRoute transitRoute1, final TransitRoute transitRoute2){
		double minDistance=0;
		// ->compare distances from first ptline with ever node of secondptline, store the minimal distance
		return minDistance;
	}

	class PseudoTimeCost implements TravelCost, TravelTime {

		public PseudoTimeCost() {
		}

		public double getLinkGeneralizedTravelCost(final Link link, final double time) {
			return 1.0;
		}
		
		public double getLinkTravelTime(final Link link, final double time) {
			return 1.0;
		}
	}

	public static void main(String[] args) {
		String config = null;

		if (args.length==1){
			config = args[0];
		}else{
			config= "../playgrounds/mmoyo/output/trRoutVis/config.xml";
		}
		ScenarioImpl scenarioImpl = new DataLoader().loadScenarioWithTrSchedule(config);
		new RepeatedStopFinder(scenarioImpl.getTransitSchedule()).run();
	}
	
}