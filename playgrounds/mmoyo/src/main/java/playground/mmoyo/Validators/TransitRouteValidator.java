package playground.mmoyo.Validators;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.TravelCost;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.transitSchedule.api.TransitLine;
import org.matsim.transitSchedule.api.TransitRoute;
import org.matsim.transitSchedule.api.TransitRouteStop;
import org.matsim.transitSchedule.api.TransitSchedule;

import playground.mmoyo.PTRouter.LogicFactory;
import playground.mmoyo.PTRouter.MyDijkstra;

/**
 * Identifies isolated TransitRoutes
 */
public class TransitRouteValidator {
	private NetworkLayer logicNetwork;
	private TransitSchedule transitSchedule;
	private static final Logger log = Logger.getLogger(TransitRouteValidator.class);
	
	public TransitRouteValidator(TransitSchedule transitSchedule){
		this.transitSchedule = transitSchedule;
	}

	public void getIsolatedPTLines(){

		int isolated=0;
		int comparisons=0;
		PseudoTimeCost pseudoTimeCost = new PseudoTimeCost();
		this.logicNetwork =	new LogicFactory(this.transitSchedule).getLogicNet();
		LeastCostPathCalculator expressDijkstra = new MyDijkstra(logicNetwork, pseudoTimeCost, pseudoTimeCost);

		List<Id[]> ptLineIdList = new ArrayList<Id[]>();
		for (TransitLine transitLine : transitSchedule.getTransitLines().values()){
			for (TransitRoute transitRoute : transitLine.getRoutes().values()){
				for (TransitRoute transitRoute2 : transitLine.getRoutes().values()){
					if(!transitRoute.equals(transitRoute2)){
						Node node1 = this.logicNetwork.getLinks().get(transitRoute.getRoute().getStartLinkId()).getToNode();
						Node node2 = this.logicNetwork.getLinks().get(transitRoute2.getRoute().getEndLinkId()).getFromNode();
						Path path = expressDijkstra.calcLeastCostPath(node1, node2, 600);
						comparisons++;
						if (path==null){
							//Id[2] intArray = [ptLine.getId(),ptLine2.getId()];
							Id[] idArray = new Id[2];
							idArray[0] = transitRoute.getId();
							idArray[1] = transitRoute2.getId();
							ptLineIdList.add(idArray);
							isolated++;
						}
					}
				}
			}
		}

		for(Id[] idarray: ptLineIdList){
			System.out.println("\n" + idarray[0] + "\n" + idarray[1] );
		}
		System.out.println(	"Total comparisons: " + comparisons + "\nisolated: " + isolated);
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

		public double getLinkTravelCost(final Link link, final double time) {
			return 1.0;
		}
		
		public double getLinkTravelTime(final Link link, final double time) {
			return 1.0;
		}
	}

	public void findRepetedStops(){
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

	public static void main(String[] args) {
		String config = null;

		if (args.length==1){
			config = args[0];
		}else{
			config= "../playgrounds/mmoyo/output/trRoutVis/config.xml";
		}
		ScenarioImpl scenarioImpl = new playground.mmoyo.utils.TransScenarioLoader().loadScenario(config);
		new TransitRouteValidator(scenarioImpl.getTransitSchedule()).findRepetedStops();
	}
	
}