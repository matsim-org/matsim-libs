/* *********************************************************************** *
 * project: org.matsim.*
 * VolumesAnalyzer.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package org.matsim.analysis.linkpaxvolumes;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.events.*;
import org.matsim.api.core.v01.events.handler.*;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.Vehicles;

import javax.inject.Inject;
import java.util.*;

/**
 * Counts the number of vehicles leaving a link and the number of passengers on those vehicles, aggregated into time
 * bins of a specified size.
 *
 * @author vsp-gleich
 * (adapted from VolumesAnalyzer by mrieser, inspired by ikaddoura DynamicLinkDemandEventHandler in matsim-analysis)
 */
public class LinkPaxVolumesAnalyzer implements LinkLeaveEventHandler, VehicleEntersTrafficEventHandler,
		VehicleLeavesTrafficEventHandler, PersonEntersVehicleEventHandler, PersonLeavesVehicleEventHandler {

	private final static Logger log = Logger.getLogger(LinkPaxVolumesAnalyzer.class);
	private final int timeBinSize;
	private final int maxTime; // TODO: maybe 24h is too restrictive with plans rather 3:00 - 27:00
	private final int maxSlotIndex;
	private final IdMap<Link, int[]> links;
	private final IdMap<Vehicle, VehicleData> vehiclesData;
	private final Vehicles vehicles;
	
	// for multi-modal / multi vehicle type support
	private final boolean observeNetworkModes;
//	private final boolean observePassengerModes; // TODO
	private final boolean observeVehicleTypes;
	private final IdMap<Vehicle, String> enRouteModes; // TODO: replace with tempVehicle
	private final IdMap<Link, Map<String, int[]>> linkVehicleVolumesPerNetworkMode;
	private final IdMap<Link, Map<String, int[]>> linkPaxVolumesPerNetworkMode;
//	private final IdMap<Link, Map<String, int[]>> linkPaxVolumesPerPassengerMode;
//	private final IdMap<Link, Map<String, int[]>> linkVehicleVolumesPerPassengerMode;
	private final IdMap<Link, Map<Id<VehicleType>, int[]>> linkVehicleVolumesPerVehicleType;
	private final IdMap<Link, Map<Id<VehicleType>, int[]>> linkPaxVolumesPerVehicleType;

	@Inject
	LinkPaxVolumesAnalyzer(Network network, Vehicles vehicles, EventsManager eventsManager /* TODO: add some config settings */) {
		this(3600, 24 * 3600 - 1, network, vehicles, true, true);
		eventsManager.addHandler(this);
	}

	public LinkPaxVolumesAnalyzer(final int timeBinSize, final int maxTime, final Network network,
								  final Vehicles vehicles, boolean observeNetworkModes, boolean observeVehicleTypes) {
		this.timeBinSize = timeBinSize;
		this.maxTime = maxTime;
		this.maxSlotIndex = (this.maxTime/this.timeBinSize) + 1;
		this.links = new IdMap<>(Link.class);
		this.vehiclesData = new IdMap<>(Vehicle.class);
		this.vehicles = vehicles;
		
		this.observeNetworkModes = observeNetworkModes;
		if (this.observeNetworkModes) {
			this.enRouteModes = new IdMap<>(Vehicle.class);
			this.linkVehicleVolumesPerNetworkMode = new IdMap<>(Link.class);
			this.linkPaxVolumesPerNetworkMode = new IdMap<>(Link.class);
		} else {
			this.enRouteModes = null;
			this.linkVehicleVolumesPerNetworkMode = null;
			this.linkPaxVolumesPerNetworkMode = null;
		}
		this.observeVehicleTypes = observeVehicleTypes;
		if (this.observeVehicleTypes) {
			this.linkVehicleVolumesPerVehicleType = new IdMap<>(Link.class);
			this.linkPaxVolumesPerVehicleType = new IdMap<>(Link.class);
		} else {
			this.linkVehicleVolumesPerVehicleType = null;
			this.linkPaxVolumesPerVehicleType = null;
		}
	}
	
	@Override
	public void handleEvent(VehicleEntersTrafficEvent event) {
		if (this.observeNetworkModes || this.observeVehicleTypes) {
			Vehicle vehicle = vehicles.getVehicles().get(event.getVehicleId());
			VehicleData vehicleData = new VehicleData(event.getVehicleId(),
					vehicle == null ? null : vehicle.getType().getId(),
					event.getPersonId(),
					event.getNetworkMode());
			this.vehiclesData.put(event.getVehicleId(), vehicleData);
		}
	}
	
	@Override
	public void handleEvent(final LinkLeaveEvent event) {
		int[] volumes = this.links.get(event.getLinkId());
		if (volumes == null) {
			volumes = new int[this.maxSlotIndex + 1]; // initialized to 0 by default, according to JVM specs
			this.links.put(event.getLinkId(), volumes);
		}
		int timeslot = getTimeSlotIndex(event.getTime());
		volumes[timeslot]++;
		
		if (this.observeNetworkModes) {
			Map<String, int[]> modeVolumes = this.linkVehicleVolumesPerNetworkMode.get(event.getLinkId());
			if (modeVolumes == null) {
				modeVolumes = new HashMap<>();
				this.linkVehicleVolumesPerNetworkMode.put(event.getLinkId(), modeVolumes);
			}
			String mode = this.enRouteModes.get(event.getVehicleId());
			volumes = modeVolumes.get(mode);
			if (volumes == null) {
				volumes = new int[this.maxSlotIndex + 1]; // initialized to 0 by default, according to JVM specs
				modeVolumes.put(mode, volumes);
			}
			volumes[timeslot]++;
		}
	}

	private int getTimeSlotIndex(final double time) {
		if (time > this.maxTime) {
			return this.maxSlotIndex;
		}
		return ((int)time / this.timeBinSize);
	}

	/**
	 * @param linkId
	 * @return Array containing the number of vehicles leaving the link <code>linkId</code> per time bin,
	 * 		starting with time bin 0 from 0 seconds to (timeBinSize-1)seconds.
	 */
	public int[] getVolumesForLink(final Id<Link> linkId) {
		return this.links.get(linkId);
	}
	
	/**
	 * @param linkId
	 * @param mode
	 * @return Array containing the number of vehicles using the specified mode leaving the link 
	 *  	<code>linkId</code> per time bin, starting with time bin 0 from 0 seconds to (timeBinSize-1)seconds.
	 */
	public int[] getVolumesForLink(final Id<Link> linkId, String mode) {
		if (observeNetworkModes) {
			Map<String, int[]> modeVolumes = this.linkVehicleVolumesPerNetworkMode.get(linkId);
			if (modeVolumes != null) return modeVolumes.get(mode);
		} 
		return null;
	}

	/**
	 *
	 * @return The size of the arrays returned by calls to the {@link #getVolumesForLink(Id)} and the {@link #getVolumesForLink(Id, String)}
	 * methods.
	 */
	public int getVolumesArraySize() {
		return this.maxSlotIndex + 1;
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
	public double[] getVolumesPerHourForLink(final Id<Link> linkId) {
		if (3600.0 % this.timeBinSize != 0) log.error("Volumes per hour and per link probably not correct!");
		
		double[] volumes = new double[24];
		
		int[] volumesForLink = this.getVolumesForLink(linkId);
		if (volumesForLink == null) return volumes;

		int slotsPerHour = (int)(3600.0 / this.timeBinSize);
		for (int hour = 0; hour < 24; hour++) {
			double time = hour * 3600.0;
			for (int i = 0; i < slotsPerHour; i++) {
				volumes[hour] += volumesForLink[this.getTimeSlotIndex(time)];
				time += this.timeBinSize;
			}
		}
		return volumes;
	}

	public double[] getVolumesPerHourForLink(final Id<Link> linkId, String mode) {
		if (observeNetworkModes) {
			if (3600.0 % this.timeBinSize != 0) log.error("Volumes per hour and per link probably not correct!");
			
			double [] volumes = new double[24];
			for (int hour = 0; hour < 24; hour++) {
				volumes[hour] = 0.0;
			}
			
			int[] volumesForLink = this.getVolumesForLink(linkId, mode);
			if (volumesForLink == null) return volumes;
	
			int slotsPerHour = (int)(3600.0 / this.timeBinSize);
			for (int hour = 0; hour < 24; hour++) {
				double time = hour * 3600.0;
				for (int i = 0; i < slotsPerHour; i++) {
					volumes[hour] += volumesForLink[this.getTimeSlotIndex(time)];
					time += this.timeBinSize;
				}
			}
			return volumes;
		}
		return null;
	}
	
	/**
	 * @return Set of Strings containing all modes for which counting-values are available.
	 */
	public Set<String> getModes() {
		Set<String> modes = new TreeSet<>();
		
		for (Map<String, int[]> map : this.linkVehicleVolumesPerNetworkMode.values()) {
			modes.addAll(map.keySet());
		}
		
		return modes;
	}
	
	/**
	 * @return Set of Strings containing all link ids for which counting-values are available.
	 */
	public Set<Id<Link>> getLinkIds() {
		return this.links.keySet();
	}

	@Override
	public void reset(final int iteration) {
		this.links.clear();
		if (observeNetworkModes) {
			this.linkVehicleVolumesPerNetworkMode.clear();
			this.enRouteModes.clear();
		}
	}

	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {

	}

	@Override
	public void handleEvent(PersonLeavesVehicleEvent event) {

	}

	@Override
	public void handleEvent(VehicleLeavesTrafficEvent event) {

	}

	/**
	 * temporary data structure used during events processing
	 */
	private static class VehicleData {
		private final Id<Vehicle> vehicleId;
		private final Id<VehicleType> vehicleTypeId;
		private final Id<Person> driverId;
		private final String transportMode;
		private int currentPax = 0;

		private VehicleData(Id<Vehicle> vehicleId, Id<VehicleType> vehicleTypeId, Id<Person> driverId,
							String transportMode) {
			this.vehicleId = vehicleId;
			this.vehicleTypeId = vehicleTypeId;
			this.driverId = driverId;
			this.transportMode = transportMode;
		}
	}
}
