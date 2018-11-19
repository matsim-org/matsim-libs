/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2018 by the members listed in the COPYING,        *
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

package peoplemover.stop;

import com.google.inject.name.Names;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.dvrp.vrpagent.VrpAgentLogic;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;

/**
 * @author Michal Maciejewski (michalm)
 */
public class VariableDurationQSimModule extends AbstractQSimModule {
    private final BusStopDurationCalculator busStopDurationCalculator;

    public VariableDurationQSimModule(BusStopDurationCalculator busStopDurationCalculator) {
        this.busStopDurationCalculator = busStopDurationCalculator;
    }

    @Override
    protected void configureQSim() {
        bind(BusStopDurationCalculator.class).toInstance(busStopDurationCalculator);
        bind(VrpAgentLogic.DynActionCreator.class).annotatedWith(Names.named(DrtConfigGroup.get(getConfig()).getMode()))
                .to(CustomizedDrtActionCreator.class)
                .asEagerSingleton();
    }
}
