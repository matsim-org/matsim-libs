/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package org.matsim.contrib.dvrp.trafficmonitoring;

import org.matsim.core.controler.AbstractModule;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.trafficmonitoring.*;

import com.google.inject.name.Names;


public class VrpTravelTimeModules
{
    public static final String DVRP_INITIAL = "dvrp_initial";
    public static final String DVRP_ESTIMATED = "dvrp_estimated";


    public static AbstractModule createTravelTimeEstimatorModule()
    {
        return createTravelTimeEstimatorModule(new FreeSpeedTravelTime());
    }


    /**
     * Travel times recorded during the previous iteration. They are always updated after the mobsim
     * ends. This is the standard approach for running DVRP
     */
    public static AbstractModule createTravelTimeEstimatorModule(final TravelTime initialTravelTime)
    {
        return new AbstractModule() {
            public void install()
            {
                bind(TravelTime.class).annotatedWith(Names.named(DVRP_INITIAL))
                        .toInstance(initialTravelTime);
                bind(VrpTravelTimeEstimator.class).asEagerSingleton();
                bind(TravelTime.class).annotatedWith(Names.named(DVRP_ESTIMATED))
                        .to(VrpTravelTimeEstimator.class);
                addMobsimListenerBinding().to(VrpTravelTimeEstimator.class);
            }
        };
    }


    /**
     * Travel times are fixed (useful for TimeVariantNetworks with variable free-flow speeds and no
     * other traffic)
     */
    public static AbstractModule createFreespeedTravelTimeModule(boolean disableTTCalculator)
    {
        return createExternalTravelTimeModule(new FreeSpeedTravelTime(), disableTTCalculator);
    }


    /**
     * Travel times are fixed
     */
    public static AbstractModule createExternalTravelTimeModule(final TravelTime travelTime,
            final boolean disableTTCalculator)
    {
        return new AbstractModule() {
            public void install()
            {
                if (disableTTCalculator) {//overwriting the default calculator
                    bind(TravelTimeCalculator.class).to(InactiveTravelTimeCalculator.class);
                }

                bind(TravelTime.class).annotatedWith(Names.named(DVRP_ESTIMATED)).toInstance(travelTime);
            }
        };
    }
}
