/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package org.matsim.contrib.taxi.optimizer;

import org.apache.commons.configuration.Configuration;


public class AbstractTaxiOptimizerParams
    implements TaxiOptimizerParams
{
    public static final String ID = "id";
    public static final String REOPTIMIZATION_TIME_STEP = "reoptimizationTimeStep";

    public final String id;//TODO is it used anywhere?

    //usually 1 s; however, the assignment strategy for TaxiBerlin used 10 s (IEEE IS paper)
    public final int reoptimizationTimeStep;


    protected AbstractTaxiOptimizerParams(Configuration optimizerConfig)
    {
        id = optimizerConfig.getString(ID);
        reoptimizationTimeStep = optimizerConfig.getInt(REOPTIMIZATION_TIME_STEP, 1);
    }
}
