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

import com.google.common.base.Preconditions;
import org.apache.commons.lang3.mutable.MutableInt;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.contrib.common.zones.Zone;
import org.matsim.contrib.common.zones.ZoneSystem;
import org.matsim.contrib.drt.run.DrtConfigGroup;

import java.util.HashMap;
import java.util.Map;
import java.util.function.ToDoubleFunction;

/**
 * Aggregates PersonDepartureEvents per iteration for the given mode and returns the numbers from the previous iteration
 * as expected demand for the current iteration.
 *
 * @author jbischoff
 * @author michalm
 */
public final class PreviousIterationDrtDemandEstimator implements ZonalDemandEstimator, PersonDepartureEventHandler {
	private static final Logger logger = LogManager.getLogger(PreviousIterationDrtDemandEstimator.class);

	private final ZoneSystem zonalSystem;
	private final String mode;
	private final int timeBinSize;
	private Map<Integer, Map<Zone, MutableInt>> currentIterationDepartures = new HashMap<>();
	private Map<Integer, Map<Zone, MutableInt>> previousIterationDepartures = new HashMap<>();

	public PreviousIterationDrtDemandEstimator(ZoneSystem zonalSystem, DrtConfigGroup drtCfg,
			int demandEstimationPeriod) {
		this.zonalSystem = zonalSystem;
		mode = drtCfg.getMode();
		timeBinSize = demandEstimationPeriod;
	}

	@Override
	public void reset(int iteration) {
		previousIterationDepartures = currentIterationDepartures;
		currentIterationDepartures = new HashMap<>();
	}

	@Override
	public void handleEvent(PersonDepartureEvent event) {
		if (event.getLegMode().equals(mode)) {
			zonalSystem.getZoneForLinkId(event.getLinkId()).ifPresentOrElse(
                    zone -> {
                        int timeBin = getBinForTime(event.getTime());
                        currentIterationDepartures.computeIfAbsent(timeBin, v -> new HashMap<>())
                            .computeIfAbsent(zone, z -> new MutableInt())
                            .increment();
                    },
				//might be that somebody walks into the service area or that service area is larger/different than DrtZonalSystem...
				() -> logger.warn("No zone found for linkId " + event.getLinkId().toString())
			);
		}
	}

	private static final MutableInt ZERO = new MutableInt(0);

	@Override
	public ToDoubleFunction<Zone> getExpectedDemand(double fromTime, double estimationPeriod) {
		Preconditions.checkArgument(estimationPeriod == timeBinSize);//TODO add more flexibility later
		int timeBin = getBinForTime(fromTime);
		Map<Zone, MutableInt> expectedDemandForTimeBin = previousIterationDepartures.getOrDefault(timeBin, Map.of());
		return zone -> expectedDemandForTimeBin.getOrDefault(zone, ZERO).intValue();
	}

	private int getBinForTime(double time) {
		return (int)(time / timeBinSize);
	}
}
