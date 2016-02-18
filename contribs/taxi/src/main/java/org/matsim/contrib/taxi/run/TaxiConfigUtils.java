/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package org.matsim.contrib.taxi.run;

import org.matsim.contrib.dvrp.run.VrpConfigUtils;
import org.matsim.contrib.taxi.TaxiUtils;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup.ModeRoutingParams;


public class TaxiConfigUtils
{
    public static Config createConfig()
    {
        Config config = VrpConfigUtils.createConfig();
        updatePlansCalcRouteConfigGroup(config);
        return config;
    }


    public static Config loadConfig(final String file)
    {
        Config config = VrpConfigUtils.loadConfig(file);
        updatePlansCalcRouteConfigGroup(config);
        return config;
    }


    private static void updatePlansCalcRouteConfigGroup(Config config)
    {
        ModeRoutingParams taxiParams = new ModeRoutingParams();
        taxiParams.setMode(TaxiUtils.TAXI_MODE);
        taxiParams.setTeleportedModeFreespeedFactor(1.1);//TODO probably should be larger than 1.0
        config.plansCalcRoute().addModeRoutingParams(taxiParams);
    }
}
