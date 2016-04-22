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

package org.matsim.contrib.taxi.benchmark;

import org.apache.log4j.Logger;
import org.matsim.contrib.dvrp.run.VrpQSimConfigConsistencyChecker;
import org.matsim.core.config.Config;


public class TaxiBenchmarkConfigConsistencyChecker
    extends VrpQSimConfigConsistencyChecker
{
    private static final Logger log = Logger.getLogger(TaxiBenchmarkConfigConsistencyChecker.class);


    @Override
    public void checkConsistency(Config config)
    {
        super.checkConsistency(config);

        if (config.network().isTimeVariantNetwork()
                && config.network().getChangeEventsInputFile() == null) {
            log.warn("No change events provided for the time variant network");
        }

        if (!config.network().isTimeVariantNetwork()
                && config.network().getChangeEventsInputFile() != null) {
            log.warn("Change events ignored, because the network is not time variant");
        }

        if (config.qsim().getFlowCapFactor() < 100) {
            log.warn(
                    "FlowCapFactor should be large enough (e.g. 100) to obtain deterministic travel times");
        }
    }
}
