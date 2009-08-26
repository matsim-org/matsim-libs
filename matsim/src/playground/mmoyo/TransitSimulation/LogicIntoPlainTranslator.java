package playground.mmoyo.TransitSimulation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.map.LRUMap;
import org.apache.commons.collections.map.MultiKeyMap;
import org.matsim.api.basic.v01.Id;
import org.matsim.api.basic.v01.TransportMode;
import org.matsim.api.basic.v01.population.PlanElement;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.core.population.routes.NetworkRouteWRefs;

/**
 * Translates logic nodes and links into plain nodes and links. 
 */
public class LogicIntoPlainTranslator {
	private NetworkLayer plainNet;
	private MultiKeyMap joiningLinkMap;
	private Map<Id,Node> logicToPlainStopMap;
	
	/**the constructor creates the joiningLinkMap that stores the relation between logic and plain Nodes*/ 
	public LogicIntoPlainTranslator(final NetworkLayer plainNetwork,  final Map<Id,Node> logicToPlanStopMap) {
		this.plainNet= plainNetwork;
		this.logicToPlainStopMap = logicToPlanStopMap;
		
		/**creates the JoiningLinkMap, it contains fromNode and toNode as keys, and the link between them as value */
		int linkNum = plainNet.getLinks().size();
		joiningLinkMap = MultiKeyMap.decorate(new LRUMap(linkNum));
		for (Link plainLink : plainNet.getLinks().values()){
			Node fromPlainNode= plainLink.getFromNode();
			Node toPlainNode= plainLink.getToNode();
			joiningLinkMap.put(fromPlainNode, toPlainNode, plainLink);
		}
	}

	public Node convertToPlain(Id logicNodeId){
		return logicToPlainStopMap.get(logicNodeId);
	}
	
	public List<Node> convertNodesToPlain(List<Node> logicNodes){
		List<Node> plainNodes  = new ArrayList<Node>();
		for (Node logicNode: logicNodes){
			Node plainNode= convertToPlain(logicNode.getId());
			plainNodes.add(plainNode);
		}
		return plainNodes;
	}	
	
	private Link convertToPlain(final Link logicLink){
		Link logicAliasLink = logicLink;
		if (((LinkImpl)logicLink).getType().equals("Transfer") || ((LinkImpl)logicLink).getType().equals("DetTransfer"))
			logicAliasLink= findLastStandardLink(logicLink);
		Node fromPlainNode = convertToPlain(logicAliasLink.getFromNode().getId());
		Node toPlainNode = convertToPlain(logicAliasLink.getToNode().getId());
		Link planLink = (Link)joiningLinkMap.get(fromPlainNode, toPlainNode); 
		return planLink;
	}
	
	/**in case of a transfer link of the logic Network, the last standard link is returned, because transfer links do not exist in the plain network*/
	private Link findLastStandardLink (Link transferLink){
		Link standardLink = null;
		for (Link inLink: transferLink.getFromNode().getInLinks().values()){
			if (((LinkImpl)inLink).getType().equals("Standard"))
				standardLink = inLink;
		}
		if (standardLink==null)
			throw new java.lang.NullPointerException(this +  transferLink.getId().toString() + "Does not exist");
		return standardLink;
	}

	public List<Link> convertToPlain(List<Link> logicLinks){
		List<Link> plainLinks  = new ArrayList<Link>();
		for (Link logicLink: logicLinks){
			Link plainLink= convertToPlain(logicLink);
			plainLinks.add(plainLink);
		}
		return plainLinks;
	}
	
	/**translates the plans of a whole population*/
	public void convertToPlain(PopulationImpl population){
		for (PersonImpl person: population.getPersons().values()) {
			PlanImpl plan = person.getPlans().get(0);
			for (PlanElement pe : plan.getPlanElements()) {  
				if (pe instanceof ActivityImpl) {  				
					ActivityImpl act =  (ActivityImpl) pe;					
					Link plainLink= plainNet.getNearestLink(act.getCoord()); 
					act.setLink(plainLink);
				}else{
					LegImpl leg = (LegImpl)pe;
					NetworkRouteWRefs logicRoute = (NetworkRouteWRefs)leg.getRoute();
					List<Node> plainNodes = convertNodesToPlain(logicRoute.getNodes());
					logicRoute.setNodes(null, plainNodes, null); 
				}
			}
		}
	}
	
	public List<LegImpl> convertToPlainLeg (List<LegImpl> logicLegList){
		List<LegImpl> plainLegList = new ArrayList<LegImpl>();
		for(LegImpl logicLeg : logicLegList){
			NetworkRouteWRefs logicRoute= (NetworkRouteWRefs)logicLeg.getRoute();
			List<Link> plainLinks = convertToPlain(logicRoute.getLinks());
			//if(plainLinks.size()>0){
				NetworkRouteWRefs plainRoute = new LinkNetworkRouteImpl(null, null);
				plainRoute.setLinks(null, plainLinks, null);
				LegImpl plainLeg = new LegImpl(logicLeg.getMode());
				plainLeg = logicLeg;
				plainLeg.setRoute(plainRoute);
				plainLegList.add(plainLeg);
				logicLeg.setRoute(plainRoute);
			//}
		}
		
		logicLegList = null;
		return plainLegList;
	}
	
	public List<LegImpl> convertToPlainLegORIGINAL (List<LegImpl> logicLegList){
		List<LegImpl> plainLegList = new ArrayList<LegImpl>();
		for(LegImpl logicLeg : logicLegList){
			NetworkRouteWRefs logicNetworkRoute= (NetworkRouteWRefs)logicLeg.getRoute();
			List<Link> plainLinkList = new ArrayList<Link>();
			
			for (Link link: logicNetworkRoute.getLinks()){
				//if (link.getType().equals("Standard"))
					plainLinkList.add(link);
			}
			if(plainLinkList.size()>0){
				NetworkRouteWRefs plainRoute = new LinkNetworkRouteImpl(null, null);
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
	
}
