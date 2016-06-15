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

import org.matsim.api.core.v01.TransportMode;
import org.matsim.contrib.dvrp.trafficmonitoring.VrpTravelTimeEstimator.Params;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;


public class VrpTravelTimeModules
{
    public static final String DVRP_ESTIMATED = "dvrp_estimated";


    public static AbstractModule createTravelTimeEstimatorModule(double expAveragingAlpha)
    {
        return createTravelTimeEstimatorModule(new FreeSpeedTravelTime(), expAveragingAlpha);
    }


    /**
     * Travel times recorded during the previous iteration. They are always updated after the mobsim
     * ends. This is the standard approach for running DVRP
     */
    public static AbstractModule createTravelTimeEstimatorModule(final TravelTime initialTravelTime,
            final double expAveragingAlpha)
    {
        return new AbstractModule() {
            public void install()
            {
                bind(Params.class).toInstance(new Params(initialTravelTime, expAveragingAlpha));
                bind(VrpTravelTimeEstimator.class).asEagerSingleton();
                addTravelTimeBinding(DVRP_ESTIMATED).to(VrpTravelTimeEstimator.class);
                addMobsimListenerBinding().to(VrpTravelTimeEstimator.class);
            }
        };
    }


    public static AbstractModule createFreeSpeedTravelTimeForBenchmarkingModule()
    {
        return createTravelTimeForBenchmarkingModule(new FreeSpeedTravelTime());
    }


    /**
     * Instead of TravelTimeCalculatorModule
     */
    public static AbstractModule createTravelTimeForBenchmarkingModule(final TravelTime travelTime)
    {
        return new AbstractModule() {
            public void install()
            {
                addTravelTimeBinding(TransportMode.car).toInstance(travelTime);
                addTravelTimeBinding(DVRP_ESTIMATED).toInstance(travelTime);
            }
        };
    }
}
