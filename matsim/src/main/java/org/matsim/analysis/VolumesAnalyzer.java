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

package org.matsim.analysis;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleEntersTrafficEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.vehicles.Vehicle;

import jakarta.inject.Inject;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Counts the number of vehicles leaving a link, aggregated into time bins of a specified size.
 *
 * @author mrieser
 */
public class VolumesAnalyzer implements LinkLeaveEventHandler, VehicleEntersTrafficEventHandler {

	private final static Logger log = LogManager.getLogger(VolumesAnalyzer.class);
	private final int timeBinSize;
	private final int maxTime;
	private final int maxSlotIndex;
	private final IdMap<Link, int[]> links;

	// for multi-modal support
	private final boolean observeModes;
	private final IdMap<Vehicle, String> enRouteModes;
	private final IdMap<Link, Map<String, int[]>> linksPerMode;

	@Inject
	VolumesAnalyzer(Network network, EventsManager eventsManager) {
		this(3600, 24 * 3600 - 1, network);
		eventsManager.addHandler(this);
	}

	public VolumesAnalyzer(final int timeBinSize, final int maxTime, final Network network) {
		this(timeBinSize, maxTime, network, true);
	}

	public VolumesAnalyzer(final int timeBinSize, final int maxTime, final Network network, boolean observeModes) {
		this.timeBinSize = timeBinSize;
		this.maxTime = maxTime;
		this.maxSlotIndex = (this.maxTime / this.timeBinSize) + 1;
		this.links = new IdMap<>(Link.class);

		this.observeModes = observeModes;
		if (this.observeModes) {
			this.enRouteModes = new IdMap<>(Vehicle.class);
			this.linksPerMode = new IdMap<>(Link.class);
		} else {
			this.enRouteModes = null;
			this.linksPerMode = null;
		}
	}

	@Override
	public void handleEvent(VehicleEntersTrafficEvent event) {
		if (this.observeModes) {
			this.enRouteModes.put(event.getVehicleId(), event.getNetworkMode());
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

		if (this.observeModes) {
			Map<String, int[]> modeVolumes = this.linksPerMode.get(event.getLinkId());
			if (modeVolumes == null) {
				modeVolumes = new HashMap<>();
				this.linksPerMode.put(event.getLinkId(), modeVolumes);
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
		return ((int) time / this.timeBinSize);
	}

	/**
	 * @param linkId
	 * @return Array containing the number of vehicles leaving the link <code>linkId</code> per time bin,
	 * starting with time bin 0 from 0 seconds to (timeBinSize-1)seconds.
	 */
	public int[] getVolumesForLink(final Id<Link> linkId) {
		return this.links.get(linkId);
	}

	/**
	 * @param linkId
	 * @param mode
	 * @return Array containing the number of vehicles using the specified mode leaving the link
	 * <code>linkId</code> per time bin, starting with time bin 0 from 0 seconds to (timeBinSize-1)seconds.
	 */
	public int[] getVolumesForLink(final Id<Link> linkId, String mode) {
		if (observeModes) {
			Map<String, int[]> modeVolumes = this.linksPerMode.get(linkId);
			if (modeVolumes != null) return modeVolumes.get(mode);
		}
		return null;
	}

	/**
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

		int slotsPerHour = (int) (3600.0 / this.timeBinSize);
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
		if (observeModes) {
			if (3600.0 % this.timeBinSize != 0) log.error("Volumes per hour and per link probably not correct!");

			double[] volumes = new double[24];
			for (int hour = 0; hour < 24; hour++) {
				volumes[hour] = 0.0;
			}

			int[] volumesForLink = this.getVolumesForLink(linkId, mode);
			if (volumesForLink == null) return volumes;

			int slotsPerHour = (int) (3600.0 / this.timeBinSize);
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

		for (Map<String, int[]> map : this.linksPerMode.values()) {
			try {
				modes.addAll(map.keySet());
			} catch (NullPointerException ex) {
				// Fails on initialized entries, which can happen during tests
				// will just be ignored here
			}
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
		if (observeModes) {
			this.linksPerMode.clear();
			this.enRouteModes.clear();
		}
	}
}
