package playground.toronto.sotr;

import java.util.ArrayList;
import java.util.List;

import org.jfree.util.Log;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.router.priorityqueue.BinaryMinHeap;

import playground.toronto.sotr.calculators.SOTRDisutilityCalculator2;
import playground.toronto.sotr.calculators.SOTRTimeCalculator2;
import playground.toronto.sotr.routernetwork2.RoutingLink;
import playground.toronto.sotr.routernetwork2.RoutingNode;

/**
 * 
 * @author pkucirek
 *
 */
public class SOTRMultiNodeDijkstra {
	
	private SOTRTimeCalculator2 timeCalc;
	private SOTRDisutilityCalculator2 costCalc;
	private playground.toronto.sotr.routernetwork2.RoutingNetwork.RoutingNetworkDelegate network;
	
	public List<RoutingLink> calculateLeastCostPath(final RoutingNode ORIGIN, final RoutingLink DESTINATION, final Person person, final double departureTime) {
		
		//Assumes that the 'virtual' access and egress links have all been created by the TransitRouter object.
		//Due to the fact that link pending costs & times are to the tail (not head) of links, the destination is
		//actually a link.
		
		BinaryMinHeap<RoutingLink> pendingLinks = new BinaryMinHeap<RoutingLink>(network.getNumberOfLinks());
		
		for (RoutingLink link : ORIGIN.getOutgoingLinks()){
			link.pendingCost = 0;
			link.pendingTime = departureTime;
		}
		for (RoutingLink link : network.iterLinks()){
			pendingLinks.add(link, link.pendingCost);
		}
		
		while (pendingLinks.size() > 0){
			RoutingLink currentLink = pendingLinks.poll();
			
			if (currentLink == DESTINATION){
				return this.constructPath(DESTINATION);
			}
			
			//Absolute time from the origin node to the tail of the current link
			double now = currentLink.pendingTime;
			
			//Relative time & cost to traverse the link
			double linkTime = timeCalc.getLinkTravelTime(currentLink, now, person, null);
			double linkCost = costCalc.getLinkTravelDisutility(currentLink, now, person, null);
			
			//Quick check, just in case. Were this C#, I'd put a compiler #IF statement to only compile this if debugging.
			if (linkTime < 0) { Log.error("Fatal Error: travel time was negative", new Exception());}
			if (linkCost < 0) { Log.error("Fatal Error: travel cost was negative", new Exception());}

			for (RoutingLink nextLink : currentLink.getOutgoingTurns(false)){
				//Relative time & cost to after the turn
				double turnTime = timeCalc.getTurnTravelTime(currentLink, nextLink, now + linkTime, person, null);
				double turnCost = costCalc.getTurnTravelDisutility(currentLink, nextLink, now + linkTime, person, null);
				
				if (turnTime < 0) { Log.error("Fatal Error: travel time was negative", new Exception());}
				if (turnCost < 0) { Log.error("Fatal Error: travel cost was negative", new Exception());}
				
				if ((turnCost + linkCost + currentLink.pendingCost) < nextLink.pendingCost){
					nextLink.pendingCost = turnCost + linkCost + currentLink.pendingCost;
					nextLink.pendingTime = turnTime + linkTime + now;
					nextLink.previousLink = currentLink;
					
					pendingLinks.decreaseKey(nextLink, nextLink.pendingCost); //Update nextLink's position in the queue
				}
			}
		}
		
		Log.warn("Could not find a route!");
		return null;
	}
	
	private List<RoutingLink> constructPath(RoutingLink DESTINATION){
		ArrayList<RoutingLink> path = new ArrayList<RoutingLink>();
		
		//Work backwards from the destination link
		RoutingLink previousLink = DESTINATION.previousLink;
		while (previousLink != null){
			path.add(0, previousLink);
			previousLink = previousLink.previousLink; //That's a fun line of code. Totally legal, too ;-)
		}
		
		return path;
	}
	
}
