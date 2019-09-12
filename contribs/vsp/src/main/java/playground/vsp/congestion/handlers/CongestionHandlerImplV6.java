///* *********************************************************************** *
// * project: org.matsim.*
// *                                                                         *
// * *********************************************************************** *
// *                                                                         *
// * copyright       : (C) 2014 by the members listed in the COPYING,        *
// *                   LICENSE and WARRANTY file.                            *
// * email           : info at matsim dot org                                *
// *                                                                         *
// * *********************************************************************** *
// *                                                                         *
// *   This program is free software; you can redistribute it and/or modify  *
// *   it under the terms of the GNU General Public License as published by  *
// *   the Free Software Foundation; either version 2 of the License, or     *
// *   (at your option) any later version.                                   *
// *   See also COPYING, LICENSE and WARRANTY file                           *
// *                                                                         *
// * *********************************************************************** */
//package playground.vsp.congestion.handlers;
//
//import java.util.ArrayList;
//import java.util.List;
//
//import org.matsim.api.core.v01.Id;
//import org.matsim.api.core.v01.Scenario;
//import org.matsim.api.core.v01.events.LinkLeaveEvent;
//import org.matsim.api.core.v01.network.Link;
//import org.matsim.api.core.v01.population.Leg;
//import org.matsim.api.core.v01.population.Person;
//import org.matsim.api.core.v01.population.PlanElement;
//import org.matsim.core.api.experimental.events.EventsManager;
//import org.matsim.core.population.routes.NetworkRoute;
//import org.matsim.core.router.TripStructureUtils;
//
//import playground.vsp.congestion.DelayInfo;
//import playground.vsp.congestion.events.CongestionEvent;
//
//
///**
// * @author amit
// * Another version of congestion handler, if a person is delayed, it will charge everything to the person who just left before if link is constrained by flow capacity else 
// * it will identify the bottleneck link (spill back causing link) and charge the person who just entered on that link.
// */
//
//public class CongestionHandlerImplV6 extends CongestionInfoHandler {
//
//	public CongestionHandlerImplV6(EventsManager events, Scenario scenario) {
//		super(events, scenario);
//		this.scenario = scenario;
//	}
//
//	private Scenario scenario;
//	
//	@Override
//	void calculateCongestion(LinkLeaveEvent event, DelayInfo delayInfo) {
//		
//		LinkCongestionInfo linkInfo = this.getLinkId2congestionInfo().get(event.getLinkId());
//		double delay = event.getTime() - linkInfo.getPersonId2freeSpeedLeaveTime().get(event.getDriverId());
//		
//		if(delay==0) return;
//		
//		Id<Person> causingAgent;
//		Id<Link> causingLink;
//		String congestionType;
//		
////		if(isLinkFree(event)){
//		if(linkInfo.getFlowQueue().isEmpty()){
//			/*
//			 * Instead of the above if statement, I should use the commented if statement (isLinkFree). So that, 
//			 * if an agent is first delayed by flowCapacity and then storageCapacity, it charges correct agent.
//			 * Right now (sep 15), the method linkInfo.isLinkFree(..) throws runtimeException because
//			 * Map containing freeSpeedLeaveTime is cleared beforehand.
//			 */
//			
//			causingLink = getDownstreamLinkInRoute(event.getDriverId());
//			// last entered person on this causing link is causing agent.
//			List<Id<Person>> personsEnteredOnCausingLink = new ArrayList<Id<Person>>(this.getLinkId2congestionInfo().get(causingLink).getPersonId2linkEnterTime().keySet());
//			causingAgent = personsEnteredOnCausingLink.get(personsEnteredOnCausingLink.size()-1); 
//			
//			if (causingAgent==null) {
//				if(delay==1){
//					this.addToDelayNotInternalized_roundingErrors(delay);
//					return;
//				}else {
//					throw new RuntimeException("Delay for person "+event.getDriverId()+" is "+ delay+" sec. But causing agent could not be located. This happened during event "+event.toString()+" Aborting...");
//				}
//			}
//			
//			congestionType = "StorageCapacity";
//			
//		} else {
//			causingLink = event.getLinkId();
//			causingAgent = linkInfo.getLastLeaveEvent().getDriverId() ;
//			congestionType = "flowCapacity";
//		}
//		
//		CongestionEvent congestionEvent = new CongestionEvent(event.getTime(), congestionType, causingAgent, 
//				event.getDriverId(), delay, causingLink, this.getLinkId2congestionInfo().get(causingLink).getPersonId2linkEnterTime().get(causingAgent));
//		this.getEventsManager().processEvent(congestionEvent);
//		this.addToTotalInternalizedDelay(delay);
//		
//	}
//	
//	private boolean isLinkFree( LinkLeaveEvent event ){
//		
//		boolean isLinkFree = false;
//
//		LinkCongestionInfo linkInfo = this.getLinkId2congestionInfo().get(event.getLinkId());
//		if(linkInfo.getLastLeaveEvent() == null) return true;
//		
//		// first check if agent will be delayed because of flowCapacity
//		double freeSpeedLeaveTimeOfLastLeftAgent = linkInfo.getPersonId2freeSpeedLeaveTime().get(linkInfo.getLastLeaveEvent());
//		double freeSpeedLeaveTimeOfNowAgent = linkInfo.getPersonId2freeSpeedLeaveTime().get(event.getDriverId());
//		
//		double timeHeadway = freeSpeedLeaveTimeOfNowAgent -  freeSpeedLeaveTimeOfLastLeftAgent;
//		double minTimeHeadway = linkInfo.getMarginalDelayPerLeavingVehicle_sec();
//		
//		if (timeHeadway < minTimeHeadway) isLinkFree = false;
//		else isLinkFree = true;
//		
////		double lastLeaveTime = linkInfo.getPersonId2linkLeaveTime().get(linkInfo.getLastLeaveEvent()); 
//		/*
//		 *  TODO : yet to fix this, at the moment (sep 15), lastLeaveTime will throw NullPointException because
//		 *  this map is cleared in AbstractCongestionHandler before the calculateCongestion() call.
//		 */
//		double lastLeaveTime = linkInfo.getLastLeaveEvent().getTime() ;
//		
//		/*
//		 * the following check ensures that the time gap between the current agent and last left agent is more than MarginalDelayPerLeavingVehicle_sec
//		 * i.e. agent is now delayed due to storage capacity
//		 */
//		
//		double earliestLeaveTime = Math.floor(lastLeaveTime+ linkInfo.getMarginalDelayPerLeavingVehicle_sec()) +1;
//		if(event.getTime() > earliestLeaveTime){
//			isLinkFree = true;
//		} else isLinkFree = false;
//		
//		return isLinkFree;
//		
//	}
//	
//	private Id<Link> getDownstreamLinkInRoute(Id<Person> personId){
//		List<PlanElement> planElements = scenario.getPopulation().getPersons().get(personId).getSelectedPlan().getPlanElements();
//		Leg leg = TripStructureUtils.getLegs(planElements).get( this.personId2legNr.get( personId ) ) ;
//		return ((NetworkRoute) leg.getRoute()).getLinkIds().get( this.personId2linkNr.get( personId ) ) ;
//	}
//	
//}
