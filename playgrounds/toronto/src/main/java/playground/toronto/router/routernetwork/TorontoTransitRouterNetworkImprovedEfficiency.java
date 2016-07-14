package playground.toronto.router.routernetwork;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.misc.Counter;
import org.matsim.pt.router.TransitRouterNetwork;
import org.matsim.pt.router.TransitRouterNetwork.TransitRouterNetworkNode;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

import playground.toronto.exceptions.NetworkFormattingException;

public class TorontoTransitRouterNetworkImprovedEfficiency  {

	private final static Logger log = Logger.getLogger(TorontoTransitRouterNetworkImprovedEfficiency.class);
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
	 * @param <K>
	 */
	public static <K> TransitRouterNetwork createTorontoTransitRouterNetwork(final Network baseNetwork, final TransitSchedule schedule, final String walkMode,
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
		
		//this map tracks the nodes at which each stop is mapped to
		HashMap<TransitRoute, List<Node>> routeToActualNodeMap = new HashMap<TransitRoute, List<Node>>();
		HashMap<TransitRoute, List<TransitRouterNetworkNode>> routeToTRNNmap = new HashMap<TransitRoute, List<TransitRouterNetworkNode>>();

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
						throw new NetworkFormattingException("Transit stop '" + stop.getStopFacility().getId() + "' does not reference a valid network link! It should be removed from the schedule");
					}
					
					TransitRouterNetworkNode trnNode = network.createNode(stop, route, line); // Create the node
					nodeCounter.incCounter();
					if(routeToTRNNmap.containsKey(route)){
						routeToTRNNmap.get(route).add(trnNode);
					}
					else if(!routeToTRNNmap.containsKey(route)){
						List<TransitRouterNetworkNode> list = new ArrayList<TransitRouterNetwork.TransitRouterNetworkNode>();
						list.add(trnNode);
						routeToTRNNmap.put(route, list);
					}
					
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
		HashMap<HashSet<TransitRoute>, HashSet<List<TransitRouterNetworkNode>>> routePairsAndViableTransferNodes = new HashMap<HashSet<TransitRoute>, HashSet<List<TransitRouterNetworkNode>>>();
		Map<Integer, List<Tuple<Integer, Integer>>> potentialReverseOverlap = new TreeMap<Integer, List<Tuple<Integer,Integer>>>();
		for(Entry<TransitRoute, HashSet<TransitRoute>> entry : routeToRoutesWithOverlapMapping.entrySet()){
			TransitRoute tr1 = entry.getKey();
			for(TransitRoute trValue : entry.getValue()){
				TransitRoute tr2 = trValue;
				List<Node> tr1Nodes = new ArrayList<Node>(routeToActualNodeMap.get(tr1));
				List<Node> tr2Nodes = new ArrayList<Node>(routeToActualNodeMap.get(tr2));
				HashMap<Integer, List<Tuple<Integer, Integer>>> overlappingSegments = new HashMap<Integer, List<Tuple<Integer, Integer>>>();
				int overlapSection = 0;
				for(int i = 0; i < tr1Nodes.size(); i ++) {
					for(int j =0; j < tr2Nodes.size(); j++) {
						Node n1 = tr1Nodes.get(i);
						Node n2 = tr2Nodes.get(j);								
						if(n1 == n2) {
							if(j >= tr2Nodes.size()) {
								System.err.println("wtf");
							}
							Tuple<Integer, Integer> indicies = new Tuple<Integer, Integer>(i, j);
								int lastIndex1 = i - 1;
								int lastIndex2 = j - 1;
								if(overlappingSegments.containsKey(overlapSection) &&
									overlappingSegments.get(overlapSection).get(overlappingSegments.get(overlapSection).size()-1).getFirst() == lastIndex1 &&
											overlappingSegments.get(overlapSection).get(overlappingSegments.get(overlapSection).size()-1).getSecond() == lastIndex2) {
										/*above if statement checks to see if the previous index 
										 * tuple for the current key are the previous indices in the
										 * actual list of node indices, if they are then the current node index 
										 * tuples is added to the mapping for the current overlapSection key
										 * else it represents a new overlap section and therefore a new ovelapSection
										 * key is generated below  
										 */
										overlappingSegments.get(overlapSection).add(indicies);
									}
									else{
										overlapSection++;
										if(overlappingSegments.containsKey(overlapSection)){
											System.err.println("wtf");
										}
										List<Tuple<Integer, Integer>> indexHolder = new ArrayList<Tuple<Integer,Integer>>();
										indexHolder.add(indicies);
										overlappingSegments.put(overlapSection, indexHolder);
									}
								
						}
					}
				}
				for(Entry<Integer, List<Tuple<Integer,Integer>>> overlap : overlappingSegments.entrySet()){
					if(overlap.getValue().size() > 1){
						int firstNodeIndex1 = overlap.getValue().get(0).getFirst();
						int lastNodeIndex1 = overlap.getValue().get(overlap.getValue().size() -1).getFirst();
						int firstNodeIndex2 = overlap.getValue().get(0).getSecond();
						int lastNodeIndex2 = overlap.getValue().get(overlap.getValue().size() -1).getSecond();

						HashSet<TransitRoute> key = new HashSet<TransitRoute>();
						key.add(tr2); key.add(tr1);
						List<TransitRouterNetworkNode> trNNHolderFirst = new ArrayList<TransitRouterNetworkNode>();
						List<TransitRouterNetworkNode> trNNHolderLast = new ArrayList<TransitRouterNetworkNode>();
						TransitRouterNetworkNode first1 = routeToTRNNmap.get(tr1).get(firstNodeIndex1);
						TransitRouterNetworkNode first2 =  routeToTRNNmap.get(tr2).get(firstNodeIndex2);
						TransitRouterNetworkNode last1 = routeToTRNNmap.get(tr1).get(lastNodeIndex1);
						TransitRouterNetworkNode last2 =  routeToTRNNmap.get(tr2).get(lastNodeIndex2);
						
						trNNHolderFirst.add(first1); trNNHolderFirst.add(first2);
						trNNHolderLast.add(last1); trNNHolderLast.add(last2);
						
						if(!routePairsAndViableTransferNodes.containsKey(key)){
							HashSet<List<TransitRouterNetworkNode>> thingy = new HashSet<List<TransitRouterNetworkNode>>();
							if(firstNodeIndex1 > 0) 
								 thingy.add(trNNHolderFirst);
							if(lastNodeIndex2 < (tr2Nodes.size()-1))
								thingy.add(trNNHolderLast);
							routePairsAndViableTransferNodes.put(key, thingy);
							}
						else if(routePairsAndViableTransferNodes.containsKey(key)){
							if(firstNodeIndex1 > 0) 	routePairsAndViableTransferNodes.get(key).add(trNNHolderFirst);
							if(lastNodeIndex2  < (tr2Nodes.size()-1))	routePairsAndViableTransferNodes.get(key).add(trNNHolderLast);
						}
					}
					potentialReverseOverlap.clear();
					//identifies sections which may overlap in the opposite direction, or overlap at a single location
					if(overlap.getValue().size() == 1) {
						potentialReverseOverlap.put(overlap.getKey(), overlap.getValue());
					}
				}
				//this clears out all the reverse direction overlap
				if(potentialReverseOverlap.size() > 1) {
					for(Entry<Integer, List<Tuple<Integer,Integer>>> overlapCandidate : potentialReverseOverlap.entrySet()){
						Tuple<Integer, Integer> theIndices = overlapCandidate.getValue().get(0);
						int firstIndex = theIndices.getFirst();
						int secondIndex = theIndices.getSecond();
						if(firstIndex > 0 && (secondIndex < tr2Nodes.size()-1)) {
							if(tr1Nodes.get(firstIndex-1) == tr2Nodes.get(secondIndex+1)) {
								overlappingSegments.remove(overlapCandidate.getKey());
							}
						}
						if(secondIndex > 0 && (firstIndex < tr1Nodes.size()-1)) {
							if(tr1Nodes.get(firstIndex+1) == tr2Nodes.get(secondIndex-1)) {
								overlappingSegments.remove(overlapCandidate.getKey());
							}
						}
					}
				}
				//will now add single node viable transfers which are not part of overlapping segment.
				for(Entry<Integer, List<Tuple<Integer,Integer>>> overlap : overlappingSegments.entrySet()){
					if(overlap.getValue().size() < 2){
						int firstNodeIndex1 = overlap.getValue().get(0).getFirst();
						int firstNodeIndex2 = overlap.getValue().get(0).getSecond();
						if((tr2Nodes.size()) -1 == firstNodeIndex2){
							continue;
						}
						HashSet<TransitRoute> key = new HashSet<TransitRoute>();
						key.add(tr2); key.add(tr1);
						List<TransitRouterNetworkNode> trNNHolderFirst = new ArrayList<TransitRouterNetworkNode>();
						TransitRouterNetworkNode first1 = routeToTRNNmap.get(tr1).get(firstNodeIndex1);
						TransitRouterNetworkNode first2 = routeToTRNNmap.get(tr2).get(firstNodeIndex2);
						trNNHolderFirst.add(first1); trNNHolderFirst.add(first2);
						
						if(!routePairsAndViableTransferNodes.containsKey(key)){
							HashSet<List<TransitRouterNetworkNode>> thingy = new HashSet<List<TransitRouterNetworkNode>>(); thingy.add(trNNHolderFirst);
							routePairsAndViableTransferNodes.put(key, thingy);
						}
						else if(routePairsAndViableTransferNodes.containsKey(key)){
							routePairsAndViableTransferNodes.get(key).add(trNNHolderFirst);
						}
					}
				}
			}
		}	
	//Create the base node-to-node connectivity mapping from the existing coded links 
		for (Node node : baseNetwork.getNodes().values()){
			HashSet<Link> transferLinks = new HashSet<Link>();
			for (Link outLink : node.getOutLinks().values()){
				if (outLink.getAllowedModes().contains(transferMode) || outLink.getAllowedModes().contains(walkMode) && outLink.getAllowedModes().size() < 2){
					transferLinks.add(outLink);
				}
			}			
			//Only save in the map if there is at least one stop at this node. Saves memory.
			if (baseToRouterNodeMap.containsKey(node))
				baseConnections.put(node, transferLinks);
			for (Link baseConnection : transferLinks){
				//baseConnection is the override transfer link in the base network.
				Node baseToNode = baseConnection.getToNode();				
					if (!baseToRouterNodeMap.containsKey(baseToNode) || !baseToRouterNodeMap.containsKey(node)){
						continue; //Skips. The code can reach this point if a walk or transfer link has been made to a node
								 //which does not have any stops incident.
						}
						List<TransitRouterNetworkNode> toTrnStops = baseToRouterNodeMap.get(baseToNode);
						for (TransitRouterNetworkNode fromNode : baseToRouterNodeMap.get(node)){ //For each stop mapped to this node
							for (TransitRouterNetworkNode toNode : toTrnStops){ //For each stop mapped to the other node
								if (fromNode.line == toNode.line) continue;
								if (fromNode.route == toNode.route) continue; 
								if(fromNode.route.getStops().indexOf(fromNode) == 0) continue;
								if(toNode.route.getStops().get(toNode.route.getStops().size()-1) == toNode.getStop()) continue;
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
		
		for(Entry<HashSet<TransitRoute>, HashSet<List<TransitRouterNetworkNode>>> e :routePairsAndViableTransferNodes.entrySet()) {
			for(List<TransitRouterNetworkNode> nodePairsToBeConnected : e.getValue()){ //connects all TRNNs flagged for connection 
				if(nodePairsToBeConnected.get(0).route == nodePairsToBeConnected.get(1).route) continue;
				network.createLink(nodePairsToBeConnected.get(0), nodePairsToBeConnected.get(1), null, null);
				linkCounter.incCounter(); 
				numberOfTransferLinks++;
				sameNodeConnections++;
			}
		}
		
		network.finishInit();
		

		log.info("Done. " + numberOfTransferLinks + " transfer links added to the original transit router network bringing the total size to " + network.getLinks().size() + "." + "\n" + "These new links consisted of " + transferconnections + " pre-defined connections from the base network and " + sameNodeConnections + " created from the transfer inteligence algorithm");
		return network;
	}
	private static HashSet<Tuple<List<Node>,Integer>> allSublistsofSizeN(int n, List<Node> fullList){
		if(n > fullList.size() || n < 1){
			throw new IllegalArgumentException("the sublist size is greater than the size of the full list or the sublist size is negative: " + n + "   "  + fullList.size());
		}
		HashSet<Tuple<List<Node>,Integer>> output = new HashSet<Tuple<List<Node>,Integer>>();
		for(int i = 0; i <= (fullList.size()-n); i++){
			List<Node> subList = new ArrayList<Node>();
			for(int j = i; j < n+i; j++){  
				subList.add(fullList.get(j));
			}
			Tuple<List<Node>, Integer> holder = new Tuple<List<Node>, Integer>(subList, i);
			output.add(holder);
		}
		return output;
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


