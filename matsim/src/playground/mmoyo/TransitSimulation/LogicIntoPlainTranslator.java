package playground.mmoyo.TransitSimulation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.map.LRUMap;
import org.apache.commons.collections.map.MultiKeyMap;
import org.matsim.api.basic.v01.TransportMode;
import org.matsim.core.api.experimental.population.PlanElement;
import org.matsim.core.api.network.Link;
import org.matsim.core.api.network.Node;
import org.matsim.core.api.population.NetworkRoute;
import org.matsim.core.api.population.Person;
import org.matsim.core.api.population.Plan;
import org.matsim.core.api.population.Population;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.routes.LinkNetworkRoute;

/**
 * Translates logic nodes and links into plain nodes and links. 
 */
public class LogicIntoPlainTranslator {
	private NetworkLayer plainNet;
	private MultiKeyMap joiningLinkMap;
	private Map<Node,Node> logicToPlainStopMap;
	
	/**the constructor creates the joiningLinkMap that stores the relation between logic and plain Nodes*/ 
	public LogicIntoPlainTranslator(final NetworkLayer plainNetwork,  final Map<Node,Node> logicToPlanStopMap) {
		this.plainNet= plainNetwork;
		this.logicToPlainStopMap = logicToPlanStopMap;
		
		/**creates the JoiningLinkMa, it contains fromNode and toNode as keys, and the link between them as value */
		int linkNum = plainNet.getLinks().size();
		joiningLinkMap = MultiKeyMap.decorate(new LRUMap(linkNum));
		for (Link plainLink : plainNet.getLinks().values()){
			Node fromPlainNode= plainLink.getFromNode();
			Node toPlainNode= plainLink.getToNode();
			joiningLinkMap.put(fromPlainNode, toPlainNode, plainLink);
		}
	}

	private Node convertToPlain(Node logicNode){
		return logicToPlainStopMap.get(logicNode);
	}
	
	private Link convertToPlain(final Link logicLink){
		Link logicAliasLink = logicLink;
		if (logicLink.getType().equals("Transfer") || logicLink.getType().equals("DetTransfer"))
			logicAliasLink= findLastStandardLink(logicLink);
		Node fromPlainNode = convertToPlain(logicAliasLink.getFromNode());
		Node toPlainNode = convertToPlain(logicAliasLink.getToNode());
		Link planLink = (Link)joiningLinkMap.get(fromPlainNode, toPlainNode); 
		return planLink;
	}
	
	/**in case of a transfer link of the logic Network, the last standard link is returned, because transfer links do not exist in the pain network*/
	private Link findLastStandardLink (Link transferLink){
		Link standardLink = null;
		for (Link inLink: transferLink.getFromNode().getInLinks().values()){
			if (inLink.getType().equals("Standard"))
				standardLink = inLink;
		}
		if (standardLink==null)
			throw new java.lang.NullPointerException(this +  transferLink.getId().toString() + "Does not exist");
		return standardLink;
	}

	private List<Link> convertToPlain(List<Link> logicLinks){
		List<Link> plainLinks  = new ArrayList<Link>();
		for (Link logicLink: logicLinks){
			Link plainLink= convertToPlain(logicLink);
			plainLinks.add(plainLink);
		}
		return plainLinks;
	}
	
	/**translates the plans of a whole population*/
	public void convertToPlain(Population population){
		for (Person person: population.getPersons().values()) {
			Plan plan = person.getPlans().get(0);
			for (PlanElement pe : plan.getPlanElements()) {  
				if (pe instanceof ActivityImpl) {  				
					ActivityImpl act =  (ActivityImpl) pe;					
					Link plainLink= plainNet.getNearestLink(act.getCoord()); 
					act.setLink(plainLink);
				}else{
					LegImpl leg = (LegImpl)pe;
					NetworkRoute logicRoute = (NetworkRoute)leg.getRoute();
					List<Link> plainLinks = convertToPlain(logicRoute.getLinks());
					logicRoute.setLinks(null, plainLinks, null); 
				}
			}
		}
	}
	
	public List<LegImpl> convertToPlainLeg (List<LegImpl> logicLegList){
		List<LegImpl> plainLegList = new ArrayList<LegImpl>();
		for(LegImpl logicLeg : logicLegList){
			NetworkRoute logicNetworkRoute= (NetworkRoute)logicLeg.getRoute();
			List<Link> plainLinkList = new ArrayList<Link>();
			
			for (Link link: logicNetworkRoute.getLinks()){
				//if (link.getType().equals("Standard"))
					plainLinkList.add(link);
			}
			if(plainLinkList.size()>0){
				NetworkRoute plainRoute = new LinkNetworkRoute(null, null); 
				plainRoute.setLinks(null, plainLinkList, null);
				
				LegImpl plainLeg = new LegImpl(TransportMode.pt);
				plainLeg = logicLeg;
				plainLeg.setRoute(plainRoute);
				plainLegList.add(plainLeg);
				
				logicLeg.setRoute(plainRoute);
			}
		}
		logicLegList = null;
		return plainLegList;
	}
	
	/*
	public Path getPlainPath (final Path logicPath){
		List<Node> plainNodes = new ArrayList<Node>(); 
		List<Link> plainLinks = new ArrayList<Link>();
		double travelTime = logicPath.travelTime;
		double travelCost = logicPath.travelCost;
		
		for (Node logicNode: logicPath.nodes){
			Node plainNode= convertToPlainNode(logicNode);
			plainNodes.add(plainNode);
		}
		
		for (Link logicLink: logicPath.links){
			Link plainLink= convertToPlainLink(logicLink);
			plainLinks.add(plainLink);
		}
	
		Path plainPath = new Path(plainNodes, plainLinks, travelTime, travelCost);
		return plainPath;
	}
	*/
}
