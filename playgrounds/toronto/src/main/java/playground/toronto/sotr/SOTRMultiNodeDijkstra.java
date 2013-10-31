package playground.toronto.sotr;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.jfree.util.Log;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.router.priorityqueue.BinaryMinHeap;

import playground.toronto.sotr.calculators.SOTRDisutilityCalculator;
import playground.toronto.sotr.calculators.SOTRTimeCalculator;
import playground.toronto.sotr.routernetwork2.AbstractRoutingLink;
import playground.toronto.sotr.routernetwork2.AbstractRoutingNode;
import playground.toronto.sotr.routernetwork2.RoutingNetwork.RoutingNetworkDelegate;

/**
 * 
 * @author pkucirek
 *
 */
public class SOTRMultiNodeDijkstra {
	
	private final SOTRTimeCalculator timeCalc;
	private final SOTRDisutilityCalculator costCalc;
	private final RoutingNetworkDelegate network;
	
	public SOTRMultiNodeDijkstra(final SOTRTimeCalculator timeFunc, final SOTRDisutilityCalculator costCalc, final RoutingNetworkDelegate network){
		this.timeCalc = timeFunc;
		this.costCalc = costCalc;
		this.network = network;
	}
	
	/**
	 * Calculates the least-cost path for a given person departing at a given time. Assumes that the 
	 * special origin {@link AbstractRoutingNode} and destination {@link AbstractRoutingLink} have been created and
	 * stored in the {@link RoutingNetworkDelegate} (which is true as long as <code>prepareForRouting()
	 * </code> has been called).
	 * 
	 * @param person
	 * @param departureTime
	 * @return
	 */
	public Path calculateLeastCostPath(final Person person, final double departureTime) {
		
		//Assumes that the 'virtual' access and egress links have all been created by the TransitRouter object.
		//Due to the fact that link pending costs & times are to the tail (not head) of links, the destination is
		//actually a link.
		
		AbstractRoutingNode ORIGIN = network.getOriginNode();
		AbstractRoutingLink DESTINATION = network.getDestinationLink();
		
		//Initialize the queue of pending links
		BinaryMinHeap<AbstractRoutingLink> pendingLinks = new BinaryMinHeap<AbstractRoutingLink>(network.getNumberOfLinks() + 1);
		for (AbstractRoutingLink link : ORIGIN.getOutgoingLinks()){
			link.pendingCost = 0;
			link.pendingTime = departureTime;
		}
		for (AbstractRoutingLink link : network.iterLinks()){
			pendingLinks.add(link, link.pendingCost);
		}
		pendingLinks.add(DESTINATION, DESTINATION.pendingCost);
		
		//Main loop
		while (pendingLinks.size() > 0){
			AbstractRoutingLink currentLink = pendingLinks.poll();
			
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

			for (AbstractRoutingLink nextLink : currentLink.getOutgoingTurns(false)){
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
	
	private Path constructPath(AbstractRoutingLink DESTINATION){
		ArrayList<AbstractRoutingLink> links = new ArrayList<AbstractRoutingLink>();
		
		//Work backwards from the destination link
		AbstractRoutingLink previousLink = DESTINATION.previousLink;
		while (previousLink != null){
			links.add(0, previousLink);
			previousLink = previousLink.previousLink;
		}
		
		return new Path(links);
		//return links;
	}
	
	public static class Path{
		private final List<AbstractRoutingLink> links;
		
		public Path(final List<AbstractRoutingLink> links){
			this.links = links;
		}
		
		public AbstractRoutingLink getFirstLink(){
			return this.links.get(0);
		}
		
		public AbstractRoutingLink getLastLink(){
			return this.links.get(this.links.size() - 1);
		}
		
		public Iterable<AbstractRoutingLink> getMiddleLinks(){
			return new Iterable<AbstractRoutingLink>() {
				@Override
				public Iterator<AbstractRoutingLink> iterator() {
					return new Iterator<AbstractRoutingLink>() {
						int index = 1;
						@Override
						public void remove() { throw new UnsupportedOperationException(); }
						@Override
						public AbstractRoutingLink next() { return links.get(index++); }
						@Override
						public boolean hasNext() { return (index < (links.size() - 1));}
					};
				}
			};
		}
		
		public Iterable<AbstractRoutingLink> getLinks(){
			return new Iterable<AbstractRoutingLink>() {
				@Override
				public Iterator<AbstractRoutingLink> iterator() { return links.iterator();}
			};
		}
		
		public int size(){
			return this.links.size();
		}
		
		public AbstractRoutingLink get(int index){
			if ((index < 0) || index >= links.size())
				throw new IndexOutOfBoundsException();
			return this.links.get(index);
		}
	}
	
}
