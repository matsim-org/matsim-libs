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
import org.matsim.core.utils.misc.Counter;
import org.matsim.pt.router.TransitRouterNetwork;
import org.matsim.pt.router.TransitRouterNetwork.TransitRouterNetworkNode;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

import playground.toronto.exceptions.NetworkFormattingException;

public class TorontoTransitRouterNetwork2  {

	private final static Logger log = Logger.getLogger(TorontoTransitRouterNetwork2.class);
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
		int transferconnections = 0;
		int sameNodeConnections = 0;
		//This map tracks base network nodes to each of their created TRN Nodes
		HashMap<Node, List<TransitRouterNetworkNode>> baseToRouterNodeMap = new HashMap<Node, List<TransitRouterNetworkNode>>();		
		HashMap<TransitRoute, List<Node>> routeToActualNodeMap = new HashMap<TransitRoute, List<Node>>();
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
					if (routeToActualNodeMap.containsKey(route)) {
						routeToActualNodeMap.get(route).add(networkNode);
					}else{
						List<Node> actualNodes = new ArrayList<Node>();
						actualNodes.add(networkNode);
						routeToActualNodeMap.put(route, actualNodes);
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
		
		HashMap<TransitRoute, HashSet<TransitRoute>> routeToRoutesWithOverlapMapping = new HashMap<TransitRoute, HashSet<TransitRoute>>();
		
		for(Entry<TransitRoute, List<Node>> entry : routeToActualNodeMap.entrySet()){
			for(Node node : entry.getValue()){
				for(Entry<TransitRoute, List<Node>> compareList : routeToActualNodeMap.entrySet()){
					if(compareList.equals(entry)){
						continue;
					}
					else if(compareList.getValue().contains(node)){
						if(!routeToRoutesWithOverlapMapping.containsKey(entry.getKey())){
							HashSet<TransitRoute> overLap = new HashSet<TransitRoute>();
							overLap.add(compareList.getKey());
							routeToRoutesWithOverlapMapping.put(entry.getKey(), overLap);
						}
						else{
							routeToRoutesWithOverlapMapping.get(entry.getKey()).add(compareList.getKey()); 
						}
					}
				}
			}
		}
		HashMap<TransitRoute, List<List<Node>>> routeAndNodeSubLists = new HashMap<TransitRoute, List<List<Node>>>();
		for(Entry<TransitRoute, List<Node>> entry : routeToActualNodeMap.entrySet()) {
			List<List<Node>> nodeSubLists = listOfSubLists(entry.getValue());
			nodeSubLists = orderTheSubLits(nodeSubLists);
			routeAndNodeSubLists.put(entry.getKey(), nodeSubLists);
		}
		HashMap<HashSet<TransitRoute>, HashSet<Node>> routePairsAndTheirOverlappingSegments = findOverlap(routeAndNodeSubLists, routeToRoutesWithOverlapMapping);
		HashMap<HashSet<TransitRoute>, HashSet<TransitRouterNetworkNode>> routePairsAndViableTransferNodes = new HashMap<HashSet<TransitRoute>, HashSet<TransitRouterNetworkNode>>();
		for(Entry<HashSet<TransitRoute>, HashSet<Node>> e : routePairsAndTheirOverlappingSegments.entrySet()) {
			HashSet<TransitRoute> routePair = e.getKey();
			for(Node n : e.getValue()){
				if(!routePairsAndViableTransferNodes.containsKey(e.getKey())){
					HashSet<TransitRouterNetworkNode> trNNHolder = new HashSet<TransitRouterNetworkNode>();
					for(TransitRouterNetworkNode candidateTRNN : baseToRouterNodeMap.get(n)){
						for(TransitRoute tr : e.getKey()){
							TransitRoute candidateRoute = candidateTRNN.getRoute();
							if(candidateTRNN.getRoute().equals(tr)) {
								trNNHolder.add(candidateTRNN);	
							}
						}
					}
					routePairsAndViableTransferNodes.put(routePair, trNNHolder);
				}
				else if(routePairsAndViableTransferNodes.containsKey(e.getKey())){
					for(TransitRouterNetworkNode candidateTRNN : baseToRouterNodeMap.get(n)){
						for(TransitRoute tr : e.getKey()){
							if(candidateTRNN.getRoute().equals(tr)) {
								routePairsAndViableTransferNodes.get(routePair).add(candidateTRNN);
							}
						}
					}
				}
			}
		}
			routePairsAndViableTransferNodes.entrySet();
		
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
			HashSet<HashSet<TransitRoute>> routesWithNoConnection = new HashSet<HashSet<TransitRoute>>();
			//Connect together two stops incident at a single node which occur at the start or end of an overlap segment 
			for (TransitRouterNetworkNode fromNode : entry.getValue()){
				for (TransitRouterNetworkNode toNode : entry.getValue()){
					if (fromNode == toNode) continue; //Don't create loops
					HashSet<TransitRoute> routePairTester = new HashSet<TransitRoute>();
					routePairTester.add(fromNode.route); routePairTester.add(toNode.route);
					if(routesWithNoConnection.contains(routePairTester)) {
						break;
					}
					if(!routePairsAndViableTransferNodes.containsKey(routePairTester)) {
						System.err.println("WARNING: there are no transfers being created between routes " + fromNode.route.getId() + " and " + toNode.route.getId());
						routesWithNoConnection.add(routePairTester);
						break;
					}
					if(!routePairsAndViableTransferNodes.get(routePairTester).contains(fromNode) || !routePairsAndViableTransferNodes.get(routePairTester).contains(toNode)) continue; 
					//  these two are for the previous code, we now have inteligence to account for this
					//	if (fromNode.line == toNode.line) continue; //Don't connect two routes belonging to the same line
						if (fromNode.route == toNode.route) continue; //Or the same route
					
					
					//double length = CoordUtils.calcDistance(fromNode.getCoord(), toNode.getCoord());
					network.createLink(fromNode, toNode, null, null);
					linkCounter.incCounter();
					numberOfTransferLinks++;
					sameNodeConnections++;
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
						transferconnections++;
					}
				}
			}
		}

		log.info("Done. " + numberOfTransferLinks + " transfer links added to the original transit router network bringing the total size to " + network.getLinks().size() + "." + "\n" + "These new links consisted of " + transferconnections + " pre-defined connections from the base network and " + sameNodeConnections + " created from the transfer inteligence algorithm");
		return network;
	}
	
		
	private static List<List<Node>> listOfSubLists(List<Node> fullList) {
		List<Node> newList = new ArrayList<Node>(fullList);
		List<List<Node>> listOfSubListsHolder = new ArrayList<List<Node>>();
		listOfSubListsHolder.add(fullList);
		while(newList.size() > 1) {
			Node first = newList.get(0);
			List<Node> firstArray = new ArrayList<Node>();
			firstArray.add(first);
			listOfSubListsHolder.add(firstArray);
			newList.remove(0);
			List<Node> temp = new ArrayList<Node>(newList);
			listOfSubListsHolder.add(temp);
			List<Node> tempList = new ArrayList<Node>();
			for(Node n : newList){
				tempList.add(n);
			}
			while(tempList.size() > 1) {
				int lastIndex = tempList.size() -1;
				tempList.remove(lastIndex);
				if(tempList.size() == 0) {
					return listOfSubListsHolder;
				}
				ArrayList<Node> addList = new ArrayList<Node>();
				addList.add(first);
				addList.addAll(tempList);
				listOfSubListsHolder.add(addList);
			}
		}
		return listOfSubListsHolder;
	}
	private static List<List<Node>> orderTheSubLits(List<List<Node>> unordered) {
		List<List<Node>> orderedSubLists = new ArrayList<List<Node>>();
		for(List<Node> subList : unordered) {
			if(orderedSubLists.isEmpty()){
				orderedSubLists.add(subList);
			}
			else {
				int index = -1;
				for(int i = 0; i < orderedSubLists.size(); i ++){
					if(orderedSubLists.get(i).size() < subList.size()) {
						index = i;
						break;
					}
				}
				if(index == -1) {
					orderedSubLists.add(subList);
				} else {
					orderedSubLists.add(index, subList);
				}
			}
		}
		return orderedSubLists;
	}
	private static HashMap<HashSet<TransitRoute>, HashSet<Node>>	findOverlap(HashMap<TransitRoute, List<List<Node>>> routeAndNodeSubLists, HashMap<TransitRoute, HashSet<TransitRoute>> routeToRoutesWithOverlapMapping) {
		HashMap<HashSet<TransitRoute>, HashSet<Node>> output = new HashMap<HashSet<TransitRoute>, HashSet<Node>>();
		for(Entry<TransitRoute, List<List<Node>>> entry : routeAndNodeSubLists.entrySet()) {
			TransitRoute routeA = entry.getKey();
			if(routeToRoutesWithOverlapMapping.containsKey(routeA)) {
				HashSet<TransitRoute> overlappingRoutes = routeToRoutesWithOverlapMapping.get(routeA);
				List<List<Node>> routeA_NodeSublists = routeAndNodeSubLists.get(routeA);
				
				for(TransitRoute overlappingRoute : overlappingRoutes){
					HashSet<TransitRoute> transitRoutePairWithOverlap = new HashSet<TransitRoute>();
					transitRoutePairWithOverlap.add(overlappingRoute); transitRoutePairWithOverlap.add(routeA);
					List<List<Node>> routeB_NodeSublists = routeAndNodeSubLists.get(overlappingRoute);
					List<Node> alreadyCoveredOrExcludedNodes = new ArrayList<Node>();
					for(int i = 0 ; i < routeA_NodeSublists.size(); i ++) {
						for(int j = 0; j < routeB_NodeSublists.size(); j++) {
							if(!alreadyCoveredOrExcludedNodes.containsAll(routeA_NodeSublists.get(i))) {
								List<Node> reverseOfA = palindrome(routeA_NodeSublists.get(i));
								if(reverseOfA.equals(routeB_NodeSublists.get(j))){
									alreadyCoveredOrExcludedNodes.addAll(reverseOfA);
								}		
								if(routeA_NodeSublists.get(i).equals(routeB_NodeSublists.get(j))){
									if(!output.containsKey(transitRoutePairWithOverlap)){
										HashSet<Node> subRouteHolder = new HashSet<Node>();
										subRouteHolder.add(routeA_NodeSublists.get(i).get(0));
										subRouteHolder.add(routeA_NodeSublists.get(i).get(routeA_NodeSublists.get(i).size()-1));
										output.put(transitRoutePairWithOverlap, subRouteHolder);
									} else if(!output.get(transitRoutePairWithOverlap).contains(routeA_NodeSublists.get(i))){
										output.get(transitRoutePairWithOverlap).add(routeA_NodeSublists.get(i).get(0));
										output.get(transitRoutePairWithOverlap).add(routeA_NodeSublists.get(i).get(routeA_NodeSublists.get(i).size()-1));
										
									}
									alreadyCoveredOrExcludedNodes.addAll(routeA_NodeSublists.get(i));
								}
							}
						}
					}
				}
			}
		}
		return output;
	}
	private static List<Node> palindrome(List<Node> initial) {
		List<Node> reverse = new ArrayList<Node>();
		for(int i = (initial.size()-1); i > -1; i--){
			reverse.add(initial.get(i));
		}
		return reverse;
	}
}


