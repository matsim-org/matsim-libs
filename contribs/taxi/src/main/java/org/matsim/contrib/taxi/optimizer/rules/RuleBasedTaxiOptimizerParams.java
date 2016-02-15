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

package org.matsim.contrib.taxi.optimizer.rules;

import org.apache.commons.configuration.Configuration;
import org.matsim.contrib.taxi.optimizer.*;
import org.matsim.contrib.taxi.optimizer.rules.RuleBasedTaxiOptimizer.Goal;


public class RuleBasedTaxiOptimizerParams
    extends AbstractTaxiOptimizerParams
{
    public static final String GOAL = "goal";

    public static final String NEAREST_REQUESTS_LIMIT = "nearestRequestsLimit";
    public static final String NEAREST_VEHICLES_LIMIT = "nearestVehiclesLimit";

    public static final String CELL_SIZE = "cellSize";

    public final Goal goal;

    public final int nearestRequestsLimit;
    public final int nearestVehiclesLimit;

    public final double cellSize;


    public RuleBasedTaxiOptimizerParams(Configuration optimizerConfig)
    {
        super(optimizerConfig);

        goal = Goal.valueOf(optimizerConfig.getString(GOAL));

        nearestRequestsLimit = optimizerConfig.getInt(NEAREST_REQUESTS_LIMIT);
        nearestVehiclesLimit = optimizerConfig.getInt(NEAREST_VEHICLES_LIMIT);

        cellSize = optimizerConfig.getDouble(CELL_SIZE);//1000 m tested for Berlin
    }


    @Override
    public RuleBasedTaxiOptimizer createTaxiOptimizer(TaxiOptimizerContext optimContext)
    {
        return new RuleBasedTaxiOptimizer(optimContext);
    }

}
