/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2020 by the members listed in the COPYING,        *
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
 * *********************************************************************** *
 */

/**
 *
 */
package org.matsim.contrib.drt.optimizer.rebalancing.demandestimator;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.ToIntFunction;

import org.apache.commons.lang3.mutable.MutableInt;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.contrib.drt.analysis.zonal.DrtZonalSystem;
import org.matsim.contrib.drt.analysis.zonal.DrtZone;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.core.utils.misc.Time;

/**
 * Aggregates PersonDepartureEvents per iteration for the given mode and returns the numbers from the previous iteration
 * as expected demand for the current iteration.
 *
 * @author jbischoff
 */
public final class PreviousIterationZonalDRTDemandEstimator
		implements ZonalDemandEstimator, PersonDepartureEventHandler {

	private final DrtZonalSystem zonalSystem;
	private final String mode;
	private final String drtSpeedUpMode;
	private final int timeBinSize;
	private final Map<Double, Map<DrtZone, MutableInt>> departures = new HashMap<>();
	private final Map<Double, Map<DrtZone, MutableInt>> previousIterationDepartures = new HashMap<>();
	private static final MutableInt ZERO = new MutableInt(0);

	public PreviousIterationZonalDRTDemandEstimator(DrtZonalSystem zonalSystem, DrtConfigGroup drtCfg) {
		this.zonalSystem = zonalSystem;
		mode = drtCfg.getMode();
		drtSpeedUpMode = drtCfg.getDrtSpeedUpMode();
		timeBinSize = drtCfg.getRebalancingParams().get().getInterval();
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
		if (event.getLegMode().equals(mode) || event.getLegMode().equals(drtSpeedUpMode)) {
			Double bin = getBinForTime(event.getTime());

			DrtZone zone = zonalSystem.getZoneForLinkId(event.getLinkId());
			if (zone == null) {
				Logger.getLogger(getClass()).error("No zone found for linkId " + event.getLinkId().toString());
				return;
			}
			if (departures.containsKey(bin)) {
				this.departures.get(bin).get(zone).increment();
			} else
				Logger.getLogger(getClass())
						.error("Time " + Time.writeTime(event.getTime()) + " / bin " + bin + " is out of boundary");
		}
	}

	private void prepareZones() {
		for (int i = 0; i < (3600 / timeBinSize) * 36; i++) {
			Map<DrtZone, MutableInt> zonesPerSlot = new HashMap<>();
			for (DrtZone zone : zonalSystem.getZones().values()) {
				zonesPerSlot.put(zone, new MutableInt());
			}
			departures.put((double)i, zonesPerSlot);
		}
	}

	private Double getBinForTime(double time) {
		return Math.floor(time / timeBinSize);
	}

	public ToIntFunction<DrtZone> getExpectedDemandForTimeBin(double time) {
		Double bin = getBinForTime(time);
		Map<DrtZone, MutableInt> expectedDemandForTimeBin = previousIterationDepartures.getOrDefault(bin,
				Collections.emptyMap());
		return zone -> expectedDemandForTimeBin.getOrDefault(zone, ZERO).intValue();
	}
}
