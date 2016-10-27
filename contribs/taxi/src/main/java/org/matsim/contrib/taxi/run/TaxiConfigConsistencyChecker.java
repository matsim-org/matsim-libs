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

package org.matsim.contrib.taxi.run;

import org.matsim.contrib.dvrp.run.VrpQSimConfigConsistencyChecker;
import org.matsim.core.config.Config;


public class TaxiConfigConsistencyChecker
    extends VrpQSimConfigConsistencyChecker
{
    @Override
    public void checkConsistency(Config config)
    {
        super.checkConsistency(config);

        TaxiConfigGroup taxiCfg = TaxiConfigGroup.get(config);
        if (taxiCfg.isVehicleDiversion() && !taxiCfg.isOnlineVehicleTracker()) {
            throw new RuntimeException("Diversion requires online tracking");
        }
    }
}
