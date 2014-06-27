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
package org.matsim.contrib.parking.PC2.analysis;

import java.util.HashMap;
import java.util.LinkedList;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.apache.log4j.Logger;
import org.geotools.math.Statistics;
import org.jfree.data.statistics.MeanAndStandardDeviation;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.parking.PC2.infrastructure.Parking;
import org.matsim.contrib.parking.PC2.simulation.ParkingArrivalEvent;
import org.matsim.contrib.parking.PC2.simulation.ParkingDepartureEvent;
import org.matsim.contrib.parking.lib.DebugLib;
import org.matsim.contrib.parking.lib.GeneralLib;
import org.matsim.contrib.parking.lib.obj.list.Lists;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.events.handler.BasicEventHandler;

import com.sun.xml.bind.v2.runtime.unmarshaller.UnmarshallingContext.State;

public abstract class AverageWalkDistanceStats implements BasicEventHandler, ActivityStartEventHandler {

	private static final Logger log = Logger.getLogger(AverageWalkDistanceStats.class);

	private Network network;
	private HashMap<Id, Parking> parking;

	private HashMap<Id, Coord> agentArrivalLocation;
	private HashMap<Id, String> agentArrivalParkingGroup;
	private HashMap<String, LinkedList<Double>> walkDistances;

	@Override
	public void reset(int iteration) {
		agentArrivalLocation = new HashMap<Id, Coord>();
		agentArrivalParkingGroup = new HashMap<Id, String>();
		walkDistances = new HashMap<String, LinkedList<Double>>();
	}

	public AverageWalkDistanceStats(Network network, HashMap<Id, Parking> parking) {
		this.network = network;
		this.parking = parking;
	}

	@Override
	public void handleEvent(Event event) {
		if (event.getEventType().equalsIgnoreCase(ParkingArrivalEvent.EVENT_TYPE)
				|| event.getEventType().equalsIgnoreCase(ParkingDepartureEvent.EVENT_TYPE)) {
			String personIdString = event.getAttributes().get(ParkingArrivalEvent.ATTRIBUTE_PERSON_ID);
			if (personIdString != null) {
				Id personId = new IdImpl(personIdString);
				Id parkingId = new IdImpl(event.getAttributes().get(ParkingArrivalEvent.ATTRIBUTE_PARKING_ID));

				if (event.getEventType().equalsIgnoreCase(ParkingArrivalEvent.EVENT_TYPE)) {
					agentArrivalLocation.put(personId, parking.get(parkingId).getCoordinate());
					agentArrivalParkingGroup.put(personId, getGroupName(parkingId));
				} else if (event.getEventType().equalsIgnoreCase(ParkingDepartureEvent.EVENT_TYPE)) {
					emptyArrivalVariables(personId);
				}
			}
		}
	}

	private void emptyArrivalVariables(Id personId) {
		agentArrivalLocation.remove(personId);
		agentArrivalParkingGroup.remove(personId);
	}

	@Override
	public void handleEvent(ActivityStartEvent event) {
		if (agentArrivalLocation.containsKey(event.getPersonId())) {
			double walkDistance = GeneralLib.getDistance(agentArrivalLocation.get(event.getPersonId()),
					network.getLinks().get(event.getLinkId()).getCoord());
			String groupName = agentArrivalParkingGroup.get(event.getPersonId());
			
			if (!walkDistances.containsKey(groupName)){
				walkDistances.put(groupName, new LinkedList<Double>());
			}
			
			walkDistances.get(groupName).add(walkDistance);

			emptyArrivalVariables(event.getPersonId());
		}
	}

	public void printStatistics() {
		for (String groupName : walkDistances.keySet()) {
			DescriptiveStatistics dd = new DescriptiveStatistics(Lists.getArray(walkDistances.get(groupName)));
			long mean = Math.round(dd.getMean());
			long stdev = Math.round(dd.getStandardDeviation());
			log.info("groupName: " + groupName + "; mean: " + mean + "[m]; standardDeviation: " + stdev + "[m]");
		}
	}

	public abstract String getGroupName(Id parkingId);

}
