/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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
package playground.vsp.congestion.handlers;

import java.io.BufferedWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.PersonStuckEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.vehicles.VehicleUtils;

import playground.vsp.congestion.CongestionType;
import playground.vsp.congestion.LinkCongestionInfoExtended;
import playground.vsp.congestion.events.MarginalCongestionEvent;

/**
 * @author amit
 * Another version of congestion handler, if a person is delayed, it will charge everything to the person who just left before if link is constrained by flow capacity else 
 * it will identify the bottleneck link (spill back causing link) and charge the person who just entered on that link.
 */

public class CongestionHandlerImplV6 implements PersonDepartureEventHandler, LinkEnterEventHandler, LinkLeaveEventHandler,
PersonStuckEventHandler
{

	public static final Logger  log = Logger.getLogger(CongestionHandlerImplV6.class);

	private final EventsManager events;
	private final Scenario scenario;

	private final List<String> congestedModes = new ArrayList<String>();
	private final Map<Id<Link>, LinkCongestionInfoExtended> 	linkId2congestionInfo = new HashMap<>();
	private final Map<Id<Person>, String> personId2LegMode = new HashMap<>();
	private double totalDelay = 0;
	private double roundingErrors =0;

	/**
	 * @param events
	 * @param scenario must contain network and config
	 */
	public CongestionHandlerImplV6(EventsManager events, Scenario scenario) {
		this.events = events;
		this.scenario = scenario;

		congestedModes.addAll(this.scenario.getConfig().qsim().getMainModes());
		if (congestedModes.size()>1) throw new RuntimeException("Mixed traffic is not tested yet.");

		if (this.scenario.getConfig().scenario().isUseTransit()) {
			log.warn("Mixed traffic (simulated public transport) is not tested. Vehicles may have different effective cell sizes than 7.5 meters.");
		}
		storeLinkInfo();
	}

	@Override
	public void handleEvent(PersonStuckEvent event) {
		log.warn("An agent is stucking. No garantee for right calculation of external congestion effects "
				+ "because there are no linkLeaveEvents for stucked agents.: \n" + event.toString());
	}

	@Override
	public void reset(int iteration) {
		this.personId2LegMode.clear();
		this.linkId2congestionInfo.clear();

		storeLinkInfo();
	}

	private void storeLinkInfo(){
		for(Link link : scenario.getNetwork().getLinks().values()){
			LinkCongestionInfoExtended linkInfo = new LinkCongestionInfoExtended();
			linkInfo.setLinkId(link.getId());
			double flowCapacity_CapPeriod = link.getCapacity() * this.scenario.getConfig().qsim().getFlowCapFactor();
			double marginalDelay_sec = ((1 / (flowCapacity_CapPeriod / this.scenario.getNetwork().getCapacityPeriod()) ) );
			linkInfo.setMarginalDelayPerLeavingVehicle(marginalDelay_sec);
			linkId2congestionInfo.put(link.getId(), linkInfo);
		}
	}

	@Override
	public void handleEvent(PersonDepartureEvent event) {
		String travelMode = event.getLegMode();
		event.getLegMode();
		if(congestedModes.contains(travelMode)){
			LinkCongestionInfoExtended linkInfo = this.linkId2congestionInfo.get(event.getLinkId());
			linkInfo.getPersonId2freeSpeedLeaveTime().put(event.getPersonId(), event.getTime() + 1);
			linkInfo.getPersonId2linkEnterTime().put(event.getPersonId(), event.getTime());
			linkInfo.setLastEnteredAgent(event.getPersonId());
		}
		this.personId2LegMode.put(event.getPersonId(), travelMode);
		if(event.getPersonId().toString().equals("16310_1")){
			System.out.println(event.getPersonId().toString()+event.toString());
		}
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		Id<Person> personId = Id.createPersonId(event.getVehicleId().toString());
		LinkCongestionInfoExtended linkInfo = this.linkId2congestionInfo.get(event.getLinkId());
		double minLinkTravelTime = getEarliestLinkExitTime(scenario.getNetwork().getLinks().get(event.getLinkId()), personId2LegMode.get(personId));
		linkInfo.getPersonId2freeSpeedLeaveTime().put(personId, event.getTime()+ minLinkTravelTime + 1.0);
		linkInfo.getPersonId2linkEnterTime().put(personId, event.getTime());
		linkInfo.setLastEnteredAgent(personId);
	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {
		Id<Person> personId = Id.createPersonId(event.getVehicleId().toString());
		Id<Link> linkId	= event.getLinkId();
		double linkLeaveTime = event.getTime();

		LinkCongestionInfoExtended linkInfo = this.linkId2congestionInfo.get(linkId);
		double freeSpeedLeaveTime = linkInfo.getPersonId2freeSpeedLeaveTime().get(personId);
		double delay = linkLeaveTime - freeSpeedLeaveTime ;

		if(delay > 0.){
			totalDelay += delay;

			Id<Person> causingAgent;
			Id<Link> causingLink;
			String congestionType;

			if(linkInfo.isLinkFree(linkLeaveTime)){
				causingLink = getNextLinkInRoute(personId, linkId, linkLeaveTime);
				causingAgent = linkId2congestionInfo.get(causingLink).getLastEnteredAgent(); 

				if (causingAgent==null ) {
					if(delay==1){
						roundingErrors+=delay;
						log.warn("Delays are more than 0 for person "+personId+" but there is no causing agent. This should not happen.");
						return;
					}else {
						throw new RuntimeException("Delay for person "+personId+" is"+delay+" sec. But causing agent could not be located. Aborting...");
					}
				} 

				congestionType = CongestionType.SpillbackDelay.toString();

			} else {
				causingLink = linkId;
				causingAgent = Id.createPersonId(linkInfo.getLastLeavingAgent().toString());
				congestionType = CongestionType.FlowDelay.toString();
			}

			MarginalCongestionEvent congestionEvent = new MarginalCongestionEvent(linkLeaveTime, congestionType, causingAgent, 
					personId, delay, causingLink, linkId2congestionInfo.get(causingLink).getPersonId2linkEnterTime().get(causingAgent));
						System.out.println(congestionEvent);
			this.events.processEvent(congestionEvent);
		}
		linkInfo.setLastLeavingAgent(personId);
		linkInfo.setLastLeaveTime(linkLeaveTime);
	}

	/**
	 * @param link to get link length and maximum allowed (legal) speed on link
	 * @param travelMode to get maximum speed of vehicle
	 * @return minimum travel time on above link depending on the allowed link speed and vehicle speed
	 */
	private double getEarliestLinkExitTime(Link link, String travelMode){
		if(!travelMode.equals(TransportMode.car)) throw new RuntimeException("Travel mode other than car is not implemented yet. Thus aborting ...");
		double linkLength = link.getLength(); // see org.matsim.core.mobsim.qsim.qnetsimengine.DefaultLinkSpeedCalculator.java
		//		Id<VehicleType> vehTyp = Id.create(travelMode,VehicleType.class);
		double vehSpeed = VehicleUtils.getDefaultVehicleType().getMaximumVelocity(); //VehicleUtils.createVehiclesContainer().getVehicleTypes().get(vehTyp).getMaximumVelocity();
		double maxFreeSpeed = Math.min(link.getFreespeed(), vehSpeed);
		double minLinkTravelTime = Math.floor(linkLength / maxFreeSpeed );
		return minLinkTravelTime;
	}

	/**
	 * @param time if person have same 'next link in route' more than one time, given time is compared with 
	 * activity end time to get the true 'next link in route'.
	 * @return next link in the route of the person, which is currently on given link.
	 */
	private Id<Link> getNextLinkInRoute(Id<Person> personId, Id<Link> linkId, double time){
		List<PlanElement> planElements = scenario.getPopulation().getPersons().get(personId).getSelectedPlan().getPlanElements();

		Map<NetworkRoute, List<Id<Link>>> nRoutesAndLinkIds = new LinkedHashMap<NetworkRoute, List<Id<Link>>>(); // save all routes and links in each route
		List<Double> activityEndTimes = new ArrayList<Double>();
		for(PlanElement pe :planElements){
			if(pe instanceof Leg){
				NetworkRoute nRoute = ((NetworkRoute)((Leg)pe).getRoute()); 
				List<Id<Link>> linkIds = new ArrayList<Id<Link>>();
				linkIds.add(nRoute.getStartLinkId());
				linkIds.addAll(nRoute.getLinkIds());  
				linkIds.add(nRoute.getEndLinkId());
				nRoutesAndLinkIds.put(nRoute, linkIds);
			}
			if(pe instanceof Activity){
				double actEndTime = ((Activity)pe).getEndTime();
				activityEndTimes.add(actEndTime);
			}
		}

		Id<Link> nextLinkInRoute =  Id.create("NA",Link.class);
		List<Id<Link>> nextLinksInRoutes = new ArrayList<Id<Link>>();

		for(NetworkRoute nr:nRoutesAndLinkIds.keySet()){
			List<Id<Link>>linkIds = nRoutesAndLinkIds.get(nr);
			Iterator<Id<Link>> it = linkIds.iterator();
			do{
				if(it.next().equals(linkId)&&it.hasNext()){
					nextLinksInRoutes.add(it.next());
					break;
				}
			}while(it.hasNext());
		}

		if(nextLinksInRoutes.size()==0) throw new RuntimeException("There is no next link in the route of person "+personId+". Check!!!");
		else if(nextLinksInRoutes.size()==1) nextLinkInRoute = nextLinksInRoutes.get(0);
		else {
			for(int i=0; i < (activityEndTimes.size()-1);){
				if(activityEndTimes.get(i)<time && activityEndTimes.get(i+1)>0){
					nextLinkInRoute =  nextLinksInRoutes.get(i);
					break;
				} else {
					throw new RuntimeException("There are more than one links which come next to link"+linkId+" for perosn "+personId+
							". To check activity end times are used but condition is not satisfied. Aborting... ");
				}
			}
		}
		if(nextLinkInRoute==null || nextLinkInRoute.equals(Id.create("NA",Link.class))){
			log.warn("Next link in route of person "+personId+ " on link "+linkId+" at time "+time+" is null. Routes are \n"+nRoutesAndLinkIds.toString());
		}
		return nextLinkInRoute;
	}

	public double getTotalDelay() {
		return totalDelay;
	}
	
	/**
	 * @return delays which are not internalized because causing agent could not be located or other rounding errors.
	 */
	public double getRoundingDelays(){
		return this.roundingErrors;
	}

	public void writeCongestionStats(String fileName) {
		BufferedWriter bw = IOUtils.getBufferedWriter(fileName);
		
		try {
			bw.write("Total delay [hours];"+this.totalDelay / 3600);
			bw.newLine();
			bw.write("Total internalied delay [hours];"+ (this.totalDelay - this.roundingErrors) / 3600);
			bw.newLine();
			bw.write("Not internalied delay (rounding errors) [hours];"+this.roundingErrors / 3600);
			bw.close();
		} catch (Exception e) {
			throw new RuntimeException("Data is not written in file. Reason: "+ e);
		}
		log.info("Congestion statistics written to " + fileName);	
	}
}
