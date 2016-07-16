package playground.toronto.router.routernetwork;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.misc.Counter;
import org.matsim.pt.router.TransitRouterNetwork;
import org.matsim.pt.router.TransitRouterNetwork.TransitRouterNetworkLink;
import org.matsim.pt.router.TransitRouterNetwork.TransitRouterNetworkNode;
import org.matsim.pt.transitSchedule.TransitRouteImpl;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import playground.toronto.exceptions.NetworkFormattingException;

public class TorontoTransitRouterNetwork  {

	private final static Logger log = Logger.getLogger(TorontoTransitRouterNetwork.class);
	public static String TRANSFER_MODE_NAME = "Transfer"; //The string name of the mode which indicates virtual-walk links (i.e., long vertical transfers between routes)
	public static String WALK_MODE_NAME = "Walk"; //The string name of the mode which indicates physical walk links (i.e., long horizontal walk connections between routes)
	
	/**
	 * <p>Creates a special "Toronto" {@link TransitRouterNetwork} from a given base network and schedule. In-line links are handled the same but transfer links
	 * between lines at stops are handled differently. Instead of using nearby stops for connections, connections are only made 
	 * between stops which 'touch' at the same network node. To compensate for this strict condition, the network is assumed to
	 * contain special 'override' transfer links, flagged by those links which permit a 'walk' or 'transfer' mode (see the static
	 * {@code TRANSFER_MODE_NAME} and {@code WALK_MODE_NAME} for defaults). </p>
	 * 
	 * <p>Transfers between {@link TransitRoute}s belonging to the same {@link TransitLine} are disabled. In the future, some code
	 * will be written to permit intra-line transfers at stops of confluence.</p>
	 * 
	 * <p>Two different transfer override types are recognized: WALK and TRANSFER. Walk links are assumed to be mostly horizontal
	 * physical paths between two nodes. Transfer links are assumed to be virtual transfers, possibly involving vertical movement;
	 * i.e., where the "real" link length (based on straight-line distance) does not represent the actual transfer time.</p> 
	 * 
	 * @param baseNetwork The multimodal base {@link Network} to build from. 
	 * @param schedule The {@link TransitSchedule} to build from
	 * @param transferPenalty The length used on all transfer links.
	 * @return A {@link TransitRouterNetwork} for routing transit trips.
	 * @throws NetworkFormattingException If a referenced ID cannot be found
	 * 
	 * @author pkucirek
	 */
	public static TransitRouterNetwork createTorontoTransitRouterNetwork(final Network baseNetwork, final TransitSchedule schedule, final double transferPenalty) 
			throws NetworkFormattingException{
		return createTorontoTransitRouterNetwork(baseNetwork, schedule, WALK_MODE_NAME, TRANSFER_MODE_NAME, transferPenalty);
	}
	
	/**
	 * <p>Creates a special "Toronto" {@link TransitRouterNetwork} from a given base network and schedule. In-line links are handled the same but transfer links
	 * between lines at stops are handled differently. Instead of using nearby stops for connections, connections are only made 
	 * between stops which 'touch' at the same network node. To compensate for this strict condition, the network is assumed to
	 * contain special 'override' transfer links, flagged by those links which permit a 'walk' or 'transfer' mode (see the static
	 * {@code TRANSFER_MODE_NAME} and {@code WALK_MODE_NAME} for defaults). </p>
	 * 
	 * <p>Transfers between {@link TransitRoute}s belonging to the same {@link TransitLine} are disabled. In the future, some code
	 * will be written to permit intra-line transfers at stops of confluence.</p>
	 * 
	 * <p>Two different transfer override types are recognized: WALK and TRANSFER. Walk links are assumed to be mostly horizontal
	 * physical paths between two nodes. Transfer links are assumed to be virtual transfers, possibly involving vertical movement;
	 * i.e., where the "real" link length (based on straight-line distance) does not represent the actual transfer time.</p> 
	 * 
	 * @param baseNetwork The multimodal base {@link Network} to build from. 
	 * @param schedule The {@link TransitSchedule} to build from
	 * @param walkMode The name of the walk mode
	 * @param transferMode The name of the transfer mode
	 * @param transferPenalty The length used on all transfer links.
	 * @return A {@link TransitRouterNetwork} for routing transit trips.
	 * @throws NetworkFormattingException If a referenced ID cannot be found
	 * 
	 * @author pkucirek
	 */
	public static TransitRouterNetwork createTorontoTransitRouterNetwork(final Network baseNetwork, final TransitSchedule schedule, final String walkMode,
			final String transferMode, final double transferPenalty) throws NetworkFormattingException{
		log.info("Creating base transit router network");
		final TransitRouterNetwork network = new TransitRouterNetwork();
		final Counter linkCounter = new Counter(" link #");
		final Counter nodeCounter = new Counter(" node #");
		long numberOfTransferLinks = 0;
		
		//This map tracks base network nodes to each of their created TRN Nodes
		HashMap<Node, List<TransitRouterNetworkNode>> baseToRouterNodeMap = new HashMap<Node, List<TransitRouterNetworkNode>>();		
		
		//This map tracks which base network nodes are connected to which other base network nodes for transfers.
		//This permits transfers from stops in only two conditions:
		//		1. Both stops are incident at the same node (i.e., their reference link's toNode are the same)
		//		2. Both stops' incident nodes are connected via a designated long transfer link (designated by a mode string)
		HashMap<Node, HashSet<Link>> baseConnections = new HashMap<Node, HashSet<Link>>();
		
		// build nodes and links connecting the nodes according to the transit routes
		for (TransitLine line : schedule.getTransitLines().values()) {
			Node networkNode = null;
			
			for (TransitRoute route : line.getRoutes().values()) {
				TransitRouterNetworkNode prevNode = null;
				
				for (TransitRouteStop stop : route.getStops()) {
					try{
						networkNode = baseNetwork.getLinks().get(stop.getStopFacility().getLinkId()).getToNode(); //Try to get the reference node
						
					}catch (NullPointerException e){
						throw new NetworkFormattingException("Transit stop '" + stop.getStopFacility().getId() + "' does not reference a valid network link!");
					}
					
					TransitRouterNetworkNode trnNode = network.createNode(stop, route, line); // Create the node
					nodeCounter.incCounter();
					
					if (baseToRouterNodeMap.containsKey(networkNode)){
						baseToRouterNodeMap.get(networkNode).add(trnNode);
					}else{
						List<TransitRouterNetworkNode> trnNodes = new ArrayList<TransitRouterNetwork.TransitRouterNetworkNode>();
						trnNodes.add(trnNode);
						baseToRouterNodeMap.put(networkNode, trnNodes);
					}
					
					networkNode = null;
					
					if (prevNode != null) {
						
						network.createLink(prevNode, trnNode, route, line); // Create the in-line link
						linkCounter.incCounter();
					}
					
					prevNode = trnNode;
				}
			}
		}
		
		network.finishInit();
		
		//Create the base node-to-node connectivity mapping
		for (Node node : baseNetwork.getNodes().values()){
			HashSet<Link> transferLinks = new HashSet<Link>();
			
			for (Link outLink : node.getOutLinks().values()){
				if (outLink.getAllowedModes().contains(transferMode) || outLink.getAllowedModes().contains(walkMode)){
					transferLinks.add(outLink);
				}
			}
			
			//Only save in the map if there is at least one stop at this node. Saves memory.
			if (baseToRouterNodeMap.containsKey(node))
				baseConnections.put(node, transferLinks);
		}
		
		for (Entry<Node, List<TransitRouterNetworkNode>> entry : baseToRouterNodeMap.entrySet()){
			//Should contain every node with an attached transit stop.

			//Connect together all stops incident at a single node
			for (TransitRouterNetworkNode fromNode : entry.getValue()){
				if (fromNode.getInLinks().size() == 0) continue;
				for (TransitRouterNetworkNode toNode : entry.getValue()){
					if (fromNode == toNode) continue; //Don't create loops
					if (toNode.getOutLinks().size() == 0) continue;
					if (fromNode.line == toNode.line) continue; //Don't connect two routes belonging to the same line
					if (fromNode.route == toNode.route) continue; //Or the same route
					//double length = CoordUtils.calcDistance(fromNode.getCoord(), toNode.getCoord());
					network.createLink(fromNode, toNode, null, null);
					linkCounter.incCounter();
					numberOfTransferLinks++;
				}
			}

			//Create all transfer connections (keeping in mind transfer penalties)
			HashSet<Link> transferLinks = baseConnections.get(entry.getKey());
			
			for (Link baseConnection : transferLinks){
				//baseConnection is the override transfer link in the base network.
				Node baseToNode = baseConnection.getToNode();
				
				if (!baseToRouterNodeMap.containsKey(baseToNode)){
					continue; //Skips. The code can reach this point if a walk or transfer link has been made to a node
						//which does not have any stops incident.
				}
				
				List<TransitRouterNetworkNode> toTrnStops = baseToRouterNodeMap.get(baseToNode);
				
				for (TransitRouterNetworkNode fromNode : entry.getValue()){ //For each stop mapped to this node
					for (TransitRouterNetworkNode toNode : toTrnStops){ //For each stop mapped to the other node
						
						if (fromNode.line == toNode.line) continue;
						if (fromNode.route == toNode.route) continue; 
						
						//TODO set transfer link length once that option becomes available.
						/*
						double length;
						if (baseConnection.getAllowedModes().contains(transferMode)){
							length = transferPenalty;
						}else{
							length = CoordUtils.calcDistance(fromNode.getCoord(), toNode.getCoord())
						}
						*/
						
						network.createLink(fromNode, toNode, null, null);
						linkCounter.incCounter();
						numberOfTransferLinks++;
					}
				}
			}
		}

		log.info("Done. " + numberOfTransferLinks + " transfer links added to the original transit router network.");

		return network;
	}
}


