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
package playground.agarwalamit.qStartPosition;

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
import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.PersonStuckEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.basic.v01.IdImpl;

/**
 * @author amit
 */
public class QueuePositionCalculationHandler implements LinkLeaveEventHandler, LinkEnterEventHandler, PersonDepartureEventHandler, PersonStuckEventHandler {

	private final Logger logger = Logger.getLogger(QueuePositionCalculationHandler.class);
	Map<Id, Map<Id, PersonOnLinkInformation>> linkId2PersonId2LinkInfo = new TreeMap<Id, Map<Id,PersonOnLinkInformation>>();
	SortedMap<Id, Queue<Id>> linkId2PersonId2VehicleOnLink= new TreeMap<Id, Queue<Id>>();
	Map<Id, Queue<Id>> linkId2PersonId2VehicleInQueue= new TreeMap<Id, Queue<Id>>();
	Map<Id, Double> linkId2LinkAvailableSpace = new TreeMap<Id, Double>();

	List<String> personIdLinkIdLinkEnterTimeLinkLeaveTimeData = new ArrayList<String>();
	List<String> personIdLinkIdLinkEnterTimeLinkLeaveTimeQueuePositionData = new ArrayList<String>();

	Map<Id, String> personId2LegMode = new TreeMap<Id, String>();
	private Scenario scenario;
	private double time2=0;

	public QueuePositionCalculationHandler(Scenario scenario) {
		this.scenario = scenario;
		for (Link link : scenario.getNetwork().getLinks().values()) {
			linkId2PersonId2LinkInfo.put(link.getId(), new TreeMap<Id, PersonOnLinkInformation>());
			linkId2PersonId2VehicleOnLink.put(link.getId(), new LinkedList<Id>());
			linkId2PersonId2VehicleInQueue.put(link.getId(), new LinkedList<Id>());
			linkId2LinkAvailableSpace.put(link.getId(), link.getLength());
		}
	}

	@Override
	public void reset(int iteration) {
		linkId2PersonId2LinkInfo.clear();
		linkId2PersonId2VehicleInQueue.clear();
		linkId2PersonId2VehicleOnLink.clear();
		personId2LegMode.clear();
		personIdLinkIdLinkEnterTimeLinkLeaveTimeQueuePositionData.clear();
		personIdLinkIdLinkEnterTimeLinkLeaveTimeData.clear();
	}

	@Override
	public void handleEvent(PersonDepartureEvent event){
		personId2LegMode.put(event.getPersonId(), event.getLegMode());
		//		updateVehicleOnLinkAndAddToQueue(event.getTime());
		updateVehicleOnLinkAndFillToQueue(event.getTime());
	}

	@Override
	public void handleEvent(LinkEnterEvent event) { 
		Id personId= event.getPersonId();
		Link link = scenario.getNetwork().getLinks().get(event.getLinkId());
		if(linkId2PersonId2LinkInfo.get(event.getLinkId()).containsKey(personId)){
			throw new RuntimeException("Person is already on the link. Cannot happen.");
		}

		Map<Id, PersonOnLinkInformation> personId2LinkInfo = linkId2PersonId2LinkInfo.get(link.getId());
		PersonOnLinkInformation personOnLinkInfo = insertLinkEnterInfo(event, link);
		personId2LinkInfo.put(personId, personOnLinkInfo);

		if(linkId2PersonId2VehicleOnLink.get(event.getLinkId()).contains(personId)){
			throw new RuntimeException("Same person can not come on the link again until it leaves the link.");
		}

		Queue<Id> personId2Position = linkId2PersonId2VehicleOnLink.get(event.getLinkId());
		personId2Position.offer(personId);

		//		updateVehicleOnLinkAndAddToQueue(event.getTime());
		updateVehicleOnLinkAndFillToQueue(event.getTime());
	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {
		if(event.getLinkId().equals(new IdImpl("-1"))) {

		} else {

			Id linkId = event.getLinkId();
			Id personId = event.getPersonId();
			Map<Id, PersonOnLinkInformation> personId2LinkInfo = linkId2PersonId2LinkInfo.get(linkId);

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
			personIdLinkIdLinkEnterTimeLinkLeaveTimeData.add(dataToWriteInList);
			

			updateVehicleOnLinkAndFillToQueue(event.getTime());
			linkId2PersonId2VehicleOnLink.get(linkId).remove(personId);
			
			if(linkId2PersonId2VehicleInQueue.get(linkId).contains(personId)) {
				if(!((LinkedList<Id>)linkId2PersonId2VehicleInQueue.get(linkId)).get(0).equals(personId)) {
					// perhaps check if this is at the front of the queue (only for no passing case).
					//logger.warn("Leaving vehicle should be first in queue if it was entered on link first.");
				}

				String qDataToWriteInList = personId+"\t"+linkId+"\t"
						+personOnLinkInfo.getLinkEnterTime()+"\t"
						//						+personOnLinkInfo.getPositionInQ()+"\t"
						+personOnLinkInfo.getQueuingTime()+"\t"
						+personOnLinkInfo.getLinkLength()+"\t"
						+personOnLinkInfo.getLegMode()+"\t"
						+personOnLinkInfo.getLinkLeaveTime();
				personIdLinkIdLinkEnterTimeLinkLeaveTimeQueuePositionData.add(qDataToWriteInList);

				linkId2PersonId2VehicleInQueue.get(linkId).remove(personId);

				double availableSpaceSoFar=Double.valueOf(linkId2LinkAvailableSpace.get(linkId));
				double newAvailableSpace = availableSpaceSoFar+getCellSize(personOnLinkInfo.getLegMode());
				linkId2LinkAvailableSpace.put(linkId, Double.valueOf(newAvailableSpace));
			}
			//			updateVehicleOnLinkAndAddToQueue(event.getTime());

			linkId2PersonId2LinkInfo.get(linkId).remove(personId);
		}
	}

	@Override
	public void handleEvent(PersonStuckEvent event) {
		//		updateVehicleOnLinkAndFillToQueue(event.getTime());

	}
	private PersonOnLinkInformation insertLinkEnterInfo(LinkEnterEvent event, Link link){
		PersonOnLinkInformation personOnLinkInfo = new PersonOnLinkInformation();
		personOnLinkInfo.setLink(link);
		personOnLinkInfo.setLinkEnterTime(event.getTime());
		personOnLinkInfo.setLegMode(personId2LegMode.get(event.getPersonId()));
		return personOnLinkInfo;
	}

//	private PersonOnLinkInformation insertPersonDepartureInfo(PersonDepartureEvent event, Link link){
//		//		double derivedLinkEnterTime = event.getTime()+1-this.linkId2FreeSpeedLinkTravelTime.get(linkId);
//		PersonOnLinkInformation personOnLinkInfo = new PersonOnLinkInformation();
//		personOnLinkInfo.setLink(link);
//		personOnLinkInfo.setLegMode(event.getLegMode());
//		personOnLinkInfo.setAvailableLinkSpace(link.getLength());
//		double derivedLinkEnterTime= event.getTime()+1-personOnLinkInfo.getFreeSpeedLinkTravelTime();
//		personOnLinkInfo.setLinkEnterTime(derivedLinkEnterTime);
//		return personOnLinkInfo;
//	}

//	private void updateVehicleOnLinkAndAddToQueue(double currentTimeStep) {
//		for (Link link : scenario.getNetwork().getLinks().values()) {
//			Id linkId = link.getId();
//			for(Id personId : linkId2PersonId2VehicleOnLink.get(linkId)){
//				PersonOnLinkInformation personOnLinkInfo = linkId2PersonId2LinkInfo.get(linkId).get(personId);
//				personOnLinkInfo.checkIfVehicleWillGoInQ(currentTimeStep);
//				if (personOnLinkInfo.addVehicleInQ()){
//					Queue<Id> queue = linkId2PersonId2VehicleInQueue.get(linkId);
//					if(queue.contains(personId)) { 
//						// already in queue
//					} else {
//						queue.offer(personId);
//						int positionOfVehicleInQueue  = ((LinkedList<Id>) queue).indexOf(personId)+1;
//						personOnLinkInfo.setPositionInQ(positionOfVehicleInQueue);
//					}
//				} 
//			}
//		}
//	}

	public List<String> getPersonLinkEnterTimeVehiclePositionDataToWrite(){
		return personIdLinkIdLinkEnterTimeLinkLeaveTimeQueuePositionData;
	}

	public List<String> getPersonLinkEnterLeaveTimeDataToWrite(){
		return personIdLinkIdLinkEnterTimeLinkLeaveTimeData;
	}
	private void updateVehicleOnLinkAndFillToQueue(double currentTimeStep) {

		for (Link link : scenario.getNetwork().getLinks().values()) {
			Id linkId = link.getId();
			for(Id personId : linkId2PersonId2VehicleOnLink.get(linkId)){
				for(double time = time2;time<=currentTimeStep;time++){
					PersonOnLinkInformation personOnLinkInfo = linkId2PersonId2LinkInfo.get(linkId).get(personId);
					personOnLinkInfo.setAvailableLinkSpace(Double.valueOf(linkId2LinkAvailableSpace.get(linkId)));
					personOnLinkInfo.checkIfVehicleWillGoInQ(time);
					if (personOnLinkInfo.addVehicleInQ()){
						Queue<Id> queue = linkId2PersonId2VehicleInQueue.get(linkId);
						if(queue.contains(personId)) { 
							// already in queue
						} else {
							queue.offer(personId);
							/*
							 * If a person (20mps)  starts on link(1000m) at t=0, then will add to queue if time t=51 sec
							 * time-1 is actually physically correct time at which it will add to Q.
							 */
							personOnLinkInfo.setQueuingTime(time-1);
							double availableSpaceSoFar=Double.valueOf(linkId2LinkAvailableSpace.get(linkId));
							double newAvailableSpace = availableSpaceSoFar-getCellSize(personOnLinkInfo.getLegMode());
							linkId2LinkAvailableSpace.put(linkId, Double.valueOf(newAvailableSpace));
							int positionOfVehicleInQueue  = ((LinkedList<Id>) queue).indexOf(personId)+1;
							personOnLinkInfo.setPositionInQ(positionOfVehicleInQueue);
						}
					} 
				}
			}
		}
		time2=currentTimeStep;
	}

	private static double getCellSize(String travelMode){
		double vehicleSpeed =7.5;
		if(travelMode.equals("cars") || travelMode.equals("fast")) {
			vehicleSpeed= 7.5;
		} else if(travelMode.equals("motorbikes") || travelMode.equals("med")) {
			vehicleSpeed = 7.5/4;
		} else if(travelMode.equals("bicycles") || travelMode.equals("truck") ){
			vehicleSpeed= 7.5/4;
		}
		return vehicleSpeed;
	}


}
