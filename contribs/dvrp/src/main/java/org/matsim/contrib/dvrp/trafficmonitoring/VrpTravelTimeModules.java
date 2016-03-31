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

import javax.inject.Singleton;

import org.matsim.core.controler.AbstractModule;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;

import com.google.inject.name.Names;


public class VrpTravelTimeModules
{
    public static final String DVRP = "dvrp";


    /**
     * Travel times recorded during the previous iteration. They are always updated after mobsim
     * ends. This is the standard approach for running DVRP
     */
    public static AbstractModule createTravelTimeEstimatorModule()
    {
        return new AbstractModule() {
            public void install()
            {
                bind(TravelTime.class).annotatedWith(Names.named(DVRP))
                        .to(VrpTravelTimeEstimator.class).in(Singleton.class);
                addMobsimListenerBinding().to(VrpTravelTimeEstimator.class);
            }
        };
    }


    /**
     * Travel times are fixed (useful for TimeVariantNetworks with variable free-flow speeds and no
     * other traffic)
     */
    public static AbstractModule createFreespeedTravelTimeModule()
    {
        return createExternalTravelTimeModule(new FreeSpeedTravelTime());
    }


    /**
     * Travel times are fixed
     */
    public static AbstractModule createExternalTravelTimeModule(final TravelTime travelTime)
    {
        return new AbstractModule() {
            public void install()
            {
                bind(TravelTime.class).annotatedWith(Names.named(DVRP)).toInstance(travelTime);
            }
        };
    }
}
