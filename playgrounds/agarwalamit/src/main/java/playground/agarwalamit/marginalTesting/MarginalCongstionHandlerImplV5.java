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
package playground.agarwalamit.marginalTesting;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;

import playground.ikaddoura.internalizationCar.MarginalCongestionEvent;

/**
 * @author amit
 * Simplest version of congestion handler, if a person is delayed, it will charge everything to the person who just left before.
 */

public class MarginalCongstionHandlerImplV5 implements PersonDepartureEventHandler, LinkEnterEventHandler, LinkLeaveEventHandler {

	public static final Logger  log = Logger.getLogger(MarginalCongstionHandlerImplV5.class);

	private final EventsManager events;
	private final Scenario scenario;

	private final List<String> congestedModes = new ArrayList<String>();
	private final Map<Id<Link>, LinkCongestionInfoExtended> linkId2congestionInfo;
	private final Map<Id<Person>, String> personId2LegMode;

	/**
	 * @param events
	 * @param scenario must contain network and config
	 */
	public MarginalCongstionHandlerImplV5(EventsManager events, Scenario scenario) {
		this.events = events;
		this.scenario = scenario;

		congestedModes.addAll(this.scenario.getConfig().qsim().getMainModes());
		if (congestedModes.size()>1) throw new RuntimeException("Mixed traffic is not tested yet.");

		if (this.scenario.getConfig().scenario().isUseTransit()) {
			log.warn("Mixed traffic (simulated public transport) is not tested. Vehicles may have different effective cell sizes than 7.5 meters.");
		}

		linkId2congestionInfo = new HashMap<>();
		personId2LegMode = new HashMap<>();

		for(Link link : scenario.getNetwork().getLinks().values()){
			LinkCongestionInfoExtended linkInfo = new LinkCongestionInfoExtended();
			linkInfo.setLinkId(link.getId());
			linkId2congestionInfo.put(link.getId(), linkInfo);
		}
	}

	@Override
	public void reset(int iteration) {
		this.linkId2congestionInfo.clear();
		this.personId2LegMode.clear();
	}

	@Override
	public void handleEvent(PersonDepartureEvent event) {
		String travelMode = event.getLegMode();
		event.getLegMode();
		if(congestedModes.contains(travelMode)){
			LinkCongestionInfoExtended linkInfo = this.linkId2congestionInfo.get(event.getLinkId());
			linkInfo.getPersonId2freeSpeedLeaveTime().put(event.getPersonId(), event.getTime() + 1);
			linkInfo.getPersonId2linkEnterTime().put(event.getPersonId(), event.getTime());
		}
		this.personId2LegMode.put(event.getPersonId(), travelMode);
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		Id<Person> personId = Id.createPersonId(event.getVehicleId().toString());
		LinkCongestionInfoExtended linkInfo = this.linkId2congestionInfo.get(event.getLinkId());
		double minLinkTravelTime = getEarliestLinkExitTime(scenario.getNetwork().getLinks().get(event.getLinkId()), personId2LegMode.get(personId));
		linkInfo.getPersonId2freeSpeedLeaveTime().put(personId, event.getTime()+ minLinkTravelTime + 1.0);
		linkInfo.getPersonId2linkEnterTime().put(personId, event.getTime());
	}

	/**
	 * @param link to get link length and maximum allowed (legal) speed on link
	 * @param travelMode to get maximum speed of vehicle
	 * @return minimum travel time on above link depending on the allowed link speed and vehicle speed
	 */
	private double getEarliestLinkExitTime(Link link, String travelMode){
		if(!travelMode.equals(TransportMode.car)) throw new RuntimeException("Travel mode other than car is not implemented yet. Thus aborting ...");
		double linkLength = link.getLength();
		double maxFreeSpeed = Math.min(link.getFreespeed(), Double.POSITIVE_INFINITY);
		double minLinkTravelTime = Math.floor(linkLength / maxFreeSpeed );
		return minLinkTravelTime;
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
			Id<Person> causingAgent = Id.createPersonId(linkInfo.getLastLeavingAgent().toString());
			if (causingAgent==null) throw new RuntimeException("Delays are more than 0. and there is no causing agent."
					+ "this should not happen.");

			MarginalCongestionEvent congestionEvent = new MarginalCongestionEvent(linkLeaveTime, "Delay", causingAgent, 
					personId, delay, linkId, linkInfo.getPersonId2linkEnterTime().get(causingAgent));
			this.events.processEvent(congestionEvent);
		}
		linkInfo.setLastLeavingAgent(personId);
	}
}
