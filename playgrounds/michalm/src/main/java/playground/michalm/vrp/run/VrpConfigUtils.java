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

package playground.michalm.vrp.run;

import org.matsim.core.config.*;


public class VrpConfigUtils
{
    public static Config createConfig()
    {
        Config config = ConfigUtils.createConfig();
        config.getQSimConfigGroup().setInsertingWaitingVehiclesBeforeDrivingVehicles(true);
        return config;
    }


    public static Config loadConfig(final String filename)
    {
        Config config = ConfigUtils.loadConfig(filename);

        if (!config.getQSimConfigGroup().isInsertingWaitingVehiclesBeforeDrivingVehicles()) {
            System.err.println("isInsertingWaitingVehiclesBeforeDrivingVehicles = false; "
                    + "will be changed to true!");
        }

        config.getQSimConfigGroup().setInsertingWaitingVehiclesBeforeDrivingVehicles(true);
        return config;
    }
}
