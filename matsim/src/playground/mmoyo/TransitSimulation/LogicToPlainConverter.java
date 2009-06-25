package playground.mmoyo.TransitSimulation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.map.LRUMap;
import org.apache.commons.collections.map.MultiKeyMap;
import org.matsim.core.api.network.Link;
import org.matsim.core.api.network.Network;
import org.matsim.core.api.network.Node;
import org.matsim.core.api.population.Activity;
import org.matsim.core.api.population.NetworkRoute;
import org.matsim.core.api.population.Person;
import org.matsim.core.api.population.Plan;
import org.matsim.core.api.population.PlanElement;
import org.matsim.core.api.population.Population;
import org.matsim.core.api.population.Leg;
import org.matsim.core.api.population.Route;
import org.matsim.api.basic.v01.Coord;
import org.matsim.core.network.NetworkLayer;

public class LogicToPlainConverter {
	private NetworkLayer plainNet;
	private MultiKeyMap joiningLinkMap;
	private Map<Node,Node> logicToPlainStopMap;
	
	public LogicToPlainConverter(final NetworkLayer plainNetwork,  final Map<Node,Node> logicToPlanStopMap) {
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
	
	private Link findLastStandardLink (Link transferLink){
		Link standardLink = null;
		for (Link inLink: transferLink.getFromNode().getInLinks().values()){
			if (inLink.getType().equals("Standard")){
				standardLink = inLink;
			}
		}
		
		if (standardLink==null){
			throw new java.lang.NullPointerException("Error with link " + transferLink.getId());
			//Node plainNode = transferLink.getToNode();
			//double length = 
			//plainNet.getNodes(coord);
		}
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
	
	public void convertToPlain (List<Leg> logicLegList){
		for (Leg leg: logicLegList){
			Route route = leg.getRoute();
			//TODO
		}
	}
	
	public void convertToPlain(Population population){
		for (Person person: population.getPersons().values()) {
			//if (true){ Person person = population.getPersons().get(new IdImpl("3937204"));
			Plan plan = person.getPlans().get(0);
			for (PlanElement pe : plan.getPlanElements()) {  
				if (pe instanceof Activity) {  				
					Activity act =  (Activity) pe;					
					//Link logicLink= act.getLink();
					//Link plainLink= this.convertToPlain(logicLink); 
					//act.setLink(plainLink);
					act.setLink(plainNet.getNearestLink(act.getCoord()));
				}else{
					Leg leg = (Leg)pe;
					NetworkRoute logicRoute = (NetworkRoute)leg.getRoute();
					List<Link> plainLinks = convertToPlain(logicRoute.getLinks());
					logicRoute.setLinks(null, plainLinks, null); 
				}
			}
		}
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
