/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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
package org.matsim.contrib.drt.analysis.zonal;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.mutable.MutableInt;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.utils.misc.Time;

/**
 * @author jbischoff
 */
public class ZonalDemandAggregator implements PersonDepartureEventHandler {

	private final DrtZonalSystem zonalSystem;
	private final String mode;
	private final int timeBinSize;
	private final Map<Double, Map<String, MutableInt>> departures = new HashMap<>();
	private final Map<Double, Map<String, MutableInt>> previousIterationDepartures = new HashMap<>();

	public ZonalDemandAggregator(EventsManager events, DrtZonalSystem zonalSystem, DrtConfigGroup drtCfg) {
		this.zonalSystem = zonalSystem;
		mode = drtCfg.getMode();
		timeBinSize = drtCfg.getMinCostFlowRebalancing().getInterval();
		events.addHandler(this);
	}

	@Override
	public void reset(int iteration) {
		previousIterationDepartures.clear();
		previousIterationDepartures.putAll(departures);
		departures.clear();
		prepareZones();
	}

	@Override
	public void handleEvent(PersonDepartureEvent event) {
		if (event.getLegMode().equals(mode)) {
			Double bin = getBinForTime(event.getTime());

			String zoneId = zonalSystem.getZoneForLinkId(event.getLinkId());
			if (zoneId == null) {
				Logger.getLogger(getClass()).error("No zone found for linkId " + event.getLinkId().toString());
				return;
			}
			if (departures.containsKey(bin)) {
				this.departures.get(bin).get(zoneId).increment();
			} else
				Logger.getLogger(getClass())
						.error("Time " + Time.writeTime(event.getTime()) + " / bin " + bin + " is out of boundary");
		}
	}

	private void prepareZones() {
		for (int i = 0; i < (3600 / timeBinSize) * 36; i++) {
			Map<String, MutableInt> zonesPerSlot = new HashMap<>();
			for (String zone : zonalSystem.getZones().keySet()) {
				zonesPerSlot.put(zone, new MutableInt());
			}
			departures.put(Double.valueOf(i), zonesPerSlot);
		}
	}

	private Double getBinForTime(double time) {
		return Math.floor(time / timeBinSize);
	}

	public Map<String, MutableInt> getExpectedDemandForTimeBin(double time) {
		Double bin = getBinForTime(time);
		return previousIterationDepartures.getOrDefault(bin, Collections.emptyMap());
	}
}
