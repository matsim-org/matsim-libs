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
package playground.wrashid.parkingSearch.ppSim.jdepSim.searchStrategies;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.parking.lib.DebugLib;
import org.matsim.contrib.parking.lib.GeneralLib;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;

import playground.wrashid.parkingChoice.infrastructure.api.Parking;
import playground.wrashid.parkingSearch.ppSim.jdepSim.AgentEventMessage;
import playground.wrashid.parkingSearch.ppSim.jdepSim.AgentWithParking;
import playground.wrashid.parkingSearch.ppSim.jdepSim.Message;
import playground.wrashid.parkingSearch.ppSim.jdepSim.routing.EditRoute;

public class RandomSearch implements ParkingSearchStrategy{
	
	
	private double maxDistance;
	private Network network;
	private Random random;
	private final double parkingDuration=60*2; 
	private final double walkSpeed=3.0 / 3.6; // [m/s]
	

	// go to final link if no parking there, then try parking at other places.
	// accept only parking within 300m, choose random links, but if leave 300m area, try
	// to take direction leading back to destination
	public RandomSearch (double maxDistance, Network network){
		this.maxDistance = maxDistance;
		this.network = network;
		this.random = MatsimRandom.getLocalInstance();
	}
	
	
	public HashSet<Id> extraSearchPathNeeded=new HashSet<Id>();
	
	
	@Override
	public void handleAgentLeg(AgentWithParking aem) {
		
		Event event = null;
		
		Leg leg = (LegImpl) aem.getPerson().getSelectedPlan().getPlanElements().get(aem.getPlanElementIndex());
		ActivityImpl prevAct = (ActivityImpl) aem.getPerson().getSelectedPlan().getPlanElements().get(aem.getPlanElementIndex()-1);
		ActivityImpl nextAct = (ActivityImpl) aem.getPerson().getSelectedPlan().getPlanElements().get(aem.getPlanElementIndex()+1);
	
		if (leg.getMode().equalsIgnoreCase(TransportMode.car)){
			
			List<Id> linkIds = ((LinkNetworkRouteImpl)leg.getRoute()).getLinkIds();
			LinkNetworkRouteImpl route= (LinkNetworkRouteImpl)leg.getRoute();
			
			boolean endOfLegReached = aem.getCurrentLinkIndex()==linkIds.size()-1;
			
			if (endOfLegReached){
				DebugLib.traceAgent(aem.getPerson().getId());
				Id parkingId = AgentWithParking.parkingManager.getFreeParkingFacilityOnLink(route.getEndLinkId(), "streetParking");
				ActivityImpl nextNonParkingAct = (ActivityImpl) aem.getPerson().getSelectedPlan().getPlanElements().get(aem.getPlanElementIndex()+3);
				
				// avoid temporary problem with car leave and next planned parking on same link
				// TODO: resolve in future implementation
				boolean invalidLink=false;
				int nextCarLegIndex = aem.duringCarLeg_getPlanElementIndexOfNextCarLeg();
				if (nextCarLegIndex!=-1){
					ActivityImpl nextActAfterNextCarLeg = (ActivityImpl) aem.getPerson().getSelectedPlan().getPlanElements().get(nextCarLegIndex+3);
					invalidLink=route.getEndLinkId().toString().equalsIgnoreCase(nextActAfterNextCarLeg.getLinkId().toString());
				}
				
				
				if (parkingId==null || invalidLink){
					DebugLib.traceAgent(aem.getPerson().getId(),1);
					extraSearchPathNeeded.add(aem.getPerson().getId());
					
					Random r=new Random();
					Link link = network.getLinks().get(route.getEndLinkId());
					
					Link nextLink = randomNextLink(link);
					ArrayList<Id> newRoute = new ArrayList<Id>();
					newRoute.addAll(route.getLinkIds());
					newRoute.add(link.getId());
					route.setLinkIds(route.getStartLinkId(), newRoute, nextLink.getId());
					route.setEndLinkId(nextLink.getId());
					
					// this will just continue the search of the agent
					aem.processLegInDefaultWay();
					
				} else {
					DebugLib.traceAgent(aem.getPerson().getId(),2);
					
					if (extraSearchPathNeeded.contains(aem.getPerson().getId())){
						extraSearchPathNeeded.remove(aem.getPerson().getId());
						
						aem.processEndOfLegCarMode_processEvents(leg, nextAct);
						
						setDurationOfParkingActivity(aem, nextAct);
						
						ActivityImpl currentParkingAct = (ActivityImpl) aem.getPerson().getSelectedPlan().getPlanElements().get(aem.getPlanElementIndex()+1);
						Leg nextWalkLeg = (Leg) aem.getPerson().getSelectedPlan().getPlanElements().get(aem.getPlanElementIndex()+2);
						Link parkingLink = network.getLinks().get(route.getEndLinkId());
						
						currentParkingAct.setLinkId(parkingLink.getId());
						
						double walkDistance=GeneralLib.getDistance(parkingLink.getCoord(), nextNonParkingAct.getCoord());
						// TODO: improve this later (no straight line)
						double walkDuration=walkDistance / walkSpeed;
						nextWalkLeg.setTravelTime(walkDuration);
						
						// check, if more car legs, only in that case adapt that leg (TODO:)
						
						// if car departs again during day, adapt the departure walking and routes
						
						int indexOfNextCarLeg = aem.duringCarLeg_getPlanElementIndexOfNextCarLeg();
						
						if (indexOfNextCarLeg!=-1){
							ActivityImpl lastActBeforeNextCarLeg = (ActivityImpl) aem.getPerson().getSelectedPlan().getPlanElements().get(indexOfNextCarLeg-3);
							Leg nextwalkLegToParking = (Leg) aem.getPerson().getSelectedPlan().getPlanElements().get(indexOfNextCarLeg-2);
							ActivityImpl nextParkingAct = (ActivityImpl) aem.getPerson().getSelectedPlan().getPlanElements().get(indexOfNextCarLeg-1);
							Leg nextCarLeg = (Leg) aem.getPerson().getSelectedPlan().getPlanElements().get(indexOfNextCarLeg);
							
							walkDistance=GeneralLib.getDistance(parkingLink.getCoord(), lastActBeforeNextCarLeg.getCoord());
							walkDuration=walkDistance / walkSpeed;
							nextwalkLegToParking.setTravelTime(walkDuration);

							nextParkingAct.setLinkId(parkingLink.getId());
							
							EditRoute.globalEditRoute.addInitialPartToRoute(nextNonParkingAct.getEndTime(), parkingLink.getId(), nextCarLeg);
						}
						
						aem.processEndOfLegCarMode_scheduleNextActivityEndEventIfNeeded(nextAct);
						
						
					} else {
						aem.processLegInDefaultWay();
					}
					AgentWithParking.parkingManager.parkVehicle(aem.getPerson().getId(), parkingId);
				}
			} else {
				aem.processLegInDefaultWay();
			}
		} else {
			
			setDurationOfParkingActivity(aem, nextAct);
			
			aem.processLegInDefaultWay();
			
		}
		
		// log search time and path! TODO:
		
		
		// TODO: add score only at end of search (store it locally during search)!
		
		//if (aem.getPlanElementIndex() >1 && aem.getPlanElementIndex() % 2 == 0){
		//	AgentWithParking.parkingStrategyManager.updateScore(person.getId(), aem.getPlanElementIndex()-1, 1*rand.nextDouble());
		//}
		
		
		// only consider arrival distance at the moment for scoring (both in future - but for this plans have to be pre-processed and cleaned first).
	}

	private void setDurationOfParkingActivity(AgentWithParking aem, ActivityImpl nextAct) {
		if (nextAct.getType().equalsIgnoreCase("parking")){
			nextAct.setEndTime(aem.getMessageArrivalTime() + parkingDuration);
		}
	}
	
	private Link randomNextLink(Link link) {
		List<Link> links = new ArrayList<Link>(link.getToNode().getOutLinks().values());

		int i = random.nextInt(links.size());
		return links.get(i);
	}

	@Override
	public String getName() {
		return "RandomSearch";
	}
	
	
	/*
	public void processEndOfLegCarMode(Leg leg, ActivityImpl nextAct,AgentWithParking aem) {
		Event event;
		
		List<Id> linkIds = ((LinkNetworkRouteImpl)leg.getRoute()).getLinkIds();
		Id currentLinkId=null;
		if (aem.getCurrentLinkIndex()==-1){
			currentLinkId=((LinkNetworkRouteImpl)leg.getRoute()).getStartLinkId();
		} else {
			currentLinkId = linkIds.get(aem.getCurrentLinkIndex());
		}
		
		event=new LinkLeaveEvent(aem.getMessageArrivalTime(),aem.getPerson().getId(),currentLinkId,aem.getPerson().getId());
		Message.eventsManager.processEvent(event);
		
		Id endLinkId = leg.getRoute().getEndLinkId();
		event=new LinkEnterEvent(aem.getMessageArrivalTime(),aem.getPerson().getId(),endLinkId,aem.getPerson().getId());
		Message.eventsManager.processEvent(event);
		
		event = new PersonArrivalEvent(aem.getMessageArrivalTime(),aem.getPerson().getId(),endLinkId , leg.getMode());
		Message.eventsManager.processEvent(event);
		
		aem.setPlanElementIndex(aem.getPlanElementIndex() + 1);
		boolean isLastActivity = aem.getPlanElementIndex()==aem.getPerson().getSelectedPlan().getPlanElements().size()-1;
		
		event = new ActivityStartEvent(aem.getMessageArrivalTime(),aem.getPerson().getId(), endLinkId, nextAct.getFacilityId(), nextAct.getType());
		aem.eventsManager.processEvent(event);
		
		
		if (!isLastActivity){
			double endTimeOfActivity = getEndTimeOfActivity(nextAct,getMessageArrivalTime());

			setMessageArrivalTime(endTimeOfActivity);
			messageQueue.schedule(this);
		}
	}
*/

}

