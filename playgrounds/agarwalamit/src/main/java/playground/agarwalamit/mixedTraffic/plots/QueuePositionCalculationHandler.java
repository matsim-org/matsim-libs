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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.SortedMap;
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

/**
 * @author amit
 */
public class QueuePositionCalculationHandler implements LinkLeaveEventHandler, LinkEnterEventHandler, PersonDepartureEventHandler {

	private final Logger logger = Logger.getLogger(QueuePositionCalculationHandler.class);
	private Map<Id<Link>, Map<Id<Person>, PersonOnLinkInformation>> linkId2PersonId2LinkInfo = new TreeMap<Id<Link>, Map<Id<Person>,PersonOnLinkInformation>>();
	private SortedMap<Id<Link>, Queue<Id<Person>>> linkId2PersonId2VehicleOnLink= new TreeMap<Id<Link>, Queue<Id<Person>>>();
	private Map<Id<Link>, Queue<Id<Person>>> linkId2PersonId2VehicleInQueue= new TreeMap<Id<Link>, Queue<Id<Person>>>();
	private Map<Id<Link>, Double> linkId2LinkAvailableSpace = new TreeMap<Id<Link>, Double>();

	private List<String> personIdLinkIdLinkEnterTimeLinkLeaveTimeData = new ArrayList<String>();
	private List<String> personIdLinkIdLinkEnterTimeLinkLeaveTimeQueuePositionData = new ArrayList<String>();

	private Map<Id<Person>, String> personId2LegMode = new TreeMap<Id<Person>, String>();
	private Scenario scenario;
	private double lastEventTimeStep=0;

	public QueuePositionCalculationHandler(Scenario scenario) {
		this.logger.info("Calculating queue position of vehicles in mixed traffic.");
		this.scenario = scenario;
		for (Link link : scenario.getNetwork().getLinks().values()) {
			this.linkId2PersonId2LinkInfo.put(link.getId(), new TreeMap<Id<Person>, PersonOnLinkInformation>());
			this.linkId2PersonId2VehicleOnLink.put(link.getId(), new LinkedList<Id<Person>>());
			this.linkId2PersonId2VehicleInQueue.put(link.getId(), new LinkedList<Id<Person>>());
			this.linkId2LinkAvailableSpace.put(link.getId(), link.getLength());
		}
	}

	@Override
	public void reset(int iteration) {
		this.linkId2PersonId2LinkInfo.clear();
		this.linkId2PersonId2VehicleInQueue.clear();
		this.linkId2PersonId2VehicleOnLink.clear();
		this.personId2LegMode.clear();
		this.personIdLinkIdLinkEnterTimeLinkLeaveTimeQueuePositionData.clear();
		this.personIdLinkIdLinkEnterTimeLinkLeaveTimeData.clear();
	}

	@Override
	public void handleEvent(PersonDepartureEvent event){
		this.personId2LegMode.put(event.getPersonId(), event.getLegMode());
//		updateVehicleOnLinkAndFillToQueue(event.getTime());
	}

	@Override
	public void handleEvent(LinkEnterEvent event) { 
		Id<Person> personId= event.getDriverId();
		Link link = this.scenario.getNetwork().getLinks().get(event.getLinkId());
		if(this.linkId2PersonId2LinkInfo.get(event.getLinkId()).containsKey(personId)){
			throw new RuntimeException("Person is already on the link. Cannot happen.");
		}

		Map<Id<Person>, PersonOnLinkInformation> personId2LinkInfo = this.linkId2PersonId2LinkInfo.get(link.getId());
		PersonOnLinkInformation personOnLinkInfo = insertLinkEnterInfo(event, link);
		personId2LinkInfo.put(personId, personOnLinkInfo);

		if(this.linkId2PersonId2VehicleOnLink.get(event.getLinkId()).contains(personId)){
			throw new RuntimeException("Same person can not come on the link again until it leaves the link.");
		}
		Queue<Id<Person>> personId2Position = this.linkId2PersonId2VehicleOnLink.get(event.getLinkId());
		personId2Position.offer(personId);
		updateVehicleOnLinkAndFillToQueue(event.getTime());
	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {
		if(event.getLinkId().equals(Id.create("-1",Link.class))) {
		} else {
			Id<Link> linkId = event.getLinkId();
			Id<Person> personId = event.getDriverId();
			Map<Id<Person>, PersonOnLinkInformation> personId2LinkInfo = this.linkId2PersonId2LinkInfo.get(linkId);

			if (personId2LinkInfo == null) {
				throw new RuntimeException("Cannot happen.");
			}
			
			PersonOnLinkInformation personOnLinkInfo = personId2LinkInfo.get(personId);
			personOnLinkInfo.setLinkLeaveTime( event.getTime());

			String dataToWriteInList = personId+"\t"+linkId+"\t"
					+personOnLinkInfo.getLinkEnterTime()+"\t"
					+personOnLinkInfo.getLinkLeaveTime()+"\t"
					+personOnLinkInfo.getLinkLength()+"\t"
					+personOnLinkInfo.getLegMode(); 
			this.personIdLinkIdLinkEnterTimeLinkLeaveTimeData.add(dataToWriteInList);

			updateVehicleOnLinkAndFillToQueue(event.getTime());
			this.linkId2PersonId2VehicleOnLink.get(linkId).remove(personId);

			if(this.linkId2PersonId2VehicleInQueue.get(linkId).contains(personId)) {
//				if(!((LinkedList<Id>)linkId2PersonId2VehicleInQueue.get(linkId)).get(0).equals(personId)) {
//					// perhaps check if this is at the front of the queue (only for no passing case).
//					//logger.warn("Leaving vehicle should be first in queue if it was entered on link first.");
//				}
				String qDataToWriteInList = personId+"\t"+linkId+"\t"
						+personOnLinkInfo.getLinkEnterTime()+"\t"
						+personOnLinkInfo.getQueuingTime()+"\t"
						+personOnLinkInfo.getLinkLength()+"\t"
						+personOnLinkInfo.getLegMode()+"\t"
						+personOnLinkInfo.getLinkLeaveTime();
				this.personIdLinkIdLinkEnterTimeLinkLeaveTimeQueuePositionData.add(qDataToWriteInList);

				this.linkId2PersonId2VehicleInQueue.get(linkId).remove(personId);
				double availableSpaceSoFar = Double.valueOf(this.linkId2LinkAvailableSpace.get(linkId));
				double newAvailableSpace = availableSpaceSoFar+MixedTrafficVehiclesUtils.getCellSize(personOnLinkInfo.getLegMode());
				this.linkId2LinkAvailableSpace.put(linkId, Double.valueOf(newAvailableSpace));
			}
			this.linkId2PersonId2LinkInfo.get(linkId).remove(personId);
		}
	}
	
	private PersonOnLinkInformation insertLinkEnterInfo(LinkEnterEvent event, Link link){
		PersonOnLinkInformation personOnLinkInfo = new PersonOnLinkInformation();
		personOnLinkInfo.setLink(link);
		personOnLinkInfo.setLinkEnterTime(event.getTime());
		personOnLinkInfo.setLegMode(this.personId2LegMode.get(event.getDriverId()));
		return personOnLinkInfo;
	}

	public List<String> getPersonLinkEnterTimeVehiclePositionDataToWrite(){
		return this.personIdLinkIdLinkEnterTimeLinkLeaveTimeQueuePositionData;
	}

	public List<String> getPersonLinkEnterLeaveTimeDataToWrite(){
		return this.personIdLinkIdLinkEnterTimeLinkLeaveTimeData;
	}
	
	private void updateVehicleOnLinkAndFillToQueue(double currentTimeStep) {
		for (Link link : this.scenario.getNetwork().getLinks().values()) {
			Id<Link> linkId = link.getId();
			for(Id<Person> personId :this.linkId2PersonId2VehicleOnLink.get(linkId)){
				for(double time = this.lastEventTimeStep;time<=currentTimeStep;time++){
					PersonOnLinkInformation personOnLinkInfo = this.linkId2PersonId2LinkInfo.get(linkId).get(personId);
					personOnLinkInfo.setAvailableLinkSpace(Double.valueOf(this.linkId2LinkAvailableSpace.get(linkId)));
					personOnLinkInfo.checkIfVehicleWillGoInQ(time);
					if (personOnLinkInfo.addVehicleInQ()){
						Queue<Id<Person>> queue = this.linkId2PersonId2VehicleInQueue.get(linkId);
						if(queue.contains(personId)) { 
							// already in queue
						} else {
							queue.offer(personId);
							/*
							 * If a person (20mps)  starts on link(1000m) at t=0, then will add to queue if time t=51 sec
							 * time-1 is actually physically correct time at which it will add to Q.
							 */
							double availableSpaceSoFar=Double.valueOf(this.linkId2LinkAvailableSpace.get(linkId));
							personOnLinkInfo.setQueuingTime(availableSpaceSoFar);
//							personOnLinkInfo.setQueuingTime(time-1);
							double newAvailableSpace = availableSpaceSoFar-MixedTrafficVehiclesUtils.getCellSize(personOnLinkInfo.getLegMode());
							this.linkId2LinkAvailableSpace.put(linkId, Double.valueOf(newAvailableSpace));
						}
					} 
				}
			}
		}
		this.lastEventTimeStep=currentTimeStep;
	}

//	private double getCellSize(String travelMode){
//		double effCellSize =7.5;
//		if(travelMode.equals("cars") || travelMode.equals("fast")) {
//			effCellSize= 7.5;
//		} else if(travelMode.equals("motorbikes") || travelMode.equals("med")) {
//			effCellSize = 7.5/4;
//		} else if(travelMode.equals("bicycles") || travelMode.equals("truck") ){
//			effCellSize= 7.5/4;
//		}
//		return effCellSize;
//	}
}
