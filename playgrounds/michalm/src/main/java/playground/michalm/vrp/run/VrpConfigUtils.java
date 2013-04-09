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


public class VrpConfigUtils
{
    public static Config createConfig()
    {
        Config config = ConfigUtils.createConfig();
        QSimConfigGroup qSimConfigGroup = new QSimConfigGroup();
        qSimConfigGroup.setInsertingWaitingVehiclesBeforeDrivingVehicles(true);
        config.addQSimConfigGroup(qSimConfigGroup);
        return config;
    }


    public static Config loadConfig(final String filename)
    {
        Config config = ConfigUtils.loadConfig(filename);
        QSimConfigGroup qSimConfigGroup = config.getQSimConfigGroup();

        if (qSimConfigGroup == null) {
            qSimConfigGroup = new QSimConfigGroup();
        }
        else if (!qSimConfigGroup.isInsertingWaitingVehiclesBeforeDrivingVehicles()) {
            System.err.println("isInsertingWaitingVehiclesBeforeDrivingVehicles was FALSE; "
                    + "has been changed to TRUE!");
        }

        config.getQSimConfigGroup().setInsertingWaitingVehiclesBeforeDrivingVehicles(true);
        return config;
    }
}
