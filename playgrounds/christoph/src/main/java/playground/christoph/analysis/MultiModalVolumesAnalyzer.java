/* *********************************************************************** *
 * project: org.matsim.*
 * MultiModalVolumesAnalyzer.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package playground.christoph.analysis;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.LinkLeaveEvent;
import org.matsim.core.api.experimental.events.PersonEntersVehicleEvent;
import org.matsim.core.api.experimental.events.PersonLeavesVehicleEvent;
import org.matsim.core.api.experimental.events.handler.AgentDepartureEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkLeaveEventHandler;
import org.matsim.core.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.core.events.handler.PersonLeavesVehicleEventHandler;
import org.matsim.core.mobsim.qsim.qnetsimengine.PassengerQNetsimEngine;

/**
 * Counts the number of vehicles and agents using non-motorized modes leaving a link, 
 * aggregated into time bins of a specified size. Each mode is counted separately.
 *
 * @author cdobler
 */
public class MultiModalVolumesAnalyzer implements LinkLeaveEventHandler, AgentDepartureEventHandler,
		PersonEntersVehicleEventHandler, PersonLeavesVehicleEventHandler {

	private final static Logger log = Logger.getLogger(MultiModalVolumesAnalyzer.class);
	private final int timeBinSize;
	private final int maxTime;
	private final int maxSlotIndex;
	private final Map<Id, Map<String,int[]>> links;
	private final Map<Id, String> enRouteModes;
	private final Map<Id, Integer> agentsInVehicles;
	
	public MultiModalVolumesAnalyzer(final int timeBinSize, final int maxTime, final Network network) {
		this.timeBinSize = timeBinSize;
		this.maxTime = maxTime;
		this.maxSlotIndex = (this.maxTime/this.timeBinSize) + 1;
		this.links = new HashMap<Id, Map<String, int[]>>((int) (network.getLinks().size() * 1.1), 0.95f);
		this.enRouteModes = new HashMap<Id, String>();
		this.agentsInVehicles = new HashMap<Id, Integer>();
	}
	
	@Override
	public void handleEvent(AgentDepartureEvent event) {
		enRouteModes.put(event.getPersonId(), event.getLegMode());
	}

	@Override
	public void handleEvent(final LinkLeaveEvent event) {
		Map<String, int[]> modesVolumes = this.links.get(event.getLinkId());
		if (modesVolumes == null) {
			modesVolumes = new HashMap<String, int[]>();
			this.links.put(event.getLinkId(), modesVolumes);
		}
		String mode = enRouteModes.get(event.getPersonId());
		int[] volumes = modesVolumes.get(mode);
		if (volumes == null) {
			volumes = new int[this.maxSlotIndex + 1]; // initialized to 0 by default, according to JVM specs
			modesVolumes.put(mode, volumes);
		}
		int timeslot = getTimeSlotIndex(event.getTime());
		volumes[timeslot]++;
		
		boolean isCarTrip = mode.equals(TransportMode.car);
		if (isCarTrip) {
			Integer agentsInVehicle = agentsInVehicles.get(event.getVehicleId());
			int passengers = agentsInVehicle - 1;
			if (passengers < 0) throw new RuntimeException("Found negative passenger count for vehicle " +
					event.getVehicleId().toString() + " at time " + event.getTime() + "!");
			
			// get ride passenger mode
			mode = PassengerQNetsimEngine.PASSENGER_TRANSPORT_MODE;
			volumes = modesVolumes.get(mode);
			if (volumes == null) {
				volumes = new int[this.maxSlotIndex + 1]; // initialized to 0 by default, according to JVM specs
				modesVolumes.put(mode, volumes);
			}
			volumes[timeslot] = volumes[timeslot] + passengers;
		}
	}

	private int getTimeSlotIndex(final double time) {
		if (time > this.maxTime) {
			return this.maxSlotIndex;
		}
		return ((int)time / this.timeBinSize);
	}

	@Override
	public void handleEvent(PersonLeavesVehicleEvent event) {
		Integer agentsInVehicle = agentsInVehicles.get(event.getVehicleId());
		agentsInVehicles.put(event.getVehicleId(), agentsInVehicle - 1);
	}

	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		
		Integer agentsInVehicle = agentsInVehicles.get(event.getVehicleId());
		if (agentsInVehicle == null) {
			agentsInVehicle = 0;
		}
		agentsInVehicles.put(event.getVehicleId(), agentsInVehicle + 1);
	}
	
	/**
	 * @param linkId
	 * @return Map containing an array for each occuring mode. Each array containing 
	 * 		the number of vehicles leaving the link <code>linkId</code> per time bin,
	 * 		starting with time bin 0 from 0 seconds to (timeBinSize-1)seconds.
	 */
	public Map<String, int[]> getVolumesForLink(final Id linkId) {
		return this.links.get(linkId);
	}
	
	/*
	 * This procedure is only working if (hour % timeBinSize == 0)
	 * 
	 * Example: 15 minutes bins
	 *  ___________________
	 * |  0 | 1  | 2  | 3  |
	 * |____|____|____|____|
	 * 0   900 1800  2700 3600
		___________________
	 * | 	  hour 0	   |
	 * |___________________|
	 * 0   				  3600
	 * 
	 * hour 0 = bins 0,1,2,3
	 * hour 1 = bins 4,5,6,7
	 * ...
	 * 
	 * getTimeSlotIndex = (int)time / this.timeBinSize => jumps at 3600.0!
	 * Thus, starting time = (hour = 0) * 3600.0
	 */
	public Map<String, double[]> getVolumesPerHourForLink(final Id linkId) {
		if (3600.0 % this.timeBinSize != 0) log.error("Volumes per hour and per link probably not correct!");
		
		Map<String, double[]> modeVolumes = new TreeMap<String, double[]>();
			
		Map<String, int[]> linkVolumes = this.getVolumesForLink(linkId);
		if (linkVolumes == null) return modeVolumes;

		int hours = (int) (this.maxTime / 3600.0);
		int slotsPerHour = (int)(3600.0 / this.timeBinSize);
		
		Set<String> modes = linkVolumes.keySet();
		for (String mode : modes) {
			int[] vols = linkVolumes.get(mode);
			double [] volumes = new double[hours];
			for (int hour = 0; hour < hours; hour++) {
				volumes[hour] = 0.0;
				double time = hour * 3600.0;
				for (int i = 0; i < slotsPerHour; i++) {
					volumes[hour] += vols[this.getTimeSlotIndex(time)];
					time += this.timeBinSize;
				}
			}			
			modeVolumes.put(mode, volumes);
		}
		
		return modeVolumes;
	}

	public Set<String> getModes() {
		Set<String> modes = new TreeSet<String>();
		
		for (Map<String,int[]> map : this.links.values()) {
			modes.addAll(map.keySet());
		}
		
		return modes;
	}
	
	/**
	 * @return Set of Strings containing all link ids for which counting-values are available.
	 */
	public Set<Id> getLinkIds() {
		return this.links.keySet();
	}

	@Override
	public void reset(final int iteration) {
		this.links.clear();
		this.enRouteModes.clear();
	}

}
