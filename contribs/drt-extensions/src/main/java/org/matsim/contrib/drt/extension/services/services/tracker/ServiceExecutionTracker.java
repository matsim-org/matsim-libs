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
package org.matsim.contrib.drt.extension.services.services.tracker;

import com.google.common.base.Verify;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdMap;
import org.matsim.contrib.drt.extension.services.events.*;
import org.matsim.contrib.drt.extension.services.schedule.DrtService;
import org.matsim.contrib.drt.extension.services.services.params.DrtServiceParams;
import org.matsim.contrib.drt.extension.services.services.triggers.ServiceExecutionTrigger;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;

import java.util.*;

/**
 * @author steffenaxer
 */
public class ServiceExecutionTracker implements DrtServiceEndedEventHandler, DrtServiceStartedEventHandler, DrtServiceScheduledEventHandler {
	private final Id<DvrpVehicle> vehicleId;
	private final Map<DrtServiceParams, List<ServiceExecutionTrigger>> service2Triggers = new HashMap<>();
	private final List<DrtServiceEntry> executionLog = new ArrayList<>();
	private final Map<String, List<DrtServiceScheduledEvent>> scheduledServices = new HashMap<>();
	private final DrtConfigGroup drtConfigGroup;
	private final Map<Id<DrtService>, DrtServiceStartedEvent> drtServicesStartedMap = new IdMap<>(DrtService.class);

	public ServiceExecutionTracker(Id<DvrpVehicle> vehicleId, DrtConfigGroup drtConfigGroup) {
		this.vehicleId = vehicleId;
		this.drtConfigGroup = drtConfigGroup;
	}

	public Set<DrtServiceParams> getServices() {
		return this.service2Triggers.keySet();
	}

	public void addTrigger(DrtServiceParams drtServiceParams, ServiceExecutionTrigger trigger) {
		this.service2Triggers.computeIfAbsent(drtServiceParams, k -> new ArrayList<>()).add(trigger);
	}

	public List<ServiceExecutionTrigger> getTriggers(DrtServiceParams drtServiceParams) {
		return this.service2Triggers.get(drtServiceParams);
	}

	public int getScheduledCounter(String serviceType) {
		return this.scheduledServices.getOrDefault(serviceType, Collections.emptyList()).size();
	}

	public List<DrtServiceEntry> getExecutionRecords() {
		return Collections.unmodifiableList(this.executionLog);
	}

	@Override
	public void handleEvent(DrtServiceEndedEvent event) {
		DrtServiceStartedEvent drtServiceStartedEvent = drtServicesStartedMap.remove(event.getDrtServiceId());
		this.executionLog.add(new DrtServiceEntry(drtServiceStartedEvent, event));
	}

	@Override
	public void handleEvent(DrtServiceStartedEvent event) {
		Verify.verify(drtConfigGroup.mode.equals(event.getMode()));
		Verify.verify(event.getVehicleId().equals(this.vehicleId));
		drtServicesStartedMap.put(event.getDrtServiceId(), event);
	}

	@Override
	public void handleEvent(DrtServiceScheduledEvent event) {
		Verify.verify(drtConfigGroup.mode.equals(event.getMode()));
		Verify.verify(event.getVehicleId().equals(this.vehicleId));
		scheduledServices.computeIfAbsent(event.getServiceType(), k -> new ArrayList<>()).add(event);
	}

	public record DrtServiceEntry(DrtServiceStartedEvent drtServiceStartedEvent, DrtServiceEndedEvent drtServiceEndedEvent) {
	}

}
