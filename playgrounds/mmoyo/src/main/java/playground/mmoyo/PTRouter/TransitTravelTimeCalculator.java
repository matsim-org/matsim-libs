package playground.mmoyo.PTRouter;

import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkLayer;
import org.matsim.transitSchedule.api.Departure;
import org.matsim.transitSchedule.api.TransitLine;
import org.matsim.transitSchedule.api.TransitRoute;
import org.matsim.transitSchedule.api.TransitRouteStop;
import org.matsim.transitSchedule.api.TransitSchedule;
import org.matsim.transitSchedule.api.TransitStopFacility;

/**
 * Calculates and stores travel time according to the logicTransitSchedule object
 **/
public class TransitTravelTimeCalculator{
	private Map<Id,Double> linkTravelTimeMap = new TreeMap<Id,Double>();
	public Map<Id,double[]> nodeDeparturesMap = new TreeMap<Id,double[]>();

	public TransitTravelTimeCalculator(final TransitSchedule logicTransitSchedule, final NetworkLayer logicNetwork){
		calculateTravelTimes(logicTransitSchedule,logicNetwork);
	}

	/**fills  a map of travelTime for links and  a map of departures for each node to create a TransitTimeTable*/
	public void calculateTravelTimes(TransitSchedule logicTransitSchedule, NetworkLayer logicNetwork){
		for (TransitLine transitLine : logicTransitSchedule.getTransitLines().values()){
			for (TransitRoute transitRoute : transitLine.getRoutes().values()){
				Node lastNode = null;
				boolean first= true;
				double departureDelay=0;
				double lastDepartureDelay =0;
				double linkTravelTime=0;

				/**Creates an array of Transit Route departures*/
				int numDepartures= transitRoute.getDepartures().size();
				double[] departuresArray =new double[numDepartures];
				int i=0;
				for (Departure departure : transitRoute.getDepartures().values()){
					departuresArray[i]=departure.getDepartureTime();
					i++;
				}

				/**iterates in each stop to calculate departures travel times*/
				for (TransitRouteStop transitRouteStop: transitRoute.getStops()) {
					TransitStopFacility transitStopFacility = transitRouteStop.getStopFacility();
					Node node = logicNetwork.getNodes().get(transitStopFacility.getId());

					/**Save node departures in the DeparturesMap*/
					double[] nodeDeparturesArray =new double[numDepartures];
					for (int j=0; j<numDepartures; j++){
						double departureTime =departuresArray[j] + transitRouteStop.getDepartureOffset();
						if (departureTime > 86400) departureTime=departureTime-86400;
						nodeDeparturesArray[j] = departureTime;
					}
					Arrays.sort(nodeDeparturesArray);
					nodeDeparturesMap.put(transitStopFacility.getId(), nodeDeparturesArray);

					/**finds the link that joins both stations, calculates and saves its travel time*/
					if (!first){
						for (Link lastLink : node.getInLinks().values()){
							if (lastLink.getFromNode().equals(lastNode)){
								departureDelay= transitRouteStop.getDepartureOffset();
								linkTravelTime= (departureDelay- lastDepartureDelay)/60; //stored in minutes
								linkTravelTimeMap.put(lastLink.getId(), linkTravelTime);   //this must be eliminated
							}
						}
					}else{
						first=false;
					}

					lastNode= node;
					lastDepartureDelay = departureDelay;
				}
			}
		}
	}

	/*
	public void fillTimeTable(PTTimeTable ptTimeTable2){
		ptTimeTable2.setLinkTravelTimeMap(linkTravelTimeMap);
		ptTimeTable2.setNodeDeparturesMap(nodeDeparturesMap);
		//linkTravelTimeMap = null;
		//nodeDeparturesMap = null;
	}
	*/


}
