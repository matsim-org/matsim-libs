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

package org.matsim.contrib.dynagent.run;

import org.apache.log4j.Logger;
import org.matsim.core.config.Config;
import org.matsim.core.config.consistency.ConfigConsistencyChecker;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.utils.misc.Time;


public class DynQSimConfigConsistencyChecker
    implements ConfigConsistencyChecker
{
    private static final Logger log = Logger.getLogger(DynQSimConfigConsistencyChecker.class);


    @Override
    public void checkConsistency(Config config)
    {
        QSimConfigGroup qSimConfig = config.qsim();

        if (qSimConfig.getStartTime() != 0 && qSimConfig.getStartTime() != Time.UNDEFINED_TIME) {
            log.warn("Simulation should start from time 0. "
                    + "This is what a typical DynAgent assumes");
        }

        if (qSimConfig.getSimStarttimeInterpretation() != //
        QSimConfigGroup.StarttimeInterpretation.onlyUseStarttime) {
            throw new RuntimeException("DynAgents require simulation from the very beginning,"
                    + "preferably sec-by-sec from time 0.");
        }
    }
}
