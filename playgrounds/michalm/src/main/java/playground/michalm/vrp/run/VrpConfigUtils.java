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
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.trafficmonitoring.TravelTimeCalculatorConfigGroup;


public class VrpConfigUtils
{
    public static Config createConfig()
    {
        Config config = ConfigUtils.createConfig();
        
        updateQSimConfigGroup(config);
        updateTravelTimeCalculatorConfigGroup(config);
        
        return config;
    }


    public static Config loadConfig(final String filename)
    {
        @SuppressWarnings("unchecked")
        Config config = ConfigUtils.loadConfig(filename);

        updateQSimConfigGroup(config);
        updateTravelTimeCalculatorConfigGroup(config);
        
        return config;
    }


    /**
     * Dynamic Taxi (and other VRP's) are designed for and validated against these QSimConfigGroup
     * settings only.
     */
    private static void updateQSimConfigGroup(Config config)
    {
        QSimConfigGroup qSimConfig = config.getQSimConfigGroup();

        if (qSimConfig == null) {
            qSimConfig = new QSimConfigGroup();
            config.addQSimConfigGroup(qSimConfig);
        }

        qSimConfig.setInsertingWaitingVehiclesBeforeDrivingVehicles(true);
        qSimConfig.setSnapshotStyle(QSimConfigGroup.SNAPSHOT_AS_QUEUE);
        qSimConfig.setRemoveStuckVehicles(false);
        qSimConfig.setStartTime(0);
        qSimConfig.setSimStarttimeInterpretation(QSimConfigGroup.ONLY_USE_STARTTIME);
    }
    

    private static void updateTravelTimeCalculatorConfigGroup(Config config)
    {
        TravelTimeCalculatorConfigGroup configGroup = config.travelTimeCalculator();
        //configGroup.setTravelTimeGetterType("linearinterpolation");
    }
}
