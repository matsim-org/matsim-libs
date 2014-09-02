/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */
package playground.wrashid.parkingSearch.ppSim.jdepSim.routing;

import java.util.LinkedList;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.parking.lib.GeneralLib;
import org.matsim.core.mobsim.jdeqsim.util.Timer;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.core.router.Dijkstra;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;

import playground.wrashid.parkingSearch.ppSim.ttmatrix.TTMatrix;
import playground.wrashid.parkingSearch.ppSim.ttmatrix.TTMatrixFromStoredTable;

public class EditRoute {

	public static EditRoute globalEditRoute;
	Network network;
	private LeastCostPathCalculator routingAlgo;
	
	public EditRoute(TTMatrix ttMatrix,Network network){
		this.network=network;
		TravelDisutility travelCost=new DummyTravelDisutility();
		TravelTime travelTime=new TTMatrixBasedTravelTime(ttMatrix);
		routingAlgo = new Dijkstra(network, travelCost,travelTime);
	}
	
	public EditRoute(TTMatrix ttMatrix,Network network, LeastCostPathCalculator routingAlgo){
		this.network=network;
		this.routingAlgo = routingAlgo;
	}
	
	public LinkNetworkRouteImpl getRoute(double time, Id startLinkId, Id endLinkId){
		Path calcLeastCostPath = routingAlgo.calcLeastCostPath(network.getLinks().get(startLinkId).getToNode(), network.getLinks().get(endLinkId).getToNode(), time, null, null);
		List<Link> links=calcLeastCostPath.links;
		
		List<Id<Link>> linkIds=new LinkedList<Id<Link>>();
		for (Link link:links){
			linkIds.add(link.getId());
		}
		
		return new LinkNetworkRouteImpl(startLinkId, linkIds, endLinkId);
	}
	
	public LinkNetworkRouteImpl addInitialPartToRoute(double time, Id startLinkId,Leg leg){
		LinkNetworkRouteImpl lastPartOfRoute=(LinkNetworkRouteImpl) leg.getRoute();
		Node middleNode = network.getLinks().get(lastPartOfRoute.getStartLinkId()).getFromNode();
		Path calcLeastCostPath = routingAlgo.calcLeastCostPath(network.getLinks().get(startLinkId).getToNode(), middleNode, time, null, null);
		List<Link> links=calcLeastCostPath.links;
		
		List<Id<Link>> linkIds=new LinkedList<Id<Link>>();
		for (Link link:links){
			linkIds.add(link.getId());
		}
		
		for (Id<Link> linkId:lastPartOfRoute.getLinkIds()){
			linkIds.add(linkId);
		}
		
		return  new LinkNetworkRouteImpl(startLinkId, linkIds, lastPartOfRoute.getEndLinkId());
	}
	
	
	
	public LinkNetworkRouteImpl addLastPartToRoute(double time, Leg leg, Id newTargetLinkId){
		LinkNetworkRouteImpl firstPartOfRoute=(LinkNetworkRouteImpl) leg.getRoute();
		Node middleNode = network.getLinks().get(firstPartOfRoute.getEndLinkId()).getToNode();
		Path calcLeastCostPath = routingAlgo.calcLeastCostPath(middleNode, network.getLinks().get(newTargetLinkId).getFromNode(), time, null, null);
		
		List<Id<Link>> linkIds=new LinkedList<Id<Link>>();
		for (Id<Link> linkId:firstPartOfRoute.getLinkIds()){
			linkIds.add(linkId);
		}
		
		List<Link> links=calcLeastCostPath.links;
		
		for (Link link:links){
			linkIds.add(link.getId());
		}
		
		return  new LinkNetworkRouteImpl(firstPartOfRoute.getStartLinkId(), linkIds, newTargetLinkId);
	}
	
	
	
	
	
	
	
	//TODO: also allow partial replacements, where first 500m/1km is cut out of route
	
	public static void main(String[] args) {
		String plansFile = "c:/data/parkingSearch/psim/zurich/inputs/ktiRun24/singleAgentPlan_1000802.xml";
		String networkFile = "c:/data/parkingSearch/psim/zurich/inputs/ktiRun24/output_network.xml.gz";
		String facilititiesPath = "c:/data/parkingSearch/psim/zurich/inputs/ktiRun24/output_facilities.xml.gz";
		Scenario scenario = GeneralLib.readScenario(plansFile, networkFile, facilititiesPath);
		
		Network network=scenario.getNetwork();
		
		TTMatrix ttMatrix = new TTMatrixFromStoredTable("C:/data/parkingSearch/psim/zurich/inputs/it.50.3600secBin.ttMatrix.txt",
				network);
		
		TravelDisutility travelCost=new DummyTravelDisutility();
		TravelTime travelTime=new TTMatrixBasedTravelTime(ttMatrix);
		
	//	PreProcessEuclidean preProcessData = new PreProcessEuclidean(travelCost);
	//	preProcessData.run(network);
	//	LeastCostPathCalculator routingAlgo= new AStarEuclidean(network, preProcessData, travelTime);
		
		LeastCostPathCalculator routingAlgo = new Dijkstra(network, travelCost,travelTime);
		
		for (Person person:scenario.getPopulation().getPersons().values()){
			ActivityImpl activityImpl=(ActivityImpl) person.getSelectedPlan().getPlanElements().get(0);
			ActivityImpl activityWorkImpl=(ActivityImpl) person.getSelectedPlan().getPlanElements().get(2);
			
			Timer t=new Timer();
			t.startTimer();
			
			for (int i=0;i<100000;i++){
				Path path = routingAlgo.calcLeastCostPath(network.getLinks().get(activityImpl.getLinkId()).getToNode(), network.getLinks().get(activityWorkImpl.getLinkId()).getToNode(), 0, person, new DummyVehicle());
			}
			t.endTimer();
			t.printMeasuredTime("duration: ");
		
			
			
			
		}
		
		
		
	}
	
	
}

