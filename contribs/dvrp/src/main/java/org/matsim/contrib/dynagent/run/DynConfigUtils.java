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

package org.matsim.contrib.dynagent.run;

import org.matsim.core.config.*;
import org.matsim.core.config.groups.QSimConfigGroup;


public class DynConfigUtils
{
    public static Config createConfig()
    {
        Config config = ConfigUtils.createConfig();
        updateQSimConfigGroup(config);
        return config;
    }


    public static Config loadConfig(final String file)
    {
        Config config = ConfigUtils.loadConfig(file);
        updateQSimConfigGroup(config);
        return config;
    }


    private static void updateQSimConfigGroup(Config config)
    {
        QSimConfigGroup qSimConfig = config.qsim();
        qSimConfig.setInsertingWaitingVehiclesBeforeDrivingVehicles(true);
        qSimConfig.setSnapshotStyle(QSimConfigGroup.SNAPSHOT_AS_QUEUE);
        qSimConfig.setRemoveStuckVehicles(false);
        qSimConfig.setStartTime(0);
        qSimConfig.setSimStarttimeInterpretation(QSimConfigGroup.ONLY_USE_STARTTIME);
    }
}
