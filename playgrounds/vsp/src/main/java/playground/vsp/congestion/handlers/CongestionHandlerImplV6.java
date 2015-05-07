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
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
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
import org.matsim.core.gbl.Gbl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.vehicles.VehicleUtils;

import playground.vsp.congestion.CongestionType;
import playground.vsp.congestion.LinkCongestionInfo;
import playground.vsp.congestion.events.CongestionEvent;

/**
 * @author amit
 * Another version of congestion handler, if a person is delayed, it will charge everything to the person who just left before if link is constrained by flow capacity else 
 * it will identify the bottleneck link (spill back causing link) and charge the person who just entered on that link.
 */

public class CongestionHandlerImplV6 implements PersonDepartureEventHandler, LinkEnterEventHandler, LinkLeaveEventHandler,
PersonStuckEventHandler, ActivityEndEventHandler
{

	public static final Logger  log = Logger.getLogger(CongestionHandlerImplV6.class);

	private final EventsManager events;
	private final Scenario scenario;

	private final List<String> congestedModes = new ArrayList<String>();
	private final Map<Id<Link>, LinkCongestionInfo> 	linkId2congestionInfo = new HashMap<>();
	private final Map<Id<Person>, String> personId2LegMode = new HashMap<>();
	private Map<Id<Person>, List<Tuple<String, Double>>> personId2ActType2ActEndTime = new HashMap<>();
	private double totalDelay = 0;
	private double roundingErrors =0;
	private double roundingErrorWarnCount =0;

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
	
	private void storeLinkInfo(){
		for(Link link : scenario.getNetwork().getLinks().values()){
			LinkCongestionInfo linkInfo = new LinkCongestionInfo();
			linkInfo.setLinkId(link.getId());
			double flowCapacity_CapPeriod = link.getCapacity() * this.scenario.getConfig().qsim().getFlowCapFactor();
			double marginalDelay_sec = ((1 / (flowCapacity_CapPeriod / this.scenario.getNetwork().getCapacityPeriod()) ) );
			linkInfo.setMarginalDelayPerLeavingVehicle(marginalDelay_sec);
			linkId2congestionInfo.put(link.getId(), linkInfo);
		}
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
		this.personId2ActType2ActEndTime.clear();

		storeLinkInfo();
	}
	
	@Override
	public void handleEvent(ActivityEndEvent event) {
		//require for the multiple next link in route of the same person
		if(personId2ActType2ActEndTime.containsKey(event.getPersonId())){
			List<Tuple<String, Double>> listSoFar = personId2ActType2ActEndTime.get(event.getPersonId());
			listSoFar.add(new Tuple<String, Double>(event.getActType(), event.getTime()));
		} else {
			List<Tuple<String, Double>> listNow = new ArrayList<Tuple<String,Double>>();
			listNow.add(new Tuple<String, Double>(event.getActType(), event.getTime()));
			personId2ActType2ActEndTime.put(event.getPersonId(), listNow);
		}
	}

	@Override
	public void handleEvent(PersonDepartureEvent event) {
		String travelMode = event.getLegMode();
		if(congestedModes.contains(travelMode)){
			LinkCongestionInfo linkInfo = this.linkId2congestionInfo.get(event.getLinkId());
			linkInfo.getPersonId2freeSpeedLeaveTime().put(event.getPersonId(), event.getTime() + 1);
			linkInfo.getPersonId2linkEnterTime().put(event.getPersonId(), event.getTime());
			linkInfo.setLastEnteredAgent(event.getPersonId());
		}
		this.personId2LegMode.put(event.getPersonId(), travelMode);
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		Id<Person> personId = Id.createPersonId(event.getVehicleId().toString());
		LinkCongestionInfo linkInfo = this.linkId2congestionInfo.get(event.getLinkId());
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

		LinkCongestionInfo linkInfo = this.linkId2congestionInfo.get(linkId);
		double freeSpeedLeaveTime = linkInfo.getPersonId2freeSpeedLeaveTime().get(personId);
		double delay = linkLeaveTime - freeSpeedLeaveTime ;

		if(delay > 0.){
			totalDelay += delay;
			processDelays(event,delay);
		}
		linkInfo.setLastLeavingAgent(personId);
		linkInfo.setLastLeaveTime(linkLeaveTime);
	}
	
	private void processDelays(LinkLeaveEvent event, final double delay){

		Id<Person> personId = Id.createPersonId(event.getVehicleId().toString());
		Id<Link> linkId	= event.getLinkId();
		double linkLeaveTime = event.getTime();
		
		LinkCongestionInfo linkInfo = this.linkId2congestionInfo.get(linkId);
		
		Id<Person> causingAgent;
		Id<Link> causingLink;
		String congestionType;
		
		if(linkInfo.isLinkFree(personId, linkLeaveTime)){
			causingLink = getNextLinkInRoute(personId, linkId, linkLeaveTime);
			causingAgent = linkId2congestionInfo.get(causingLink).getLastEnteredAgent(); 

			if (causingAgent==null) {
				if(delay==1){
					roundingErrors+=delay;
					if(this.roundingErrorWarnCount==0){
						log.warn("Delay is 1 sec for person "+personId+" but there is no causing agent. This can happen.");
						log.warn(Gbl.ONLYONCE);
						this.roundingErrorWarnCount++;
					}
					return;
				}else {
					throw new RuntimeException("Delay for person "+personId+" is "+ delay+" sec. But causing agent could not be located. This happened during event "+event.toString()+" Aborting...");
				}
			} 
			congestionType = CongestionType.SpillbackDelay.toString();

		} else {
			causingLink = linkId;
			causingAgent = Id.createPersonId(linkInfo.getLastLeavingAgent().toString());
			congestionType = CongestionType.FlowDelay.toString();
		}
		
		CongestionEvent congestionEvent = new CongestionEvent(linkLeaveTime, congestionType, causingAgent, 
				personId, delay, causingLink, linkId2congestionInfo.get(causingLink).getPersonId2linkEnterTime().get(causingAgent));
		this.events.processEvent(congestionEvent);
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

		List<Tuple<String, Double>> personActInfos = personId2ActType2ActEndTime.get(personId);
		int numberOfActEnded = personActInfos.size();

		String currentAct = personId2ActType2ActEndTime.get(personId).get(numberOfActEnded-1).getFirst();
		int noOfOccuranceOfCurrentAct = 0;

		SortedSet<Double> actEndTimes = new TreeSet<Double>();

		for(int i =0;i<numberOfActEnded;i++){ // last stored act is currentAct
			Tuple<String, Double> actInfo = personId2ActType2ActEndTime.get(personId).get(i);
			if(currentAct.equals(actInfo.getFirst())) {
				actEndTimes.add(actInfo.getSecond());
				noOfOccuranceOfCurrentAct++;
			}
		}

		List<Id<Link>> routeLinks = new ArrayList<Id<Link>>();

		int actIndex = 0;

		for(PlanElement pe :planElements){
			if(pe instanceof Activity && actIndex < noOfOccuranceOfCurrentAct){
				if(((Activity)pe).getType().equals(currentAct)) actIndex ++;	
			}

			if(pe instanceof Leg && actIndex == noOfOccuranceOfCurrentAct){
				//				The following is necessary where a person makes several trips with different modes and thus non car trips are not instance of NetworkRoute.
				if(!((Leg)pe).getMode().equals(TransportMode.car)) continue; 

				NetworkRoute nRoute = ((NetworkRoute)((Leg)pe).getRoute()); 
				routeLinks.add(nRoute.getStartLinkId());
				routeLinks.addAll(nRoute.getLinkIds());  
				routeLinks.add(nRoute.getEndLinkId());
				break;
			}
		}

		Id<Link> nextLinkInRoute =  Id.create("NA",Link.class);
		Iterator<Id<Link>> it = routeLinks.iterator();
		do{
			if(it.next().equals(linkId) && it.hasNext()){
				nextLinkInRoute = it.next();
				break;
			}
		} while(it.hasNext());

		if (nextLinkInRoute.equals(Id.create("NA",Link.class))){ 
			throw new RuntimeException("Next link in the route of person "+personId+" is not found. At time "+time+" person is on the link "+linkId+". Aborting ...");
		} else return nextLinkInRoute;

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
