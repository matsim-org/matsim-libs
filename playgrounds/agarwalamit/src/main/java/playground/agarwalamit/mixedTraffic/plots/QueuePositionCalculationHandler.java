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

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashMap;
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
import org.matsim.core.events.algorithms.Vehicle2DriverEventHandler;
import org.matsim.core.utils.io.IOUtils;

import playground.agarwalamit.mixedTraffic.MixedTrafficVehiclesUtils;
import playground.agarwalamit.mixedTraffic.plots.LinkPersonInfoContainer.PersonPositionChecker;

/**
 * @author amit
 */
public class QueuePositionCalculationHandler implements LinkLeaveEventHandler, LinkEnterEventHandler, PersonDepartureEventHandler {

	private static final Logger LOG = Logger.getLogger(QueuePositionCalculationHandler.class);
	private final Map<Id<Link>,LinkPersonInfoContainer> linkid2Container = new HashMap<>();
	private Vehicle2DriverEventHandler delegate = new Vehicle2DriverEventHandler();
	private BufferedWriter writer1 ;
	private BufferedWriter writer2 ;

	private final Map<Id<Person>, String> personId2LegMode = new TreeMap<Id<Person>, String>();
	private final Scenario scenario;
	private double lastEventTimeStep = 0;

	public QueuePositionCalculationHandler(final Scenario scenario) {
		LOG.info("Calculating queue position of vehicles in mixed traffic.");
		this.scenario = scenario;
		for (Link link : scenario.getNetwork().getLinks().values()) {
			this.linkid2Container.put(link.getId(), new LinkPersonInfoContainer(link.getId(),link.getLength()));
		}
	}

	@Override
	public void reset(int iteration) {
		this.linkid2Container.clear();
		this.personId2LegMode.clear();
		this.delegate.reset(iteration);
	}

	@Override
	public void handleEvent(PersonDepartureEvent event){
		this.personId2LegMode.put(event.getPersonId(), event.getLegMode());
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		Id<Person> personId= this.delegate.getDriverOfVehicle(event.getVehicleId());
		Id<Link> linkId = event.getLinkId();

		//store info
		LinkPersonInfoContainer container = this.linkid2Container.get(linkId);
		Link link = this.scenario.getNetwork().getLinks().get(linkId);
		EnteringPersonInfo.Builder builder = new EnteringPersonInfo.Builder();
		builder.setAgentId(personId);
		builder.setEnterTime(event.getTime());
		builder.setLegMode(this.personId2LegMode.get(personId));
		builder.setLink(link);
		PersonPositionChecker checker = container.getPerson2PersonPositionChecker().get(personId);

		if( checker!=null && checker.getLink().getId().equals(linkId) ){
			checker.updateCycleNumberOfPerson();
		} 
		container.getPerson2EnteringPersonInfo().put(personId, builder.build());

		Queue<Id<Person>> personId2AgentPositions = container.getAgentsOnLink();
		personId2AgentPositions.offer(personId);
	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {
		Id<Link> linkId = event.getLinkId();
		Id<Person> personId = this.delegate.getDriverOfVehicle(event.getVehicleId());

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

		writeString(writer1, getPersonEnterLeaveInfo(checker) );

		updateVehicleOnLinkAndFillToQueue(event.getTime());
		container.getAgentsOnLink().remove(personId);

		if(container.getAgentsInQueue().contains(personId)) {//write info for queuing
			double initialPos = Double.valueOf(checker.getLink().getId().toString())*checker.getLink().getLength();
			double vehicleSpeed =  MixedTrafficVehiclesUtils.getSpeed(checker.getEnteredPersonInfo().getLegMode());
			double qStartDistFromFNode = initialPos + (checker.getQueuingTime()- checker.getEnteredPersonInfo().getLinkEnterTime()) * vehicleSpeed;

			if((qStartDistFromFNode-initialPos) > checker.getLink().getLength()){
				qStartDistFromFNode=initialPos + checker.getLink().getLength();
			}

			writeString(writer2,personId+"\t"+	
					linkId+"\t"+
					checker.getEnteredPersonInfo().getLinkEnterTime()+"\t"+
					initialPos+"\t"+
					checker.getQueuingTime()+"\t"+
					qStartDistFromFNode +"\t"+
					checker.getEnteredPersonInfo().getLegMode()+"\n"
					);

			writeString(writer2,personId+"\t"+	
					linkId+"\t"+
					checker.getQueuingTime()+"\t"+
					qStartDistFromFNode+"\t"+
					checker.getLeftPersonInfo().getLinkLeaveTime()+"\t"+
					(1 + Double.valueOf(checker.getLink().getId().toString() ) )*checker.getLink().getLength() + "\t"+
					checker.getEnteredPersonInfo().getLegMode()+"\n"
					);

			container.getAgentsInQueue().remove(personId);
			double newAvailableSpace = container.getAvailableLinkSpace() + MixedTrafficVehiclesUtils.getCellSize(leavingPersonInfo.getLegMode());
			container.setAvailableLinkSpace(newAvailableSpace);

		} else {//write info for non queuing
			writeString(writer2, getPersonEnterLeaveInfo(checker) );
		}
		container.getPerson2PersonPositionChecker().remove(personId);
	}

	private String getPersonEnterLeaveInfo(PersonPositionChecker checker) {
		return 	checker.getPersonId()+"\t"+
				checker.getLink().getId()+"\t"+
				checker.getEnteredPersonInfo().getLinkEnterTime()+"\t"+
				Double.valueOf(checker.getLink().getId().toString())*checker.getLink().getLength()+"\t"+
				checker.getLeftPersonInfo().getLinkLeaveTime()+"\t"+
				(1+Double.valueOf(checker.getLink().getId().toString()))*checker.getLink().getLength()+"\t"+
				checker.getEnteredPersonInfo().getLegMode()+"\n";
	}

	private void writeString (BufferedWriter writer, String str){
		try {
			writer.write(str);
		} catch (Exception e) {
			throw new RuntimeException("Data is not written. Reason -"+ e);
		}
	}

	private void updateVehicleOnLinkAndFillToQueue(final double now) {
		for ( Id<Link> linkId : this.linkid2Container.keySet() ) {
			LinkPersonInfoContainer container = this.linkid2Container.get(linkId);

			for(Id<Person> personId :container.getAgentsOnLink()){
				PersonPositionChecker checker = container.getOrCreatePersonPositionChecker(personId);
				double personPositionUpdateTimeStep = Math.floor( Math.max( this.lastEventTimeStep, checker.getProbableQueuingTime()) );

				for(double time = personPositionUpdateTimeStep; time <= now && !checker.isPersonAlreadyQueued(); time++){
					if( checker.isAddingVehicleInQueue(time) ){
						Queue<Id<Person>> queue = container.getAgentsInQueue();
						queue.offer(personId);
						checker.updateQueuingTime();
						double newAvailableSpace = container.getAvailableLinkSpace() - MixedTrafficVehiclesUtils.getCellSize(checker.getEnteredPersonInfo().getLegMode());
						container.setAvailableLinkSpace(newAvailableSpace);
					} 
				}
			}
		}
		this.lastEventTimeStep=now;
	}

	public void openWriter(final String outputFolder){
		this.writer1 = IOUtils.getBufferedWriter(outputFolder+"rDataPersonLinkEnterLeave.txt");
		this.writer2 = IOUtils.getBufferedWriter(outputFolder+"rDataPersonInQueueData6.txt");
		try {
			this.writer1.write("personId \t linkId \t linkEnterTimeX1 \t initialPositionY1 \t linkLeaveTimeX2 \t endPositionY2 \t travelMode \n");
			this.writer2.write("personId \t linkId \t startTimeX1 \t initialPositionY1 \t endTimeX2 \t endPositionY2 \t travelMode \n");
		} catch (Exception e) {
			throw new RuntimeException("Data is not written. Reason -"+ e);
		}

	}

	public void closeWriter(){
		try {
			this.writer1.close();
			this.writer2.close();
		} catch (IOException e) {
			throw new RuntimeException("Data is not written. Reason -"+ e);
		}
	}
}