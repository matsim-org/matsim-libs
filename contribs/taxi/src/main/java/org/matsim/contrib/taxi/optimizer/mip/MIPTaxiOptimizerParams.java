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

package org.matsim.contrib.taxi.optimizer.mip;

import org.apache.commons.configuration.Configuration;
import org.matsim.contrib.taxi.optimizer.AbstractTaxiOptimizerParams;


public class MIPTaxiOptimizerParams
    extends AbstractTaxiOptimizerParams
{
    public static final String FIND_START_SOLUTION = "findStartSolution";
    public static final String OPTIMIZE = "optimize";
    public static final String LOAD = "load";
    public static final String REQ_PER_VEH_PLANNING_HORIZON = "reqPerVehPlanningHorizon";


    public MIPTaxiOptimizerParams(Configuration optimizerConfig)
    {
        super(optimizerConfig);
        throw new UnsupportedOperationException("Unused temporarily");
    }
}
