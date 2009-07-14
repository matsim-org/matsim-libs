package playground.mmoyo.TransitSimulation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.map.LRUMap;
import org.apache.commons.collections.map.MultiKeyMap;
import org.matsim.api.basic.v01.TransportMode;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.network.NodeImpl;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.routes.LinkNetworkRoute;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.api.basic.v01.Id;
import org.matsim.api.basic.v01.population.PlanElement;

/**
 * Translates logic nodes and links into plain nodes and links. 
 */
public class LogicIntoPlainTranslator {
	private NetworkLayer plainNet;
	private MultiKeyMap joiningLinkMap;
	private Map<Id,NodeImpl> logicToPlainStopMap;
	
	/**the constructor creates the joiningLinkMap that stores the relation between logic and plain Nodes*/ 
	public LogicIntoPlainTranslator(final NetworkLayer plainNetwork,  final Map<Id,NodeImpl> logicToPlanStopMap) {
		this.plainNet= plainNetwork;
		this.logicToPlainStopMap = logicToPlanStopMap;
		
		/**creates the JoiningLinkMap, it contains fromNode and toNode as keys, and the link between them as value */
		int linkNum = plainNet.getLinks().size();
		joiningLinkMap = MultiKeyMap.decorate(new LRUMap(linkNum));
		for (LinkImpl plainLink : plainNet.getLinks().values()){
			NodeImpl fromPlainNode= plainLink.getFromNode();
			NodeImpl toPlainNode= plainLink.getToNode();
			joiningLinkMap.put(fromPlainNode, toPlainNode, plainLink);
		}
	}

	private NodeImpl convertToPlain(Id logicNodeId){
		return logicToPlainStopMap.get(logicNodeId);
	}
	
	public List<NodeImpl> convertNodesToPlain(List<NodeImpl> logicNodes){
		List<NodeImpl> plainNodes  = new ArrayList<NodeImpl>();
		for (NodeImpl logicNode: logicNodes){
			NodeImpl plainNode= convertToPlain(logicNode.getId());
			plainNodes.add(plainNode);
		}
		return plainNodes;
	}	
	
	private LinkImpl convertToPlain(final LinkImpl logicLink){
		LinkImpl logicAliasLink = logicLink;
		if (logicLink.getType().equals("Transfer") || logicLink.getType().equals("DetTransfer"))
			logicAliasLink= findLastStandardLink(logicLink);
		NodeImpl fromPlainNode = convertToPlain(logicAliasLink.getFromNode().getId());
		NodeImpl toPlainNode = convertToPlain(logicAliasLink.getToNode().getId());
		LinkImpl planLink = (LinkImpl)joiningLinkMap.get(fromPlainNode, toPlainNode); 
		return planLink;
	}
	
	/**in case of a transfer link of the logic Network, the last standard link is returned, because transfer links do not exist in the plain network*/
	private LinkImpl findLastStandardLink (LinkImpl transferLink){
		LinkImpl standardLink = null;
		for (LinkImpl inLink: transferLink.getFromNode().getInLinks().values()){
			if (inLink.getType().equals("Standard"))
				standardLink = inLink;
		}
		if (standardLink==null)
			throw new java.lang.NullPointerException(this +  transferLink.getId().toString() + "Does not exist");
		return standardLink;
	}

	public List<LinkImpl> convertToPlain(List<LinkImpl> logicLinks){
		List<LinkImpl> plainLinks  = new ArrayList<LinkImpl>();
		for (LinkImpl logicLink: logicLinks){
			LinkImpl plainLink= convertToPlain(logicLink);
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
					LinkImpl plainLink= plainNet.getNearestLink(act.getCoord()); 
					act.setLink(plainLink);
				}else{
					LegImpl leg = (LegImpl)pe;
					NetworkRoute logicRoute = (NetworkRoute)leg.getRoute();
					List<LinkImpl> plainLinks = convertToPlain(logicRoute.getLinks());
					logicRoute.setLinks(null, plainLinks, null); 
				}
			}
		}
	}
	
	public List<LegImpl> convertToPlainLeg (List<LegImpl> logicLegList){
		List<LegImpl> plainLegList = new ArrayList<LegImpl>();
		for(LegImpl logicLeg : logicLegList){
			NetworkRoute logicRoute= (NetworkRoute)logicLeg.getRoute();
			List<LinkImpl> plainLinks = convertToPlain(logicRoute.getLinks());
			
			//if(plainLinks.size()>0){
				NetworkRoute plainRoute = new LinkNetworkRoute(null, null);
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
			NetworkRoute logicNetworkRoute= (NetworkRoute)logicLeg.getRoute();
			List<LinkImpl> plainLinkList = new ArrayList<LinkImpl>();
			
			for (LinkImpl link: logicNetworkRoute.getLinks()){
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
