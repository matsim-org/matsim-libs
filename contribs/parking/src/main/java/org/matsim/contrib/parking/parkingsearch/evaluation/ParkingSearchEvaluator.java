/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

/**
 * 
 */
package org.matsim.contrib.parking.parkingsearch.evaluation;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.TeleportationArrivalEvent;
import org.matsim.core.api.experimental.events.handler.TeleportationArrivalEventHandler;
import org.matsim.core.utils.io.IOUtils;

import com.google.common.math.DoubleMath;

/**
 * @author  jbischoff
 *
 */
/**
 *
 */
public class ParkingSearchEvaluator implements TeleportationArrivalEventHandler, PersonDepartureEventHandler, PersonArrivalEventHandler {

	Map<Id<Person>,Double> departureTimes = new HashMap<>();
	Map<Id<Person>,Double> walkDistance = new HashMap<>();
	Map<Id<Link>,List<Double>> distances = new HashMap<>();
	Map<Id<Link>,List<Double>> times = new HashMap<>();
	
	/* (non-Javadoc)
	 * @see org.matsim.core.events.handler.EventHandler#reset(int)
	 */
	@Override
	public void reset(int iteration) {
		this.departureTimes.clear();;
		this.walkDistance.clear();
		this.distances.clear();
		this.times.clear();
	}

	/* (non-Javadoc)
	 * @see org.matsim.core.api.experimental.events.handler.TeleportationArrivalEventHandler#handleEvent(org.matsim.core.api.experimental.events.TeleportationArrivalEvent)
	 */
	@Override
	public void handleEvent(TeleportationArrivalEvent event) {
		if (departureTimes.containsKey(event.getPersonId())){
			this.walkDistance.put(event.getPersonId(), event.getDistance());
		}
		
	}

	/* (non-Javadoc)
	 * @see org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler#handleEvent(org.matsim.api.core.v01.events.PersonArrivalEvent)
	 */
	@Override
	public void handleEvent(PersonArrivalEvent event) {
		if (departureTimes.containsKey(event.getPersonId())&&walkDistance.containsKey(event.getPersonId())){
			double walkD = walkDistance.remove(event.getPersonId());
			double walkT = event.getTime() - departureTimes.remove(event.getPersonId());
			Id<Link> linkId = event.getLinkId();
			if (!this.distances.containsKey(linkId)){
				this.distances.put(linkId, new ArrayList<Double>());
				this.times.put(linkId, new ArrayList<Double>());
			}
			this.distances.get(linkId).add(walkD);
			this.times.get(linkId).add(walkT);
		}
		
	}

	/* (non-Javadoc)
	 * @see org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler#handleEvent(org.matsim.api.core.v01.events.PersonDepartureEvent)
	 */
	@Override
	public void handleEvent(PersonDepartureEvent event) {
		if (event.getLegMode().equals(TransportMode.non_network_walk )){
			this.departureTimes.put(event.getPersonId(), event.getTime());
		}	
	}
	
	public void writeEgressWalkStatistics(String folder){
		String distanceFile = folder+"/egressWalkDistances.csv";
		String timesFile = folder+"/egressWalkTimes.csv";
		writeStats(this.distances,distanceFile);
		writeStats(this.times,timesFile);
		
	}

	/**
	 * @param distances2
	 * @param distanceFile
	 */	
	private void writeStats(Map<Id<Link>, List<Double>> map, String distanceFile) {
		BufferedWriter bw = IOUtils.getBufferedWriter(distanceFile);
		
		try {
			bw.write("LinkId;Average;Min;Max;Arrivals");
			for (Entry<Id<Link>, List<Double>> e : map.entrySet()){
				bw.newLine();
				bw.write(e.getKey()+";"+DoubleMath.mean(e.getValue())+";"+Collections.min(e.getValue())+";"+Collections.max(e.getValue())+";"+e.getValue().size());
				
			}
			bw.flush();
			bw.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	
	}

}
