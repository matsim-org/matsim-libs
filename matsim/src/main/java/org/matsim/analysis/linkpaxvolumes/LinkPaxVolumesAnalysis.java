/* *********************************************************************** *
 * project: org.matsim.*
 * LinkPaxVolumesAnalysis.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2021 by the members listed in the COPYING,        *
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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.IdSet;
import org.matsim.api.core.v01.events.*;
import org.matsim.api.core.v01.events.handler.*;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.Vehicles;

import java.util.*;

/**
 * Counts the number of vehicles _entering_ a link and the number of passengers on those vehicles, aggregated into time
 * bins of a specified size.
 * Traditionally other analysis code tended to count vehicles leaving a link, but in practice cars enter and leave the
 * network at the link's end and pt and drt passengers board and alight at the link's end, too. So it seems more precise
 * to count the travel distance starting from the link following the departure link and including the arrival link.
 * RouteUtils.calcDistance() solves this issue in a more precise way, but does not seem applicable here.
 *
 * On top this class counts the number of vehicles (i.e. vehicle ids) per vehicle type which operated on the network
 * and the time vehicles spent on the network.
 *
 * @author vsp-gleich
 * (adapted from VolumesAnalyzer by mrieser, inspired by ikaddoura DynamicLinkDemandEventHandler in matsim-analysis)
 */
public final class LinkPaxVolumesAnalysis implements LinkEnterEventHandler, VehicleEntersTrafficEventHandler,
		VehicleLeavesTrafficEventHandler, PersonDepartureEventHandler, PersonEntersVehicleEventHandler,
		PersonLeavesVehicleEventHandler {

	private final static Logger log = LogManager.getLogger(LinkPaxVolumesAnalysis.class);
	private final int timeBinSize;
	private final int maxTime; // TODO: maybe 24h is too restrictive with plans rather 3:00 - 27:00
	private final int maxSlotIndex;
	private final IdMap<Link, int[]> linkVehicleVolumes;
	private final IdMap<Link, int[]> linkPaxVolumes;
	private final IdMap<Vehicle, VehicleData> vehiclesData;
	private final Vehicles vehicles;
	private final Vehicles transitVehicles;
	private final Id<VehicleType> nullVehicleType = Id.create("nullVehicleType", VehicleType.class);

	// for multi-modal / multi vehicle type support
	final boolean observeNetworkModes;
	final boolean observePassengerModes;
	final boolean observeVehicleTypes;
	private final IdMap<Link, Map<String, int[]>> linkVehicleVolumesPerNetworkMode;
	private final IdMap<Link, Map<String, int[]>> linkPaxVolumesPerNetworkMode;
	private final IdMap<Link, Map<String, int[]>> linkVehicleVolumesPerPassengerMode;
	private final IdMap<Link, Map<String, int[]>> linkPaxVolumesPerPassengerMode;
	private final IdMap<Link, IdMap<VehicleType, int[]>> linkVehicleVolumesPerVehicleType;
	private final IdMap<Link, IdMap<VehicleType, int[]>> linkPaxVolumesPerVehicleType;
	private final IdMap<Person, String> person2passengerMode;
	private final IdSet<Vehicle> vehiclesAboutToLeave;
	private final IdMap<VehicleType, Double> vehicleType2timeOnNetwork;
	private final IdMap<VehicleType, Integer> vehicleType2numberSeen;
	private final IdSet<Vehicle> vehicleIdsSeen;

	public LinkPaxVolumesAnalysis(Vehicles vehicles, Vehicles transitVehicles /* TODO: add some config settings */) {
		this(3600, 30 * 3600 - 1, vehicles, transitVehicles, true, true, true);
	}

	LinkPaxVolumesAnalysis(final int timeBinSize, final int maxTime, final Vehicles vehicles,
								  final Vehicles transitVehicles, boolean observeNetworkModes, boolean observePassengerModes,
								  boolean observeVehicleTypes) {
		this.timeBinSize = timeBinSize;
		this.maxTime = maxTime;
		this.maxSlotIndex = (this.maxTime/this.timeBinSize) + 1;
		this.linkVehicleVolumes = new IdMap<>(Link.class);
		this.linkPaxVolumes = new IdMap<>(Link.class);
		this.vehiclesData = new IdMap<>(Vehicle.class);
		this.vehicles = vehicles;
		this.transitVehicles = transitVehicles;
		this.person2passengerMode = new IdMap<>(Person.class);
		this.vehiclesAboutToLeave = new IdSet<>(Vehicle.class);
		this.vehicleType2timeOnNetwork = new IdMap<>(VehicleType.class);
		this.vehicleType2numberSeen = new IdMap<>(VehicleType.class);
		this.vehicleIdsSeen = new IdSet<>(Vehicle.class);

		this.observeNetworkModes = observeNetworkModes;
		if (this.observeNetworkModes) {
			this.linkVehicleVolumesPerNetworkMode = new IdMap<>(Link.class);
			this.linkPaxVolumesPerNetworkMode = new IdMap<>(Link.class);
		} else {
			this.linkVehicleVolumesPerNetworkMode = null;
			this.linkPaxVolumesPerNetworkMode = null;
		}
		this.observePassengerModes = observePassengerModes;
		if (this.observePassengerModes) {
			this.linkVehicleVolumesPerPassengerMode = new IdMap<>(Link.class);
			this.linkPaxVolumesPerPassengerMode = new IdMap<>(Link.class);
		} else {
			this.linkVehicleVolumesPerPassengerMode = null;
			this.linkPaxVolumesPerPassengerMode = null;
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
	public void handleEvent(PersonDepartureEvent event) {
		this.person2passengerMode.put(event.getPersonId(), event.getLegMode());
	}

	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		VehicleData vehicleData = this.vehiclesData.get(event.getVehicleId());
		if (vehicleData == null) {
			Vehicle vehicle = vehicles.getVehicles().get(event.getVehicleId());
			if (vehicle == null) {
				// TODO: Why can the transit vehicles not be found in the general scenario.getVehicles() ?
				vehicle = transitVehicles.getVehicles().get(event.getVehicleId());
			}
			vehicleData = new VehicleData(event.getVehicleId(),
					vehicle == null ? nullVehicleType : vehicle.getType().getId(),
					event.getPersonId());
			this.vehiclesData.put(event.getVehicleId(), vehicleData);
		}
		String passengerMode = person2passengerMode.get(event.getPersonId());
		Map<String, Integer> passengersByMode = this.vehiclesData.get(event.getVehicleId()).passengerMode2currentPax;
		passengersByMode.put(passengerMode, passengersByMode.getOrDefault(passengerMode, 0) + 1);
	}

	@Override
	public void handleEvent(VehicleEntersTrafficEvent event) {
		VehicleData vehicleData = this.vehiclesData.get(event.getVehicleId());
		vehicleData.networkMode = event.getNetworkMode();
		vehicleData.enteredNetworkTime = event.getTime();

		// count number of vehicles
		if (!vehicleIdsSeen.contains(event.getVehicleId())) {
			vehicleIdsSeen.add(event.getVehicleId());
			this.vehicleType2numberSeen.put(vehicleData.vehicleTypeId,
					this.vehicleType2numberSeen.computeIfAbsent(vehicleData.vehicleTypeId, k -> 0) + 1);
		}
	}

	@Override
	public void handleEvent(final LinkEnterEvent event) {
		int[] vehicleVolumesAll = this.linkVehicleVolumes.get(event.getLinkId());
		if (vehicleVolumesAll == null) {
			vehicleVolumesAll = new int[this.maxSlotIndex + 1]; // initialized to 0 by default, according to JVM specs
			this.linkVehicleVolumes.put(event.getLinkId(), vehicleVolumesAll);
		}
		int timeslot = getTimeSlotIndex(event.getTime());
		vehicleVolumesAll[timeslot]++;

		VehicleData vehicleData = this.vehiclesData.get(event.getVehicleId());
		int currentPaxAllPassengerModes = 0;
		for (int i : vehicleData.passengerMode2currentPax.values()) {
			currentPaxAllPassengerModes += i;
		}

		int[] passengerVolumesAll = this.linkPaxVolumes.get(event.getLinkId());
		if (passengerVolumesAll == null) {
			passengerVolumesAll = new int[this.maxSlotIndex + 1]; // initialized to 0 by default, according to JVM specs
			this.linkPaxVolumes.put(event.getLinkId(), passengerVolumesAll);
		}
		passengerVolumesAll[timeslot] += currentPaxAllPassengerModes;

		if (this.observeNetworkModes) {
			String mode = vehicleData.networkMode;

			Map<String, int[]> modeVehicleVolumes = this.linkVehicleVolumesPerNetworkMode.
					computeIfAbsent(event.getLinkId(), k -> new HashMap<>());
			int[] vehicleVolumes = modeVehicleVolumes.get(mode);
			if (vehicleVolumes == null) {
				vehicleVolumes = new int[this.maxSlotIndex + 1]; // initialized to 0 by default, according to JVM specs
				modeVehicleVolumes.put(mode, vehicleVolumes);
			}
			vehicleVolumes[timeslot]++;

			Map<String, int[]> modePassengerVolumes = this.linkPaxVolumesPerNetworkMode.
					computeIfAbsent(event.getLinkId(), k -> new HashMap<>());
			int[] passengerVolumes = modePassengerVolumes.get(mode);
			if (passengerVolumes == null) {
				passengerVolumes = new int[this.maxSlotIndex + 1]; // initialized to 0 by default, according to JVM specs
				modePassengerVolumes.put(mode, passengerVolumes);
			}
			passengerVolumes[timeslot] += currentPaxAllPassengerModes;
		}

		if (this.observePassengerModes) {
			// There is no clear way how to count a vehicle which is used by passengers of different passenger modes
			// Here we simply count multiple times the same vehicle (once per passenger mode)
			for (Map.Entry<String, Integer> passengerMode2modePax: vehicleData.passengerMode2currentPax.entrySet()) {

				Map<String, int[]> modeVehicleVolumes = this.linkVehicleVolumesPerPassengerMode.
						computeIfAbsent(event.getLinkId(), k -> new HashMap<>());
				int[] vehicleVolumes = modeVehicleVolumes.get(passengerMode2modePax.getKey());
				if (vehicleVolumes == null) {
					vehicleVolumes = new int[this.maxSlotIndex + 1]; // initialized to 0 by default, according to JVM specs
					modeVehicleVolumes.put(passengerMode2modePax.getKey(), vehicleVolumes);
				}
				vehicleVolumes[timeslot]++;

				Map<String, int[]> modePassengerVolumes = this.linkPaxVolumesPerPassengerMode.
						computeIfAbsent(event.getLinkId(), k -> new HashMap<>());
				int[] passengerVolumes = modePassengerVolumes.get(passengerMode2modePax.getKey());
				if (passengerVolumes == null) {
					passengerVolumes = new int[this.maxSlotIndex + 1]; // initialized to 0 by default, according to JVM specs
					modePassengerVolumes.put(passengerMode2modePax.getKey(), passengerVolumes);
				}
				passengerVolumes[timeslot] += passengerMode2modePax.getValue();
			}
		}

		if (this.observeVehicleTypes) {
			Id<VehicleType> vehicleType = vehicleData.vehicleTypeId;

			IdMap<VehicleType, int[]> typeVehicleVolumes = this.linkVehicleVolumesPerVehicleType.
					computeIfAbsent(event.getLinkId(), k -> new IdMap<>(VehicleType.class));
			int[] vehicleVolumes = typeVehicleVolumes.get(vehicleType);
			if (vehicleVolumes == null) {
				vehicleVolumes = new int[this.maxSlotIndex + 1]; // initialized to 0 by default, according to JVM specs
				typeVehicleVolumes.put(vehicleType, vehicleVolumes);
			}
			vehicleVolumes[timeslot]++;

			IdMap<VehicleType, int[]> modePassengerVolumes = this.linkPaxVolumesPerVehicleType.
					computeIfAbsent(event.getLinkId(), k -> new IdMap<>(VehicleType.class));
			int[] passengerVolumes = modePassengerVolumes.get(vehicleType);
			if (passengerVolumes == null) {
				passengerVolumes = new int[this.maxSlotIndex + 1]; // initialized to 0 by default, according to JVM specs
				modePassengerVolumes.put(vehicleType, passengerVolumes);
			}
			passengerVolumes[timeslot] += currentPaxAllPassengerModes;
		}
	}

	@Override
	public void handleEvent(PersonLeavesVehicleEvent event) {
		String passengerMode = person2passengerMode.get(event.getPersonId());
		Map<String, Integer> passengersByMode = this.vehiclesData.get(event.getVehicleId()).passengerMode2currentPax;
		passengersByMode.put(passengerMode, passengersByMode.get(passengerMode) - 1);
		if (vehiclesAboutToLeave.contains(event.getVehicleId())) {
			// drt vehicles have a VehicleLeavesTrafficEvent at each stop, then alights the driver and after that
			// the remaining passengers
			// -> only remove vehicle when empty
			int totalPax = 0;
			for (int i: passengersByMode.values()) {
				totalPax += i;
			}
			if (totalPax == 0) {
				this.vehiclesData.remove(event.getVehicleId());
				vehiclesAboutToLeave.remove(event.getVehicleId());
			}
		}
	}

	@Override
	public void handleEvent(VehicleLeavesTrafficEvent event) {
		this.vehiclesAboutToLeave.add(event.getVehicleId());
		VehicleData vehicleData = this.vehiclesData.get(event.getVehicleId());
		this.vehicleType2timeOnNetwork.put(vehicleData.vehicleTypeId,
				this.vehicleType2timeOnNetwork.getOrDefault(vehicleData.vehicleTypeId, 0.0) +
						event.getTime() - vehicleData.enteredNetworkTime);
	}

	private int getTimeSlotIndex(final double time) {
		if (time > this.maxTime) {
			return this.maxSlotIndex;
		}
		return ((int)time / this.timeBinSize);
	}

	/**
	 * @param linkId
	 * @return Array containing the number of vehicles entering the link <code>linkId</code> per time bin,
	 * 		starting with time bin 0 from 0 seconds to (timeBinSize-1)seconds.
	 */
	int[] getVehicleVolumesForLink(final Id<Link> linkId) {
		return this.linkVehicleVolumes.get(linkId);
	}

	/**
	 * @param linkId
	 * @param networkMode
	 * @return Array containing the number of vehicles using the specified networkMode entering the link
	 *  	<code>linkId</code> per time bin, starting with time bin 0 from 0 seconds to (timeBinSize-1)seconds.
	 */
	int[] getVehicleVolumesForLinkPerNetworkMode(final Id<Link> linkId, String networkMode) {
		if (observeNetworkModes) {
			Map<String, int[]> modeVolumes = this.linkVehicleVolumesPerNetworkMode.get(linkId);
			if (modeVolumes != null) return modeVolumes.get(networkMode);
		}
		return null;
	}

	/**
	 * @param linkId
	 * @param passengerMode
	 * @return Array containing the number of vehicles having passengers of the specified passengerMode entering the link
	 *  	<code>linkId</code> per time bin, starting with time bin 0 from 0 seconds to (timeBinSize-1)seconds.
	 */
	int[] getVehicleVolumesForLinkPerPassengerMode(final Id<Link> linkId, String passengerMode) {
		if (observePassengerModes) {
			Map<String, int[]> modeVolumes = this.linkVehicleVolumesPerPassengerMode.get(linkId);
			if (modeVolumes != null) return modeVolumes.get(passengerMode);
		}
		return null;
	}

	/**
	 * @param linkId
	 * @param vehicleType
	 * @return Array containing the number of vehicles of the specified VehicleType entering the link
	 *  	<code>linkId</code> per time bin, starting with time bin 0 from 0 seconds to (timeBinSize-1)seconds.
	 */
	int[] getVehicleVolumesForLinkPerVehicleType(final Id<Link> linkId, Id<VehicleType> vehicleType) {
		if (observeVehicleTypes) {
			IdMap<VehicleType, int[]> typeVolumes = this.linkVehicleVolumesPerVehicleType.get(linkId);
			if (typeVolumes != null) return typeVolumes.get(vehicleType);
		}
		return null;
	}

	/**
	 * @param linkId
	 * @return Array containing the number of passengers entering the link <code>linkId</code> per time bin,
	 * 		starting with time bin 0 from 0 seconds to (timeBinSize-1)seconds.
	 */
	public int[] getPaxVolumesForLink(final Id<Link> linkId) {
		return this.linkPaxVolumes.get(linkId);
	}

	/**
	 * @param linkId
	 * @param networkMode
	 * @return Array containing the number of passengers on vehicles using the specified networkMode entering the link
	 *  	<code>linkId</code> per time bin, starting with time bin 0 from 0 seconds to (timeBinSize-1)seconds.
	 */
	int[] getPaxVolumesForLinkPerNetworkMode(final Id<Link> linkId, String networkMode) {
		if (observeNetworkModes) {
			Map<String, int[]> modeVolumes = this.linkPaxVolumesPerNetworkMode.get(linkId);
			if (modeVolumes != null) return modeVolumes.get(networkMode);
		}
		return null;
	}

	/**
	 * @param linkId
	 * @param passengerMode
	 * @return Array containing the number of passengers of the specified passengerMode entering the link
	 *  	<code>linkId</code> per time bin, starting with time bin 0 from 0 seconds to (timeBinSize-1)seconds.
	 */
	int[] getPaxVolumesForLinkPerPassengerMode(final Id<Link> linkId, String passengerMode) {
		if (observePassengerModes) {
			Map<String, int[]> modeVolumes = this.linkPaxVolumesPerPassengerMode.get(linkId);
			if (modeVolumes != null) return modeVolumes.get(passengerMode);
		}
		return null;
	}

	/**
	 * @param linkId
	 * @param vehicleType
	 * @return Array containing the number of passengers on vehicles of the specified VehicleType entering the link
	 *  	<code>linkId</code> per time bin, starting with time bin 0 from 0 seconds to (timeBinSize-1)seconds.
	 */
	int[] getPaxVolumesForLinkPerVehicleType(final Id<Link> linkId, Id<VehicleType> vehicleType) {
		if (observeVehicleTypes) {
			IdMap<VehicleType, int[]> typeVolumes = this.linkPaxVolumesPerVehicleType.get(linkId);
			if (typeVolumes != null) return typeVolumes.get(vehicleType);
		}
		return null;
	}

	/**
	 *
	 * @return The size of the arrays returned by calls to the {@link #getVehicleVolumesForLink(Id)} etc.
	 * methods.
	 */
	int getVolumesArraySize() {
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
	int[] getVolumePerHourFromTimeBinArray(int[] volumesForLink) {
		if (3600.0 % this.timeBinSize != 0) log.error("Volumes per hour and per link probably not correct!");

		int[] volumes = new int[30];
		if (volumesForLink == null) return volumes;

		int slotsPerHour = (int)(3600.0 / this.timeBinSize);
		for (int hour = 0; hour < 30; hour++) {
			double time = hour * 3600.0;
			for (int i = 0; i < slotsPerHour; i++) {
				volumes[hour] += volumesForLink[this.getTimeSlotIndex(time)];
				time += this.timeBinSize;
			}
		}
		return volumes;
	}

	int getVolumePerDayFromTimeBinArray(int[] volumesForLink) {
		int volumePerDay= 0;
		for (int i = 0; i < this.maxSlotIndex; i++) {
			volumePerDay += volumesForLink[i];
		}
		return volumePerDay;
	}

	Map<Id<VehicleType>, Integer> getVehicleType2numberSeen() {
		return Collections.unmodifiableMap(vehicleType2numberSeen);
	}

	Map<Id<VehicleType>, Double> getVehicleType2timeOnNetwork() {
		return Collections.unmodifiableMap(vehicleType2timeOnNetwork);
	}

	/**
	 * @return Set of Strings containing all modes for which counting-values are available.
	 */
	Set<String> getNetworkModes() {
		Set<String> modes = new TreeSet<>();

		for (Map<String, int[]> map : this.linkVehicleVolumesPerNetworkMode.values()) {
			modes.addAll(map.keySet());
		}

		return modes;
	}

	/**
	 * @return Set of Strings containing all modes for which counting-values are available.
	 */
	Set<String> getPassengerModes() {
		Set<String> modes = new TreeSet<>();

		for (Map<String, int[]> map : this.linkVehicleVolumesPerPassengerMode.values()) {
			modes.addAll(map.keySet());
		}

		return modes;
	}

	/**
	 * @return Set of Strings containing all modes for which counting-values are available.
	 */
	IdSet<VehicleType> getVehicleTypes() {
		IdSet<VehicleType> vehicleTypes = new IdSet<>(VehicleType.class);

		for (IdMap<VehicleType, int[]> map : this.linkVehicleVolumesPerVehicleType.values()) {
			vehicleTypes.addAll(map.keySet());
		}

		return vehicleTypes;
	}

	/**
	 * @return Set of Strings containing all link ids for which counting-values are available.
	 */
	Set<Id<Link>> getLinkIds() {
		return this.linkVehicleVolumes.keySet();
	}

	int getNumberOfHours() {
		return maxSlotIndex * timeBinSize / 3600;
	}

	@Override
	public void reset(final int iteration) {
		this.linkVehicleVolumes.clear();
		this.linkPaxVolumes.clear();
		this.person2passengerMode.clear();
		this.vehiclesAboutToLeave.clear();
		this.vehiclesData.clear();
		this.vehicleIdsSeen.clear();
		this.vehicleType2numberSeen.clear();
		this.vehicleType2timeOnNetwork.clear();

		if (observeNetworkModes) {
			this.linkVehicleVolumesPerNetworkMode.clear();
			this.linkPaxVolumesPerNetworkMode.clear();
		}
		if (this.observePassengerModes) {
			this.linkVehicleVolumesPerPassengerMode.clear();
			this.linkPaxVolumesPerPassengerMode.clear();
		}
		if (this.observeVehicleTypes) {
			this.linkVehicleVolumesPerVehicleType.clear();
			this.linkPaxVolumesPerVehicleType.clear();
		}
	}

	/**
	 * temporary data structure used during events processing
	 */
	private static class VehicleData {
		private final Id<Vehicle> vehicleId;
		private final Id<VehicleType> vehicleTypeId;
		private final Id<Person> driverId;
		private String networkMode;
		private Map<String, Integer> passengerMode2currentPax = new HashMap<>();
		private double enteredNetworkTime;

		public VehicleData(Id<Vehicle> vehicleId, Id<VehicleType> vehicleTypeId, Id<Person> driverId) {
			this.vehicleId = vehicleId;
			this.vehicleTypeId = vehicleTypeId;
			this.driverId = driverId;
		}
	}
}
