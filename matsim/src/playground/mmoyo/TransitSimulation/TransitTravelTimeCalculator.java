package playground.mmoyo.TransitSimulation;

import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;

import org.matsim.api.basic.v01.Id;
import org.matsim.transitSchedule.TransitStopFacility;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.network.NodeImpl;


import playground.marcel.pt.transitSchedule.api.Departure;
import playground.marcel.pt.transitSchedule.api.TransitLine;
import playground.marcel.pt.transitSchedule.api.TransitRoute;
import playground.marcel.pt.transitSchedule.api.TransitRouteStop;
import playground.marcel.pt.transitSchedule.api.TransitSchedule;
import playground.mmoyo.PTRouter.PTTimeTable2;

/**
 * Calculates and stores travel time according to the logicTransitSchedule object
 **/
public class TransitTravelTimeCalculator {
	private Map<Id,Double> linkTravelTimeMap = new TreeMap<Id,Double>();
	public Map<Id,double[]> nodeDeparturesMap = new TreeMap<Id,double[]>();
	
	public TransitTravelTimeCalculator(final TransitSchedule logicTransitSchedule, final NetworkLayer logicNetwork){
		calculateTravelTimes(logicTransitSchedule,logicNetwork);
	}
	
	/**fills  a map of travelTime for links and  a map of departures for each node to create a TransitTimeTable*/
	public void calculateTravelTimes(TransitSchedule logicTransitSchedule, NetworkLayer logicNetwork){
		for (TransitLine transitLine : logicTransitSchedule.getTransitLines().values()){
			for (TransitRoute transitRoute : transitLine.getRoutes().values()){
				NodeImpl lastNode = null;
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
					NodeImpl node = logicNetwork.getNode(transitStopFacility.getId());
					
					/**Save node departures in the DeparturesMap*/
					double[] nodeDeparturesArray =new double[numDepartures];
					for (int j=0; j<numDepartures; j++){
						double departureTime =departuresArray[j] + transitRouteStop.getDepartureDelay();
						if (departureTime > 86400) departureTime=departureTime-86400;
						nodeDeparturesArray[j] = departureTime; 
					} 
					Arrays.sort(nodeDeparturesArray);
					nodeDeparturesMap.put(transitStopFacility.getId(), nodeDeparturesArray);
					
					
					/**finds the link that joins both stations, calculate and save its travel time*/ 
					if (!first){
						for (LinkImpl lastLink : node.getInLinks().values()){
							if (lastLink.getFromNode().equals(lastNode)){
								departureDelay= transitRouteStop.getDepartureDelay();
								linkTravelTime= departureDelay- lastDepartureDelay;
								linkTravelTime = linkTravelTime/60;
								linkTravelTimeMap.put(lastLink.getId(), linkTravelTime);
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

	
	public void fillTimeTable(PTTimeTable2 ptTimeTable2){
		ptTimeTable2.setLinkTravelTimeMap(linkTravelTimeMap);
		ptTimeTable2.setNodeDeparturesMap(nodeDeparturesMap);
		//linkTravelTimeMap = null;
		//nodeDeparturesMap = null;
	}
	
	
}
