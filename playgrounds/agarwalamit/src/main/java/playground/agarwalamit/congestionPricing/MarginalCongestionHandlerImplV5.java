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
package playground.agarwalamit.congestionPricing;

import java.io.BufferedWriter;
import java.io.IOException;
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
import org.matsim.core.utils.io.IOUtils;
import org.matsim.vehicles.VehicleUtils;

import playground.vsp.congestion.LinkCongestionInfo;
import playground.vsp.congestion.events.CongestionEvent;

/**
 * @author amit
 * Simplest version of congestion handler, if a person is delayed, it will charge everything to the person who just left before.
 */

public final class MarginalCongestionHandlerImplV5 implements PersonDepartureEventHandler, LinkEnterEventHandler, LinkLeaveEventHandler {

	public static final Logger  log = Logger.getLogger(MarginalCongestionHandlerImplV5.class);

	private final EventsManager events;
	private final Scenario scenario;

	private final List<String> congestedModes = new ArrayList<String>();
	private final Map<Id<Link>, LinkCongestionInfo> linkId2congestionInfo = new HashMap<>();
	private final Map<Id<Person>, String> personId2LegMode = new HashMap<>();
	private double totalDelay = 0;
	private double roundingErrors =0;
	private double nonInternalizedDelay=0;

	/**
	 * @param events
	 * @param scenario must contain network and config
	 */
	public MarginalCongestionHandlerImplV5(EventsManager events, Scenario scenario) {
		this.events = events;
		this.scenario = scenario;

		congestedModes.addAll(this.scenario.getConfig().qsim().getMainModes());
		if (congestedModes.size()>1) throw new RuntimeException("Mixed traffic is not tested yet.");

		if (this.scenario.getConfig().transit().isUseTransit()) {
			log.warn("Mixed traffic (simulated public transport) is not tested. Vehicles may have different effective cell sizes than 7.5 meters.");
		}
		storeLinkInfo();
	}

	@Override
	public void reset(int iteration) {
		this.personId2LegMode.clear();
		this.linkId2congestionInfo.clear();

		storeLinkInfo();

	}

	private void storeLinkInfo(){
		for(Link link : scenario.getNetwork().getLinks().values()){
			LinkCongestionInfo linkInfo = new LinkCongestionInfo();
			linkInfo.setLinkId(link.getId());
			linkId2congestionInfo.put(link.getId(), linkInfo);
		}
	}

	@Override
	public void handleEvent(PersonDepartureEvent event) {
		String travelMode = event.getLegMode();
		event.getLegMode();
		if(congestedModes.contains(travelMode)){
			LinkCongestionInfo linkInfo = this.linkId2congestionInfo.get(event.getLinkId());
			linkInfo.getPersonId2freeSpeedLeaveTime().put(event.getPersonId(), event.getTime() + 1);
			linkInfo.getPersonId2linkEnterTime().put(event.getPersonId(), event.getTime());
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
			if (linkInfo.getLastLeaveEvent()==null){
				if(delay==1) {
					roundingErrors+=delay;
					log.error("Agent "+event.getPersonId()+" is leaving link "+event.getLinkId()+" at time "+event.getTime()+". Delay is 1 sec and no one left link before (no causing agent). \n"
							+ "Thus, possible reason is throwing of wait2Link and departure event at different time steps.");
					return;
				} else {
					nonInternalizedDelay+=delay;
					log.error("Agent "+event.getPersonId()+" is leaving link "+event.getLinkId()+" at time "+event.getTime()+". Delay is "+delay+ "sec but no causing agents. \n "
							+ "Possibly due to spill back delays.");
					//				throw new RuntimeException("Delays are more than 0. and there is no causing agent."
					//					+ "this should not happen.");
					return;
				}
			}
			Id<Person> causingAgent = Id.createPersonId(linkInfo.getLastLeaveEvent().toString());

			CongestionEvent congestionEvent = new CongestionEvent(linkLeaveTime, "Delay", causingAgent, 
					personId, delay, linkId, linkInfo.getPersonId2linkEnterTime().get(causingAgent));
			this.events.processEvent(congestionEvent);
			System.out.println(congestionEvent.toString());
		}
		linkInfo.memorizeLastLinkLeaveEvent(event);
	}

	/**
	 * @param link to get link length and maximum allowed (legal) speed on link
	 * @param travelMode to get maximum speed of vehicle
	 * @return minimum travel time on above link depending on the allowed link speed and vehicle speed
	 */
	private static double getEarliestLinkExitTime(Link link, String travelMode){
		if(!travelMode.equals(TransportMode.car)) throw new RuntimeException("Travel mode other than car is not implemented yet. Thus aborting ...");
		double linkLength = link.getLength(); // see org.matsim.core.mobsim.qsim.qnetsimengine.DefaultLinkSpeedCalculator.java
		//		Id<VehicleType> vehTyp = Id.create(travelMode,VehicleType.class);
		double vehSpeed = VehicleUtils.getDefaultVehicleType().getMaximumVelocity(); //VehicleUtils.createVehiclesContainer().getVehicleTypes().get(vehTyp).getMaximumVelocity();
		double maxFreeSpeed = Math.min(link.getFreespeed(), vehSpeed);
		double minLinkTravelTime = Math.floor(linkLength / maxFreeSpeed );
		return minLinkTravelTime;
	}

	public double getTotalDelay() {
		return totalDelay;
	}

	public double getNonInternalizedDelay() {
		return nonInternalizedDelay;
	}

	public double getRoundingErrors() {
		return roundingErrors;
	}

	public void writeCongestionStats(String fileName) {
		BufferedWriter bw = IOUtils.getBufferedWriter(fileName);
		try {
			bw.write("Total delay [hours];" + this.totalDelay / 3600.);
			bw.newLine();
			bw.write("Total internalized delay [hours];" + (this.totalDelay - this.roundingErrors - this.nonInternalizedDelay) / 3600.);
			bw.newLine();
			bw.write("Not internalized delay (rounding errors) [hours];" + this.roundingErrors / 3600.);
			bw.newLine();
			bw.write("Not internalied delay (No causing agent(s)) [hours];"+this.nonInternalizedDelay / 3600);
			bw.close();
		} catch (IOException e) {
			throw new RuntimeException("Data is not written to file. Reason "+e);
		}
		log.info("Congestion statistics written to " + fileName);	
	}
}
