/* *********************************************************************** *
 * project: org.matsim.* 												   *
 *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2023 by the members listed in the COPYING,        *
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

package ch.sbb.matsim.routing.pt.raptor;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.pt.router.TransitScheduleChangedEventHandler;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.vehicles.Vehicles;

import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;

/**
 * @author mrieser / SBB
 */
@Singleton
public class SwissRailRaptorFactory implements Provider<SwissRailRaptor> {

    private SwissRailRaptorData data = null;
    private final TransitSchedule schedule;
    private final Vehicles transitVehicles;
    private final RaptorStaticConfig raptorConfig;
    private final RaptorParametersForPerson raptorParametersForPerson;
    private final RaptorRouteSelector routeSelector;
    private final Provider<RaptorStopFinder> stopFinderProvider;
    private final OccupancyData occupancyData;
    private final RaptorInVehicleCostCalculator inVehicleCostCalculator;
    private final RaptorTransferCostCalculator transferCostCalculator;

    private final Network network;

    @Inject
    public SwissRailRaptorFactory(final Scenario scenario, final Config config,
                                  RaptorParametersForPerson raptorParametersForPerson, RaptorRouteSelector routeSelector,
                                  Provider<RaptorStopFinder> stopFinderProvider, OccupancyData occupancyData,
                                  RaptorInVehicleCostCalculator inVehicleCostCalculator,
                                  RaptorTransferCostCalculator transferCostCalculator,
                                  final EventsManager events) {
        this.schedule = scenario.getTransitSchedule();
        this.transitVehicles = scenario.getTransitVehicles();
        this.raptorConfig = RaptorUtils.createStaticConfig(config);
        this.network = scenario.getNetwork();
        this.raptorParametersForPerson = raptorParametersForPerson;
        this.routeSelector = routeSelector;
        this.stopFinderProvider = stopFinderProvider;
        this.occupancyData = occupancyData;
        this.inVehicleCostCalculator = inVehicleCostCalculator;
        this.transferCostCalculator = transferCostCalculator;

        if (events != null) {
            events.addHandler((TransitScheduleChangedEventHandler) event -> this.data = null);
        }
    }

    @Override
    public SwissRailRaptor get() {
        SwissRailRaptorData data = getData();
        return new SwissRailRaptor(data, this.raptorParametersForPerson, this.routeSelector, this.stopFinderProvider.get(), this.inVehicleCostCalculator, this.transferCostCalculator);
    }

    private SwissRailRaptorData getData() {
        if (this.data == null) {
            this.data = prepareData();
        }
        return this.data;
    }

    synchronized private SwissRailRaptorData prepareData() {
        if (this.data != null) {
            // due to multithreading / race conditions, this could still happen.
            // prevent doing the work twice.
            return this.data;
        }
        this.data = SwissRailRaptorData.create(this.schedule, this.transitVehicles, this.raptorConfig, this.network, this.occupancyData);
        return this.data;
    }

}
