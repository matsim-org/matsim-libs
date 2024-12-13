/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
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
 * *********************************************************************** */

package org.matsim.contrib.drt.extension.insertion.spatialFilter;

import org.matsim.contrib.drt.extension.DrtWithExtensionsConfigGroup;
import org.matsim.contrib.drt.optimizer.insertion.RequestFleetFilter;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.dvrp.fleet.Fleet;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeQSimModule;
import org.matsim.core.mobsim.framework.MobsimTimer;

/**
 * @author nkuehnel | MOIA
 */
public class SpatialFilterInsertionSearchQSimModule extends AbstractDvrpModeQSimModule {


    private final DrtSpatialRequestFleetFilterParams drtSpatialRequestFleetFilterParams;

    public SpatialFilterInsertionSearchQSimModule(DrtConfigGroup drtCfg) {
        super(drtCfg.getMode());
        if(drtCfg instanceof DrtWithExtensionsConfigGroup withExtensionsConfigGroup &&
            withExtensionsConfigGroup.getSpatialRequestFleetFilterParams().isPresent()) {
            drtSpatialRequestFleetFilterParams = withExtensionsConfigGroup.getSpatialRequestFleetFilterParams().get();
        } else {
            throw new RuntimeException("Requires DrtSpatialRequestFleetFilterParams to be set. Use DrtWithExtensionsConfigGroup " +
                    "to do so.");
        }
    }

    public record SpatialInsertionFilterSettings(double expansionIncrement, double minExpansion, double maxExpansion,
                                          boolean returnAllIfEmpty, int minCandidates, double updateInterval){}

    @Override
    protected void configureQSim() {
        bindModal(RequestFleetFilter.class).toProvider(modalProvider(getter ->
                new SpatialRequestFleetFilter(getter.getModal(Fleet.class), getter.get(MobsimTimer.class), drtSpatialRequestFleetFilterParams)
        ));
    }
}
