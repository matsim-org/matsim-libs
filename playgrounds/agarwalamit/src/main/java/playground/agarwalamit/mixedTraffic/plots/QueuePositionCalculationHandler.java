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
package playground.agarwalamit.mixedTraffic.plots;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;

import playground.agarwalamit.mixedTraffic.MixedTrafficVehiclesUtils;
import playground.agarwalamit.mixedTraffic.plots.LinkPersonInfoContainer.PersonPositionChecker;

/**
 * @author amit
 */
public class QueuePositionCalculationHandler implements LinkLeaveEventHandler, LinkEnterEventHandler, PersonDepartureEventHandler {

	private static final Logger LOG = Logger.getLogger(QueuePositionCalculationHandler.class);
	private final Map<Id<Link>,LinkPersonInfoContainer> linkid2Container = new HashMap<>();
	private final List<PersonPositionChecker> person2LinkEnterLeaveTimeData = new ArrayList<PersonPositionChecker>();
	private final List<PersonPositionChecker> personLinkEnterLeaveTimeQueuePositionData = new ArrayList<PersonPositionChecker>();

	private final Map<Id<Person>, String> personId2LegMode = new TreeMap<Id<Person>, String>();
	private final Scenario scenario;
	private double lastEventTimeStep = 0;

	public QueuePositionCalculationHandler(final Scenario scenario) {
		LOG.info("Calculating queue position of vehicles in mixed traffic.");
		this.scenario = scenario;
		for (Link link : scenario.getNetwork().getLinks().values()) {
			this.linkid2Container.put(link.getId(), new LinkPersonInfoContainer(link.getId()));
		}
	}

	@Override
	public void reset(int iteration) {
		this.linkid2Container.clear();
		this.personId2LegMode.clear();
		this.personLinkEnterLeaveTimeQueuePositionData.clear();
		this.person2LinkEnterLeaveTimeData.clear();
	}

	@Override
	public void handleEvent(PersonDepartureEvent event){
		this.personId2LegMode.put(event.getPersonId(), event.getLegMode());
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		Id<Person> personId= event.getDriverId();
		Id<Link> linkId = event.getLinkId();

		//store info
		LinkPersonInfoContainer container = this.linkid2Container.get(linkId);
		Link link = this.scenario.getNetwork().getLinks().get(linkId);
		EnteringPersonInfo.Builder builder = new EnteringPersonInfo.Builder();
		builder.setAgentId(personId);
		builder.setEnterTime(event.getTime());
		builder.setLegMode(this.personId2LegMode.get(personId));
		builder.setLink(link);
		container.getPerson2EnteringPersonInfo().put(personId, builder.build());

		Queue<Id<Person>> personId2AgentPositions = container.getAgentsOnLink();
		personId2AgentPositions.offer(personId);
	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {
		Id<Link> linkId = event.getLinkId();
		Id<Person> personId = event.getDriverId();

		LinkPersonInfoContainer container = this.linkid2Container.get(linkId);
		if( ! container.getPerson2EnteringPersonInfo().containsKey(personId) ) return; // if agent has departed on this link
		
		LeavingPersonInfo.Builder builder = new LeavingPersonInfo.Builder();
		builder.setAgentId(personId);
		builder.setLeaveTime(event.getTime());
		builder.setLegMode(this.personId2LegMode.get(personId));
		builder.setLinkId(linkId);
		LeavingPersonInfo leavingPersonInfo = builder.build();
		container.getPerson2LeavingPersonInfo().put(personId, leavingPersonInfo);
		
		PersonPositionChecker checker = container.getOrCreatePersonPositionChecker(personId);
		checker.updatePresonLeavingInfo();
		
		this.person2LinkEnterLeaveTimeData.add(checker);
		updateVehicleOnLinkAndFillToQueue(event.getTime());
		container.getAgentsOnLink().remove(personId);

		if(container.getAgentsInQueue().contains(personId)) {
			this.personLinkEnterLeaveTimeQueuePositionData.add(checker);

			container.getAgentsInQueue().remove(personId);
			double availableSpaceSoFar = checker.getAvailableLinkSpace();
			double newAvailableSpace = availableSpaceSoFar + MixedTrafficVehiclesUtils.getCellSize(leavingPersonInfo.getLegMode());
			checker.updateAvailableLinkSpace(newAvailableSpace);
		}
	}

	private void updateVehicleOnLinkAndFillToQueue(final double now) {
		for ( Id<Link> linkId : this.linkid2Container.keySet() ) {
			LinkPersonInfoContainer container = this.linkid2Container.get(linkId);
			
			for(Id<Person> personId :container.getAgentsOnLink()){
				PersonPositionChecker checker = container.getOrCreatePersonPositionChecker(personId);
				double presonPositionUpdateTimeStep = Math.max( this.lastEventTimeStep, checker.getQueuingTime());
				
				for(double time = presonPositionUpdateTimeStep; time <= now && !checker.isPersonAlreadyQueued(); time++){
					
					if( checker.isAddingVehicleInQueue(time) ){
						Queue<Id<Person>> queue = container.getAgentsInQueue();
						if(! queue.contains(personId) ) {
							queue.offer(personId);
							/*
							 * If a person (20mps)  starts on link(1000m) at t=0, then will add to queue if time t=51 sec
							 * time-1 is actually physically correct time at which it will add to Q.
							 */
							double availableSpaceSoFar = checker.getAvailableLinkSpace();
							double newAvailableSpace = availableSpaceSoFar - MixedTrafficVehiclesUtils.getCellSize(checker.getEnteredPersonInfo().getLegMode());
							checker.updateAvailableLinkSpace(newAvailableSpace);
						}  else throw new RuntimeException("Person is alreay in queue. Aborting ...");
						
					} 
				}
				
			}
			
		}
		this.lastEventTimeStep=now;
	}
	
	public List<PersonPositionChecker> getPersonLinkEnterTimeVehiclePositionDataToWrite(){
		return this.personLinkEnterLeaveTimeQueuePositionData;
	}

	public List<PersonPositionChecker> getPersonLinkEnterLeaveTimeDataToWrite(){
		return this.person2LinkEnterLeaveTimeData;
	}
}