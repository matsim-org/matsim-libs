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
package org.matsim.contrib.drt.extension.services.optimizer;

import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.drt.extension.DrtWithExtensionsConfigGroup;
import org.matsim.contrib.drt.extension.services.services.tracker.ServiceExecutionTrackers;
import org.matsim.contrib.drt.extension.services.dispatcher.ServiceTaskDispatcher;
import org.matsim.contrib.drt.extension.services.dispatcher.ServiceTaskDispatcherImpl;
import org.matsim.contrib.drt.extension.services.services.*;
import org.matsim.contrib.drt.extension.services.schedule.ServiceTaskScheduler;
import org.matsim.contrib.drt.extension.services.schedule.ServiceTaskSchedulerImpl;
import org.matsim.contrib.drt.extension.services.services.params.DrtServicesParams;
import org.matsim.contrib.drt.extension.operations.operationFacilities.OperationFacilityFinder;
import org.matsim.contrib.drt.extension.services.tasks.StackableTasks;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.schedule.DrtTaskFactory;
import org.matsim.contrib.dvrp.fleet.Fleet;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeQSimModule;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.TravelTime;

/**
 * @author steffenaxer
 */
public class DrtServiceQSimModule extends AbstractDvrpModeQSimModule {

    private final DrtServicesParams drtServicesParams;
    private final DrtConfigGroup drtConfigGroup;

    public DrtServiceQSimModule(DrtConfigGroup drtConfigGroup) {
        super(drtConfigGroup.getMode());
        this.drtConfigGroup = drtConfigGroup;
        this.drtServicesParams = ((DrtWithExtensionsConfigGroup) drtConfigGroup).getServicesParams().orElseThrow();
    }

    @Override
    protected void configureQSim() {

        bindModal(ServiceTaskDispatcher.class).toProvider(modalProvider(getter -> new ServiceTaskDispatcherImpl(
                drtServicesParams,
                getter.getModal(Fleet.class),
                getter.getModal(ServiceTaskScheduler.class),
                getter.getModal(OperationFacilityFinder.class),
                getter.getModal(ServiceTriggerFactory.class),
                getter.getModal(ServiceExecutionTrackers.class)
		))).asEagerSingleton();

        bindModal(ServiceTaskScheduler.class).toProvider(modalProvider(getter -> new ServiceTaskSchedulerImpl(
                drtConfigGroup,
                getter.getModal(Network.class),
                getter.getModal(TravelTime.class),
                getter.getModal(TravelDisutilityFactory.class).createTravelDisutility(getter.getModal(TravelTime.class)),
                getter.get(MobsimTimer.class),
                getter.getModal(DrtTaskFactory.class),
                getter.getModal(StackableTasks.class),
                getter.get(EventsManager.class),
                getter.get(MobsimTimer.class)
        ))).asEagerSingleton();

    }
}
