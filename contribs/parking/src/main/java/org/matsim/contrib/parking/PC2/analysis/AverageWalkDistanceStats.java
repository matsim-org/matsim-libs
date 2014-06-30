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
import java.util.HashSet;
import java.util.LinkedList;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.apache.log4j.Logger;
import org.geotools.math.Statistics;
import org.jfree.data.statistics.MeanAndStandardDeviation;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.parking.PC2.infrastructure.Parking;
import org.matsim.contrib.parking.PC2.simulation.ParkingArrivalEvent;
import org.matsim.contrib.parking.PC2.simulation.ParkingDepartureEvent;
import org.matsim.contrib.parking.lib.DebugLib;
import org.matsim.contrib.parking.lib.GeneralLib;
import org.matsim.contrib.parking.lib.obj.DoubleValueHashMap;
import org.matsim.contrib.parking.lib.obj.list.Lists;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.events.handler.BasicEventHandler;

import com.sun.xml.bind.v2.runtime.unmarshaller.UnmarshallingContext.State;

public abstract  class AverageWalkDistanceStats implements BasicEventHandler {

	private static final Logger log = Logger.getLogger(AverageWalkDistanceStats.class);

	private HashMap<Id, Parking> parking;

	private HashMap<String, LinkedList<Double>> walkDistances;

	@Override
	public void reset(int iteration) {
		walkDistances=new HashMap<String, LinkedList<Double>>();
	}

	public AverageWalkDistanceStats(HashMap<Id, Parking> parking) {
		this.parking = parking;
	}

	@Override
	public void handleEvent(Event event) {
		if (event.getEventType().equalsIgnoreCase(ParkingArrivalEvent.EVENT_TYPE)
				) {
			Id personId=ParkingArrivalEvent.getPersonId(event.getAttributes());
			if (personId != null) {
				Id parkingId = ParkingArrivalEvent.getParkingId(event.getAttributes());

				Coord destCoord = ParkingArrivalEvent.getDestCoord(event.getAttributes());
				
				double walkDistance = GeneralLib.getDistance(parking.get(parkingId).getCoordinate(),
						destCoord);
				
				if (!walkDistances.containsKey(getGroupName(parkingId))){
					walkDistances.put(getGroupName(parkingId), new LinkedList<Double>());
				}
				
				walkDistances.get(getGroupName(parkingId)).add(walkDistance);
				
			}
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
