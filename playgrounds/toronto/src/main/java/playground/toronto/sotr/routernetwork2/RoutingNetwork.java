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
	
	private ArrayList<AbstractRoutingNode> nodes;
	private ArrayList<AbstractRoutingLink> links;
	
	public RoutingNetwork(){
		this.nodes = new ArrayList<AbstractRoutingNode>();
		this.links = new ArrayList<AbstractRoutingLink>();
	}
	
	public void addNode(AbstractRoutingNode node){
		this.nodes.add(node);
	}
	
	
	/**
	 * Creates and adds a new {@link RoutingWalkLink} to the network, of type
	 * <code>TRANSFER</code>. Other walk types are created during the routing procedure.
	 * @param fromNode
	 * @param toNode
	 * @return
	 */
	public RoutingWalkLink createWalkLink(AbstractRoutingNode fromNode, AbstractRoutingNode toNode){
		RoutingWalkLink link = new RoutingWalkLink(fromNode, toNode, WalkType.TRANSFER);
		this.links.add(link);
		fromNode.outgoingLinks.add(link);
		return link;
	}
	
	/**
	 * Creates and adds a new {@link RoutingWalkLink} to the network, of type
	 * <code>TRANSFER</code>. Other walk types are created during the routing procedure.
	 * @param fromNode
	 * @param toNode
	 * @param length
	 * @return
	 */
	public RoutingWalkLink createWalkLink(AbstractRoutingNode fromNode, AbstractRoutingNode toNode, double length){
		RoutingWalkLink link = new RoutingWalkLink(fromNode, toNode, length, WalkType.TRANSFER);
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
	public RoutingInVehicleLink createInVehicleLink(AbstractRoutingNode fromNode, AbstractRoutingNode toNode, InVehicleLinkData data){
		RoutingInVehicleLink link = new RoutingInVehicleLink(fromNode, toNode, data);
		this.links.add(link);
		fromNode.outgoingLinks.add(link);
		return link;
	}
	
	public void createTurnRestriction(AbstractRoutingLink fromLink, AbstractRoutingLink toLink){
		if (fromLink.toNode != toLink.fromNode)
			throw new IllegalArgumentException("Cannot create turn restriction: links must be connected.");
		
		fromLink.prohibitedOutgoingTurns.add(toLink);
	}
	
	/**
	 * Creates a deep copy {@link RoutingNetworkDelegate} of this network.
	 * @return
	 */
	public RoutingNetworkDelegate createDelegate(){
		
		double minX = Double.POSITIVE_INFINITY, minY = Double.POSITIVE_INFINITY;
		double maxX = Double.NEGATIVE_INFINITY, maxY = Double.NEGATIVE_INFINITY;
		
		for (AbstractRoutingNode n : nodes){
			Coord c = n.getCoord();
			
			if (c.getX() < minX) minX = c.getX();
			if (c.getY() < minY) minY = c.getY();
			if (c.getX() > maxX) maxX = c.getX();
			if (c.getY() > maxY) maxY = c.getY();
		}
		
		RoutingNetworkDelegate delegate = new RoutingNetworkDelegate(minX, minY, maxX, maxY, links.size());
		
		HashMap<AbstractRoutingNode, AbstractRoutingNode> nodeProxyMap = new HashMap<AbstractRoutingNode, AbstractRoutingNode>();
		HashMap<AbstractRoutingLink, AbstractRoutingLink> linkProxyMap = new HashMap<AbstractRoutingLink, AbstractRoutingLink>();
		
		//Copy the nodes
		for (AbstractRoutingNode node : nodes){
			AbstractRoutingNode copy = new AbstractRoutingNode.RoutingNodeCopy(node);
			
			delegate.quadTree.put(copy.getCoord().getX(), copy.getCoord().getY(), copy);
			nodeProxyMap.put(node, copy);
		}
		
		//Copy the links
		int currentIndex = 0;
		for (AbstractRoutingLink link : links){
			AbstractRoutingNode fromNode = nodeProxyMap.get(link.fromNode);
			AbstractRoutingNode toNode = nodeProxyMap.get(link.toNode);
			
			AbstractRoutingLink copy= null;		
			
			if (link instanceof RoutingWalkLink){
				copy = new RoutingWalkLink(fromNode, toNode, ((RoutingWalkLink) link).getLength(), WalkType.TRANSFER);
				copy.isCopy = true;
			} else if (link instanceof RoutingInVehicleLink){
				RoutingInVehicleLink wrapped = (RoutingInVehicleLink) link;
				copy = new RoutingInVehicleLink(fromNode, toNode, wrapped.data);
			}
			
			fromNode.outgoingLinks.add(copy);
			copy.index = currentIndex;
			delegate.links[currentIndex++] = copy;
			linkProxyMap.put(link, copy);
		}
		
		//Copy turn restrictions
		for (Entry<AbstractRoutingLink, AbstractRoutingLink> entry : linkProxyMap.entrySet()){
			AbstractRoutingLink link = entry.getKey();
			AbstractRoutingLink copy = entry.getValue();
			
			for (AbstractRoutingLink restrictedTurn : link.prohibitedOutgoingTurns){
				AbstractRoutingLink turnCopy = linkProxyMap.get(restrictedTurn);
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
		
		private final QuadTree<AbstractRoutingNode> quadTree;
		private final AbstractRoutingLink[] links;
		
		private AbstractRoutingNode ORIGIN_NODE = null;
		private AbstractRoutingLink[] accessLinks = null;
		private AbstractRoutingLink DESTINATION_LINK = null;
		private AbstractRoutingLink[] egressLinks = null;
		
		private RoutingNetworkDelegate(double minX, double minY, double maxX, double maxY, int numberOfLinks){
			quadTree = new QuadTree<AbstractRoutingNode>(minX, minY, maxX, maxY);
			this.links = new AbstractRoutingLink[numberOfLinks];
		}
		
		public Collection<AbstractRoutingNode> getNearestNodes(Coord coord, double radius){
			return this.quadTree.getDisk(coord.getX(), coord.getY(), radius);
		}
		
		/**
		 * Iterates through the links in the network, including the access and egress links.
		 * @return
		 */
		public Iterable<AbstractRoutingLink> iterLinks(){
			if (accessLinks == null || egressLinks == null)
				throw new NullPointerException("Tried to get links on an unprepared network. Use prepareForRouting first.");
			
			return new Iterable<AbstractRoutingLink>() {
				
				@Override
				public Iterator<AbstractRoutingLink> iterator() {
					return new Iterator<AbstractRoutingLink>() {
						
						int currentIndex = 0;
						
						@Override
						public void remove() { throw new UnsupportedOperationException(); }
						
						@Override
						public AbstractRoutingLink next() {							
							if (currentIndex < accessLinks.length)
								return accessLinks[currentIndex++];
							else if (currentIndex < (accessLinks.length + links.length))
								return links[currentIndex++ - accessLinks.length];
							else if (currentIndex < (accessLinks.length + links.length + egressLinks.length))
								return egressLinks[currentIndex++ - accessLinks.length - links.length];
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
		 * Prepares the network for a routing request. Creates ACCESS and EGRESS
		 * instances of {@link RoutingWalkLink} connected to the origin and
		 * destination. Also clears the delegate of stored pending cost data.
		 * 
		 * @param ORIGIN Origin coordinate.
		 * @param accessNodes Collection of access nodes.
		 * @param DESTINATIION Destination coordinate.
		 * @param egressNodes Collection of egress nodes.
		 */
		public void prepareForRouting(Coord ORIGIN, Collection<AbstractRoutingNode> accessNodes,
				Coord DESTINATIION, Collection<AbstractRoutingNode> egressNodes){
			
			clearEgressLinks(); //Dereference previous request's egress links
			reset(); //Reset the network links' pending costs & times.
			
			int nextIndex = links.length; //For the HasIndex interface
			
			ORIGIN_NODE = new RoutingNoStopNode(ORIGIN);
			accessLinks = new AbstractRoutingLink[accessNodes.size()];
			int i = 0;
			for(AbstractRoutingNode accessNode : accessNodes){
				RoutingWalkLink link = new RoutingWalkLink(ORIGIN_NODE, accessNode, WalkType.ACCESS);
				link.isCopy = true;
				ORIGIN_NODE.outgoingLinks.add(link);
				link.index = nextIndex++;
				accessLinks[i++] = link;
			}
			
			AbstractRoutingNode destination1 = new RoutingNoStopNode(DESTINATIION);
			AbstractRoutingNode destination2 = new RoutingNoStopNode(DESTINATIION);
			DESTINATION_LINK = new DestinationLink(destination1, destination2);
			egressLinks = new AbstractRoutingLink[egressNodes.size()];
			i = 0;
			for (AbstractRoutingNode egressNode : egressNodes){
				RoutingWalkLink link = new RoutingWalkLink(egressNode, destination1, WalkType.EGRESS);
				egressNode.outgoingLinks.add(link);
				link.toNode.outgoingLinks.add(DESTINATION_LINK);
				link.index = nextIndex++;
				link.isCopy = true;
				egressLinks[i++] = link;
			}
			DESTINATION_LINK.index = nextIndex;
		}
		
		private void clearEgressLinks(){
			//This method removes references to the egress links from the network. The ones from the previous
			//routing request should get cleaned by the GC next time it checks since they're not longer being used.
			if (egressLinks == null) return;
			
			for (AbstractRoutingLink link : egressLinks){
				link.fromNode.outgoingLinks.remove(link);
			}
		}
		
		private void reset(){
			for(AbstractRoutingLink link : links){
				link.reset();
			}
		}
		
		public AbstractRoutingNode getOriginNode(){
			if (this.ORIGIN_NODE == null) throw new NullPointerException("Use prepareForRouting first.");
			return this.ORIGIN_NODE;
		}
		
		public AbstractRoutingLink getDestinationLink(){
			if (this.ORIGIN_NODE == null) throw new NullPointerException("Use prepareForRouting first.");
			return this.DESTINATION_LINK;
		}
		
		public int getNumberOfLinks(){
			return accessLinks.length + links.length + egressLinks.length;
		}
	}
	
	private static class DestinationLink extends AbstractRoutingLink{

		public DestinationLink(AbstractRoutingNode fromNode, AbstractRoutingNode toNode) {
			super(fromNode, toNode);
		}
		
	}
	
}
