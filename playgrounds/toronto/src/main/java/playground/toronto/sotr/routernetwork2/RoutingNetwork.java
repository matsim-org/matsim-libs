package playground.toronto.sotr.routernetwork2;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.NoSuchElementException;

import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.pt.router.TransitRouter;

import playground.toronto.sotr.routernetwork2.RoutingWalkLink.WalkType;

/**
 * Represents a graph for routing transit trips, with limited functionality. The network itself
 * is not able to be used for routing, but instead creates {@link RoutingNetworkDelegate} copies
 * of itself (for thread-safety). The delegates can then be used for routing.
 * 
 * @author pkucirek
 *
 */
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
		
		double minX = Double.POSITIVE_INFINITY, minY = Double.POSITIVE_INFINITY;
		double maxX = Double.NEGATIVE_INFINITY, maxY = Double.NEGATIVE_INFINITY;
		
		for (RoutingNode n : nodes){
			Coord c = n.coord;
			
			if (c.getX() < minX) minX = c.getX();
			if (c.getY() < minY) minY = c.getY();
			if (c.getX() > maxX) maxX = c.getX();
			if (c.getY() > maxY) maxY = c.getY();
		}
		
		RoutingNetworkDelegate delegate = new RoutingNetworkDelegate(minX, minY, maxX, maxY, links.size());
		
		HashMap<RoutingNode, RoutingNode> nodeProxyMap = new HashMap<RoutingNode, RoutingNode>();
		HashMap<RoutingLink, RoutingLink> linkProxyMap = new HashMap<RoutingLink, RoutingLink>();
		
		//Copy the nodes
		for (RoutingNode node : nodes){
			RoutingNode copy = new RoutingNode(node.coord);
			
			delegate.quadTree.put(node.coord.getX(), node.coord.getY(), copy);
			nodeProxyMap.put(node, copy);
		}
		
		//Copy the links
		int currentIndex = 1;
		for (RoutingLink link : links){
			RoutingNode fromNode = nodeProxyMap.get(link.fromNode);
			RoutingNode toNode = nodeProxyMap.get(link.toNode);
			
			RoutingLink copy= null;		
			
			if (link instanceof RoutingWalkLink){
				copy = new RoutingWalkLink(fromNode, toNode, ((RoutingWalkLink) link).getLength(), WalkType.TRANSFER);
			} else if (link instanceof RoutingInVehicleLink){
				RoutingInVehicleLink wrapped = (RoutingInVehicleLink) link;
				copy = new RoutingInVehicleLink(fromNode, toNode, wrapped.data);
			}
			
			fromNode.outgoingLinks.add(copy);
			copy.index = currentIndex++;
			linkProxyMap.put(link, copy);
		}
		
		//Copy turn restrictions
		for (Entry<RoutingLink, RoutingLink> entry : linkProxyMap.entrySet()){
			RoutingLink link = entry.getKey();
			RoutingLink copy = entry.getValue();
			
			for (RoutingLink restrictedTurn : link.prohibitedOutgoingTurns){
				RoutingLink turnCopy = linkProxyMap.get(restrictedTurn);
				copy.prohibitedOutgoingTurns.add(turnCopy);
			}
		}
		
		return delegate;
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
			
			int nextIndex = links.length; //For the HasIndex interface
			
			ORIGIN_NODE = new RoutingNode(ORIGIN);
			accessLinks = new RoutingLink[accessNodes.size()];
			int i = 0;
			for(RoutingNode accessNode : accessNodes){
				RoutingWalkLink link = new RoutingWalkLink(ORIGIN_NODE, accessNode, WalkType.ACCESS);
				ORIGIN_NODE.outgoingLinks.add(link);
				link.index = nextIndex++;
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
				link.index = nextIndex++;
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
