/* *********************************************************************** *
 * project: org.matsim.*
 * PassengerVolumesAnalyzer.java
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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.analysis.VolumesAnalyzer;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.PersonLeavesVehicleEventHandler;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.mobsim.qsim.qnetsimengine.PassengerQNetsimEngine;

/**
 * Counts the number of vehicles and agents using non-motorized modes leaving a link, 
 * aggregated into time bins of a specified size. Each mode is counted separately.
 *
 * TODO: check whether this produces the same results as MultiModalVolumesAnalyzer does.
 *
 * @author cdobler
 */
public class PassengerVolumesAnalyzer extends VolumesAnalyzer implements
		PersonEntersVehicleEventHandler, PersonLeavesVehicleEventHandler {

	private final static Logger log = Logger.getLogger(PassengerVolumesAnalyzer.class);
	private final int timeBinSize;
	private final int maxTime;
	private final int maxSlotIndex;
	private final Map<Id, int[]> passengerVolumes;
	
	private final Set<Id> enRouteDrivers;
	private final Map<Id, Integer> agentsInVehicles;
		
	public PassengerVolumesAnalyzer(final int timeBinSize, final int maxTime, final Network network) {
		super(timeBinSize, maxTime, network, true);
		this.timeBinSize = timeBinSize;
		this.maxTime = maxTime;
		this.maxSlotIndex = (this.maxTime/this.timeBinSize) + 1;
		this.passengerVolumes = new HashMap<Id, int[]>((int) (network.getLinks().size() * 1.1), 0.95f);
		this.enRouteDrivers = new HashSet<Id>();
		this.agentsInVehicles = new HashMap<Id, Integer>();
	}
	
	@Override
	public void handleEvent(PersonDepartureEvent event) {
		super.handleEvent(event);
		if (event.getLegMode().equals(TransportMode.car)) {
			this.enRouteDrivers.add(event.getPersonId());
		} else this.enRouteDrivers.remove(event.getPersonId());
	}
	
	@Override
	public void handleEvent(final LinkLeaveEvent event) {
		super.handleEvent(event);
		boolean isCarTrip = this.enRouteDrivers.contains(event.getDriverId());
		if (isCarTrip) {
			Id linkId = event.getLinkId();
			Integer agentsInVehicle = agentsInVehicles.get(event.getVehicleId());
			int passengers = agentsInVehicle - 1;
			if (passengers < 0) throw new RuntimeException("Found negative passenger count for vehicle " +
					event.getVehicleId().toString() + " at time " + event.getTime() + "!");
			
			int[] volumes = passengerVolumes.get(linkId);
			if (volumes == null) {
				volumes = new int[this.maxSlotIndex + 1]; // initialized to 0 by default, according to JVM specs
				passengerVolumes.put(linkId, volumes);
			}
			int timeslot = getTimeSlotIndex(event.getTime());
			volumes[timeslot] = volumes[timeslot] + passengers;
		}
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
	
	// had to copy this from the super class since it is private there
	private int getTimeSlotIndex(final double time) {
		if (time > this.maxTime) {
			return this.maxSlotIndex;
		}
		return ((int)time / this.timeBinSize);
	}

	/*
	 * We have to overwrite this since the super implementation limits the outputs to 24 hours.
	 * The method of the super class is NOT called!
	 */
	@Override
	public double[] getVolumesPerHourForLink(final Id linkId, String mode) {
		
		if (3600.0 % this.timeBinSize != 0) log.error("Volumes per hour and per link probably not correct!");
		
		int[] volumesForLink;
		if (mode.equals(PassengerQNetsimEngine.PASSENGER_TRANSPORT_MODE)) {
			 volumesForLink = this.passengerVolumes.get(linkId);
		} else volumesForLink = this.getVolumesForLink(linkId, mode);
		
		int hours = (int) (this.maxTime / 3600.0);
		double [] volumes = new double[hours];  // initialized to 0 by default, according to JVM specs

		if (volumesForLink == null) return volumes;

		int slotsPerHour = (int)(3600.0 / this.timeBinSize);
		for (int hour = 0; hour < hours; hour++) {
			double time = hour * 3600.0;
			for (int i = 0; i < slotsPerHour; i++) {
				volumes[hour] += volumesForLink[this.getTimeSlotIndex(time)];
				time += this.timeBinSize;
			}
		}
		return volumes;
	}
	
	
	
	/**
	 * @return Set of Strings containing all modes for which counting-values are available.
	 */
	@Override
	public Set<String> getModes() {
		Set<String> modes = super.getModes();
		modes.add(PassengerQNetsimEngine.PASSENGER_TRANSPORT_MODE);
		return modes;
	}

	@Override
	public void reset(final int iteration) {
		this.passengerVolumes.clear();
		this.enRouteDrivers.clear();
	}
}