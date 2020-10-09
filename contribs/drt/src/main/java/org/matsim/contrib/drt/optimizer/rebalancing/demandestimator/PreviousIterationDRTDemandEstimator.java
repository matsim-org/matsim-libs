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

import java.util.HashMap;
import java.util.Map;
import java.util.function.ToDoubleFunction;

import org.apache.commons.lang3.mutable.MutableInt;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.contrib.drt.analysis.zonal.DrtZonalSystem;
import org.matsim.contrib.drt.analysis.zonal.DrtZone;
import org.matsim.contrib.drt.run.DrtConfigGroup;

import com.google.common.base.Preconditions;

/**
 * Aggregates PersonDepartureEvents per iteration for the given mode and returns the numbers from the previous iteration
 * as expected demand for the current iteration.
 *
 * @author jbischoff
 * @author michalm
 */
public final class PreviousIterationDRTDemandEstimator implements ZonalDemandEstimator, PersonDepartureEventHandler {
	private static final Logger logger = Logger.getLogger(PreviousIterationDRTDemandEstimator.class);

	private final DrtZonalSystem zonalSystem;
	private final String mode;
	private final String drtSpeedUpMode;
	private final int timeBinSize;
	private Map<Double, Map<DrtZone, MutableInt>> currentIterationDepartures = new HashMap<>();
	private Map<Double, Map<DrtZone, MutableInt>> previousIterationDepartures = new HashMap<>();

	public PreviousIterationDRTDemandEstimator(DrtZonalSystem zonalSystem, DrtConfigGroup drtCfg) {
		this.zonalSystem = zonalSystem;
		mode = drtCfg.getMode();
		drtSpeedUpMode = drtCfg.getDrtSpeedUpMode();
		timeBinSize = drtCfg.getRebalancingParams().get().getInterval();
	}

	@Override
	public void reset(int iteration) {
		previousIterationDepartures = currentIterationDepartures;
		currentIterationDepartures = new HashMap<>();
	}

	@Override
	public void handleEvent(PersonDepartureEvent event) {
		if (event.getLegMode().equals(mode) || event.getLegMode().equals(drtSpeedUpMode)) {
			Double bin = getBinForTime(event.getTime());

			DrtZone zone = zonalSystem.getZoneForLinkId(event.getLinkId());
			if (zone == null) {
				//might be that somebody walks into the service area or that service area is larger/different than DrtZonalSystem...
				logger.warn("No zone found for linkId " + event.getLinkId().toString());
				return;
			}

			currentIterationDepartures.computeIfAbsent(bin, v -> new HashMap<>())
					.computeIfAbsent(zone, z -> new MutableInt())
					.increment();
		}
	}

	private static final MutableInt ZERO = new MutableInt(0);

	public ToDoubleFunction<DrtZone> getExpectedDemandForTimeBin(double time) {
		Double bin = getBinForTime(time);
		Map<DrtZone, MutableInt> expectedDemandForTimeBin = previousIterationDepartures.getOrDefault(bin, Map.of());
		return zone -> expectedDemandForTimeBin.getOrDefault(zone, ZERO).intValue();
	}

	private Double getBinForTime(double time) {
		return Math.floor(time / timeBinSize);
	}
}
