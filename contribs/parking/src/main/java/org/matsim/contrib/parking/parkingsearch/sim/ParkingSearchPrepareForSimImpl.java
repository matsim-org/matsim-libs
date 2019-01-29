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

package org.matsim.contrib.parking.parkingsearch.sim;


import java.util.HashSet;
import javax.inject.Inject;
import javax.inject.Provider;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.groups.GlobalConfigGroup;
import org.matsim.core.controler.PrepareForSim;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.TransportModeNetworkFilter;
import org.matsim.core.population.algorithms.AbstractPersonAlgorithm;
import org.matsim.core.population.algorithms.ParallelPersonAlgorithmUtils;
import org.matsim.core.population.algorithms.PersonPrepareForSim;
import org.matsim.core.router.PlanRouter;
import org.matsim.core.router.TripRouter;
import org.matsim.core.scenario.Lockable;
import org.matsim.facilities.ActivityFacilities;

@Deprecated // what is this class doing that is not done by the central infrastructure?  kai, jul'18
public class ParkingSearchPrepareForSimImpl implements PrepareForSim {
    private static Logger log = Logger.getLogger(PrepareForSim.class);

    private final GlobalConfigGroup globalConfigGroup;
    private final Scenario scenario;
    private final Network network;
    private final Population population;
    private final ActivityFacilities activityFacilities;
    private final Provider<TripRouter> tripRouterProvider;

    @Inject
    ParkingSearchPrepareForSimImpl(GlobalConfigGroup globalConfigGroup, Scenario scenario, Network network, Population population, ActivityFacilities activityFacilities, Provider<TripRouter> tripRouterProvider) {
        this.globalConfigGroup = globalConfigGroup;
        this.scenario = scenario;
        this.network = network;
        this.population = population;
        this.activityFacilities = activityFacilities;
        this.tripRouterProvider = tripRouterProvider;
    }


    @Override
    public void run() {
		/*
		 * Create single-mode network here and hand it over to PersonPrepareForSim. Otherwise, each instance would create its
		 * own single-mode network. However, this assumes that the main mode is car - which PersonPrepareForSim also does. Should
		 * be probably adapted in a way that other main modes are possible as well. cdobler, oct'15.
		 */
//        final Network net;
//        if (NetworkUtils.isMultimodal(network)) {
//            log.info("Network seems to be multimodal. Create car-only network which is handed over to PersonPrepareForSim.");
//            TransportModeNetworkFilter filter = new TransportModeNetworkFilter(network);
//            net = NetworkUtils.createNetwork();
//            HashSet<String> modes = new HashSet<>();
//            modes.add(TransportMode.car);
//            filter.filter(net, modes);
//        } else {
//            net = network;
//        }

        // make sure all routes are calculated.
        ParallelPersonAlgorithmUtils.run(population, globalConfigGroup.getNumberOfThreads(),
                new ParallelPersonAlgorithmUtils.PersonAlgorithmProvider() {
                    @Override
                    public AbstractPersonAlgorithm getPersonAlgorithm() {
                        return new PersonPrepareForSim(new PlanRouter(tripRouterProvider.get(), activityFacilities), scenario);
                    }
                });

        if (scenario instanceof Lockable) {
            ((Lockable)scenario).setLocked();
            // see comment in ScenarioImpl. kai, sep'14
        }

        if (population instanceof Lockable) {
            ((Lockable) population).setLocked();
        }

        if ( network instanceof Lockable ) {
            ((Lockable) network).setLocked();
        }

        // (yyyy means that if someone replaces prepareForSim and does not add the above lines, the containers are not locked.  kai, nov'16)

    }
}
