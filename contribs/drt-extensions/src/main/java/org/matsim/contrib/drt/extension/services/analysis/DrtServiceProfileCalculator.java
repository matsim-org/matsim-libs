/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2024 by the members listed in the COPYING,        *
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
package org.matsim.contrib.drt.extension.services.analysis;

import com.google.common.base.Verify;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdMap;
import org.matsim.contrib.common.timeprofile.TimeDiscretizer;
import org.matsim.contrib.drt.extension.services.events.DrtServiceEndedEvent;
import org.matsim.contrib.drt.extension.services.events.DrtServiceEndedEventHandler;
import org.matsim.contrib.drt.extension.services.events.DrtServiceStartedEvent;
import org.matsim.contrib.drt.extension.services.events.DrtServiceStartedEventHandler;
import org.matsim.contrib.drt.extension.services.schedule.DrtService;
import org.matsim.contrib.dvrp.fleet.DvrpVehicleSpecification;
import org.matsim.contrib.dvrp.fleet.FleetSpecification;
import org.matsim.core.config.groups.QSimConfigGroup;

import java.util.HashMap;
import java.util.Map;

/**
 * @author steffenaxer
 */
public class DrtServiceProfileCalculator implements DrtServiceEndedEventHandler,
	DrtServiceStartedEventHandler {

	private final TimeDiscretizer timeDiscretizer;

	private Map<String, double[]> serviceProfile;

	private final Map<Id<DrtService>, DrtServiceStartedEvent> vehicleServices = new IdMap<>(DrtService.class);

	private final double analysisEndTime;

	private final String dvrpMode;

	private boolean wasConsolidatedInThisIteration = false;

	public DrtServiceProfileCalculator(String dvrpMode, FleetSpecification fleet, int timeInterval,
                                       QSimConfigGroup qsimConfig) {
		this.dvrpMode = dvrpMode;
		double qsimEndTime = qsimConfig.getEndTime().orElse(0);
		double maxServiceEndTime = fleet.getVehicleSpecifications()
				.values()
				.stream()
				.mapToDouble(DvrpVehicleSpecification::getServiceEndTime)
				.max()
				.orElse(0);
		analysisEndTime = Math.max(qsimEndTime, maxServiceEndTime);
		timeDiscretizer = new TimeDiscretizer((int)Math.ceil(analysisEndTime), timeInterval);
	}


	public TimeDiscretizer getTimeDiscretizer() {
		return timeDiscretizer;
	}

	public Map<String, double[]> getProfile()
	{
		this.consolidate();
		return this.serviceProfile;
	}

	private void consolidate() {
		if (!wasConsolidatedInThisIteration) {
			serviceProfile.values().forEach(this::normalizeProfile);
			wasConsolidatedInThisIteration = true;
		}
	}

	private void normalizeProfile(double[] profile) {
		for (int i = 0; i < profile.length; i++) {
			profile[i] /= timeDiscretizer.getTimeInterval();
		}
	}

	private void increment(String serviceType,double beginTime, double endTime) {
		Verify.verify(serviceType != null);

		double[] profile = serviceProfile.computeIfAbsent(serviceType,
						v -> new double[timeDiscretizer.getIntervalCount()]);
		increment(profile, Math.min(beginTime, endTime), endTime);
	}

	private void increment(double[] values, double beginTime, double endTime) {
		if (beginTime == endTime && beginTime >= analysisEndTime) {
			return;
		}
		endTime = Math.min(endTime, analysisEndTime);

		double timeInterval = timeDiscretizer.getTimeInterval();
		int fromIdx = timeDiscretizer.getIdx(beginTime);
		int toIdx = timeDiscretizer.getIdx(endTime);

		for (int i = fromIdx; i < toIdx; i++) {
			values[i] += timeInterval;
		}
	}


	/* Event handling starts here */

	@Override
	public void handleEvent(DrtServiceEndedEvent event) {
		if (!event.getMode().equals(dvrpMode)) {
			return;
		}

		DrtServiceStartedEvent serviceStartedEvent = this.vehicleServices.remove(event.getDrtServiceId());
		increment(serviceStartedEvent.getServiceType(), serviceStartedEvent.getTime(), event.getTime());
	}

	@Override
	public void handleEvent(DrtServiceStartedEvent event) {
		if (!event.getMode().equals(dvrpMode)) {
			return;
		}

		this.vehicleServices.putIfAbsent(event.getDrtServiceId(), event);
	}

	@Override
	public void reset(int iteration) {
		vehicleServices.clear();
		serviceProfile = new HashMap<>();
		wasConsolidatedInThisIteration = false;
	}
}
