package playground.toronto.sotr.routernetwork2;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.pt.router.TransitRouter;
import org.matsim.pt.transitSchedule.api.TransitRoute;

import playground.toronto.sotr.routernetwork2.RoutingWalkLink.WalkType;

public class RoutingNetwork {
	
	private ArrayList<RoutingNode> nodes;
	private ArrayList<RoutingLink> links;
	
	public RoutingNode createNode(Coord coord){
		RoutingNode node = new RoutingNode(coord);
		this.nodes.add(node);
		return node;
	}
	
	/**
	 * Creates and adds a new {@link RoutingWalkLink} to the network, of type
	 * <code>TRANSFER</code>. Other walk types are created during the routing procedure.
	 * @param fromNode
	 * @param toNode
	 * @return
	 */
	public RoutingWalkLink createWalkLink(RoutingNode fromNode, RoutingNode toNode){
		RoutingWalkLink link = new RoutingWalkLink(fromNode, toNode, WalkType.TRANSFER);
		this.links.add(link);
		fromNode.outgoingLinks.add(link);
		return link;
	}
	
	/**
	 * Creates and adds a new {@link RoutingInVehicleLink} to the network.
	 * @param fromNode
	 * @param toNode
	 * @param data
	 * @return
	 */
	public RoutingInVehicleLink createInVehicleLink(RoutingNode fromNode, RoutingNode toNode, InVehicleLinkData data){
		RoutingInVehicleLink link = new RoutingInVehicleLink(fromNode, toNode, data);
		this.links.add(link);
		fromNode.outgoingLinks.add(link);
		return link;
	}
	
	public void createTurnRestriction(RoutingLink fromLink, RoutingLink toLink){
		if (fromLink.toNode != toLink.fromNode)
			throw new IllegalArgumentException("Cannot create turn restriction: links must be connected.");
		
		fromLink.prohibitedOutgoingTurns.add(toLink);
	}
	
	public RoutingNetworkDelegate createDelegate(){
		//TODO
		return null;
	}
	
	/**
	 * A deep copy of the {@link RoutingNetwork} which lives on one thread, one instance of the {@link TransitRouter}.
	 * I've tried to keep it as lean as possible on memory.<br><br>
	 * To use for routing, call <code>prepareForRouting</code>. Then, iterate through links
	 * by calling <code>iterLinks</code>. The origin node and destination link can be accessed 
	 * through <code>getOriginNode</code> and <code>getDestinationLink</code>, respectively.
	 * @author pkucirek
	 *
	 */
	public static class RoutingNetworkDelegate{
		
		private final QuadTree<RoutingNode> quadTree;
		private final RoutingLink[] links;
		
		private RoutingNode ORIGIN_NODE = null;
		private RoutingLink[] accessLinks = null;
		private RoutingLink DESTINATION_LINK = null;
		private RoutingLink[] egressLinks = null;
		
		private RoutingNetworkDelegate(double minX, double minY, double maxX, double maxY, int numberOfLinks){
			quadTree = new QuadTree<RoutingNode>(minX, minY, maxX, maxY);
			this.links = new RoutingLink[numberOfLinks];
		}
		
		public Collection<RoutingNode> getNearestNodes(Coord coord, double radius){
			return this.quadTree.get(coord.getX(), coord.getY(), radius);
		}
		
		/**
		 * Iterates through the links in the network, including the access and egress links.
		 * @return
		 */
		public Iterable<RoutingLink> iterLinks(){
			if (accessLinks == null || egressLinks == null)
				throw new NullPointerException("Tried to get links on an unprepared network. Use prepareForRouting first.");
			
			return new Iterable<RoutingLink>() {
				
				@Override
				public Iterator<RoutingLink> iterator() {
					return new Iterator<RoutingLink>() {
						
						int currentIndex = 0;
						
						@Override
						public void remove() { throw new UnsupportedOperationException(); }
						
						@Override
						public RoutingLink next() {							
							if (currentIndex < accessLinks.length)
								return accessLinks[currentIndex++];
							else if (currentIndex < (accessLinks.length + links.length))
								return links[currentIndex++ - accessLinks.length];
							else if (currentIndex < (accessLinks.length + links.length + egressLinks.length))
								return egressLinks[currentIndex++ - accessLinks.length - egressLinks.length];
							else throw new NoSuchElementException();
						}
						
						@Override
						public boolean hasNext() {
							return currentIndex < (accessLinks.length + links.length + egressLinks.length);
						}
					};
				}
			};
		}
		
		/**
		 * Prepares the network for a routing request. 
		 * 
		 * @param ORIGIN Origin coordinate.
		 * @param accessNodes Collection of access nodes.
		 * @param DESTINATIION Destination coordinate.
		 * @param egressNodes Collection of egress nodes.
		 */
		public void prepareForRouting(Coord ORIGIN, Collection<RoutingNode> accessNodes,
				Coord DESTINATIION, Collection<RoutingNode> egressNodes){
			
			clearEgressLinks(); //Dereference previous request's egress links
			reset(); //Reset the network links' pending costs & times.
			
			ORIGIN_NODE = new RoutingNode(ORIGIN);
			accessLinks = new RoutingLink[accessNodes.size()];
			int i = 0;
			for(RoutingNode accessNode : accessNodes){
				RoutingWalkLink link = new RoutingWalkLink(ORIGIN_NODE, accessNode, WalkType.ACCESS);
				ORIGIN_NODE.outgoingLinks.add(link);
				accessLinks[i++] = link;
			}
			
			RoutingNode destination1 = new RoutingNode(DESTINATIION);
			RoutingNode destination2 = new RoutingNode(DESTINATIION);
			DESTINATION_LINK = new DestinationLink(destination1, destination2);
			egressLinks = new RoutingLink[egressNodes.size()];
			i = 0;
			for (RoutingNode egressNode : egressNodes){
				RoutingWalkLink link = new RoutingWalkLink(egressNode, destination1, WalkType.EGRESS);
				egressNode.outgoingLinks.add(link);
				link.toNode.outgoingLinks.add(DESTINATION_LINK);
				egressLinks[i++] = link;
			}
		}
		
		private void clearEgressLinks(){
			//This method removes references to the egress links from the network. The ones from the previous
			//routing request should get cleaned by the GC next time it checks since they're not longer being used.
			if (egressLinks == null) return;
			
			for (RoutingLink link : egressLinks){
				link.fromNode.outgoingLinks.remove(link);
			}
		}
		
		private void reset(){
			for(RoutingLink link : links){
				link.reset();
			}
		}
		
		public RoutingNode getOriginNode(){
			if (this.ORIGIN_NODE == null) throw new NullPointerException("Use prepareForRouting first.");
			return this.ORIGIN_NODE;
		}
		
		public RoutingLink getDestinationLink(){
			if (this.ORIGIN_NODE == null) throw new NullPointerException("Use prepareForRouting first.");
			return this.DESTINATION_LINK;
		}
		
		public int getNumberOfLinks(){
			return accessLinks.length + links.length + egressLinks.length;
		}
	}
	
	private static class DestinationLink extends RoutingLink{

		public DestinationLink(RoutingNode fromNode, RoutingNode toNode) {
			super(fromNode, toNode);
		}
		
	}
	
}
