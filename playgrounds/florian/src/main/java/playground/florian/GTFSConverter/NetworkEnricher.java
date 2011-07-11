package playground.florian.GTFSConverter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.routes.LinkNetworkRouteFactory;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.misc.ConfigUtils;

public class NetworkEnricher {
	
	private Map<Id,NetworkRoute> outputRoutes = new HashMap<Id,NetworkRoute>();
	
	private NetworkImpl inputNet;

	private ScenarioImpl sc;
	
	NetworkEnricher(NetworkImpl inputNet) {
		this.inputNet = inputNet;
		Config config = ConfigUtils.createConfig();
		sc = (ScenarioImpl)(ScenarioUtils.createScenario(config));
	}

	Map<Id, NetworkRoute> replaceLinks(Map<Id, List<Coord>> shapedLinks, Map<Id,NetworkRoute> netRoutes) {
		Map<Id,List<Id>> replacedLinks = new HashMap<Id,List<Id>>();
		Map<Id,List<Id>> fromNodes = new HashMap<Id,List<Id>>();
		Map<Coord,Node> existingNodes = new HashMap<Coord,Node>();
		// Copy Nodes from original Network to new Network
		for(Id id: inputNet.getNodes().keySet()){
			sc.getNetwork().createAndAddNode(id, inputNet.getNodes().get(id).getCoord());
			existingNodes.put(sc.getNetwork().getNodes().get(id).getCoord(), sc.getNetwork().getNodes().get(id));
		}
		// Replace Links
		for(Id linkId: inputNet.getLinks().keySet()){
			Link link = inputNet.getLinks().get(linkId);
			if(shapedLinks.containsKey(linkId)){				
				List<Id> newLinks = new ArrayList<Id>();
				Node n1 = sc.getNetwork().getNodes().get(link.getFromNode().getId());
				Node n2;
				int shapeCounter = 1;
				for(Coord x: shapedLinks.get(linkId)){
					boolean addLink = false;
					if(existingNodes.containsKey(x)){
						n2 = existingNodes.get(x);
					}else{
						n2 = sc.getNetwork().createAndAddNode(new IdImpl("s" + linkId.toString() + "." + shapeCounter), x);
						existingNodes.put(x, n2);
					}
					if(fromNodes.containsKey(n1.getId())){
						if(!(fromNodes.get(n1.getId()).contains(n2.getId()))){
							addLink = true;
						}else{
							newLinks.add(this.getLinkBetweenNodes(sc.getNetwork(),n1.getId(),n2.getId()).getId());
						}
					}else{
						fromNodes.put(n1.getId(), new ArrayList<Id>());
						addLink = true;
					}
					if(n1.getCoord().equals(x)){
						addLink = false;
					}
					if(addLink){
						double length = CoordUtils.calcDistance(n1.getCoord(), n2.getCoord());						
						double freespeed = 50/3.6;
						Link newLink = sc.getNetwork().createAndAddLink(new IdImpl(linkId + "." + shapeCounter), n1, n2, length, freespeed, 1500, 1);											
						// Change the linktype to pt
						Set<String> modes = new HashSet<String>();
						modes.add(TransportMode.pt);
						link.setAllowedModes(modes);
						fromNodes.get(n1.getId()).add(n2.getId());
						newLinks.add(newLink.getId());
						shapeCounter++;						
					}
					n1 = n2;
				}
				n2 = link.getToNode();
				if(!n2.getCoord().equals(n1.getCoord())){
					if(fromNodes.containsKey(n1.getId())){
						if(!(fromNodes.get(n1.getId()).contains(n2.getId()))){
							double length = CoordUtils.calcDistance(n1.getCoord(), n2.getCoord());
							double freespeed = 50/3.6;
							Link newLink = sc.getNetwork().createAndAddLink(new IdImpl(linkId + "." + shapeCounter), n1, n2, length, freespeed, 1500, 1);						
							// Change the linktype to pt
							Set<String> modes = new HashSet<String>();
							modes.add(TransportMode.pt);
							link.setAllowedModes(modes);
							newLinks.add(newLink.getId());
						}else{
							newLinks.add(this.getLinkBetweenNodes(sc.getNetwork(),n1.getId(),n2.getId()).getId());
						}
					}else{
						fromNodes.put(n1.getId(), new ArrayList<Id>());
						double length = CoordUtils.calcDistance(n1.getCoord(), n2.getCoord());
						double freespeed = 50/3.6;
						Link newLink = sc.getNetwork().createAndAddLink(new IdImpl(linkId + "." + shapeCounter), n1, n2, length, freespeed, 1500, 1);						
						// Change the linktype to pt
						Set<String> modes = new HashSet<String>();
						modes.add(TransportMode.pt);
						link.setAllowedModes(modes);
						newLinks.add(newLink.getId());
					}
					fromNodes.get(n1.getId()).add(n2.getId());
				}								
				replacedLinks.put(linkId, newLinks);
			}else{
				if(!sc.getNetwork().getLinks().containsKey(link.getId())){
					Link newLink = sc.getNetwork().createAndAddLink(link.getId(), sc.getNetwork().getNodes().get(link.getFromNode().getId()), sc.getNetwork().getNodes().get(link.getToNode().getId()), link.getLength(), link.getFreespeed(), link.getCapacity(), link.getNumberOfLanes());						
					// Change the linktype to pt
					Set<String> modes = new HashSet<String>();
					modes.add(TransportMode.pt);
					newLink.setAllowedModes(modes);
					if(fromNodes.containsKey(newLink.getFromNode().getId())){
						fromNodes.get(newLink.getFromNode().getId()).add(newLink.getToNode().getId());
					}else{
						List<Id> toNodes = new ArrayList<Id>();
						toNodes.add(newLink.getToNode().getId());
						fromNodes.put(newLink.getFromNode().getId(), toNodes);
					}
				}
			}
		}
		// Calculate Freespeed
		for(Id id: replacedLinks.keySet()){
			double oldLength = inputNet.getLinks().get(id).getLength();
			double oldFreespeed = inputNet.getLinks().get(id).getFreespeed();
			double newLength = 0;
			if ((oldFreespeed > 0) && (oldFreespeed != 50/3.6) && (oldLength != 0)){
				for(Id newId: replacedLinks.get(id)){
					newLength += sc.getNetwork().getLinks().get(newId).getLength();
				}
				double newFreespeed = oldFreespeed * newLength / oldLength;
				for(Id newId: replacedLinks.get(id)){
					sc.getNetwork().getLinks().get(newId).setFreespeed(newFreespeed);
				}
			}
		}
		// ReplaceLinks in Netroute
		for(Id id: netRoutes.keySet()){
			NetworkRoute route = netRoutes.get(id);
			LinkedList<Id> routeIds = new LinkedList<Id>();
			for(Id routedLinkId: route.getLinkIds()){
				if(replacedLinks.containsKey(routedLinkId)){
					routeIds.addAll(replacedLinks.get(routedLinkId));
				}else{
					routeIds.add(routedLinkId);
				}
			}
			NetworkRoute newRoute = (NetworkRoute) (new LinkNetworkRouteFactory()).createRoute(route.getStartLinkId(), route.getEndLinkId());
			newRoute.setLinkIds(route.getStartLinkId(), routeIds, route.getEndLinkId());
			outputRoutes.put(id, newRoute);
		}
		return outputRoutes;
	}

	
	private Link getLinkBetweenNodes(NetworkImpl net2, Id fromId, Id toId) {
		Link result = null;
		for(Link l: net2.getNodes().get(fromId).getOutLinks().values()){
			if(l.getToNode().getId().equals(toId)){
				result = l;
			}
		}
		if(result == null){
			System.out.println("Couldn't find a link between " + fromId + " and " + toId);
		}
		return result;
	}

	public NetworkImpl getEnrichedNetwork() {
		return sc.getNetwork();
	}

}
