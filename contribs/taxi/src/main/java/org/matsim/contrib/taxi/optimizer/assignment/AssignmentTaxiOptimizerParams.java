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

package org.matsim.contrib.taxi.optimizer.assignment;

import org.apache.commons.configuration.Configuration;
import org.matsim.contrib.taxi.optimizer.*;
import org.matsim.contrib.taxi.optimizer.assignment.AssignmentProblem.Mode;


public class AssignmentTaxiOptimizerParams
    extends AbstractTaxiOptimizerParams
{
    public static final String MODE = "mode";
    public static final String NULL_PATH_COST = "nullPathCost";

    public static final String VEH_PLANNING_HORIZON_OVERSUPPLY = "vehPlanningHorizonOversupply";
    public static final String VEH_PLANNING_HORIZON_UNDERSUPPLY = "vehPlanningHorizonUndersupply";

    public static final String NEAREST_REQUESTS_LIMIT = "nearestRequestsLimit";
    public static final String NEAREST_VEHICLES_LIMIT = "nearestVehiclesLimit";

    public final Mode mode;
    public final double nullPathCost;

    public final double vehPlanningHorizonOversupply;// 120 s used in the IEEE IS paper
    public final double vehPlanningHorizonUndersupply;// 30 s used in the IEEE IS paper

    public final int nearestRequestsLimit;
    public final int nearestVehiclesLimit;


    public AssignmentTaxiOptimizerParams(Configuration optimizerConfig)
    {
        super(optimizerConfig);

        mode = Mode.valueOf(optimizerConfig.getString(MODE));

        // when the cost is measured in time units (seconds),
        //48 * 36000 s (2 days) seem big enough to prevent such assignments
        nullPathCost = optimizerConfig.getDouble(NULL_PATH_COST, 48 * 3600);

        vehPlanningHorizonOversupply = optimizerConfig.getInt(VEH_PLANNING_HORIZON_OVERSUPPLY);
        vehPlanningHorizonUndersupply = optimizerConfig.getInt(VEH_PLANNING_HORIZON_UNDERSUPPLY);

        nearestRequestsLimit = optimizerConfig.getInt(NEAREST_REQUESTS_LIMIT);
        nearestVehiclesLimit = optimizerConfig.getInt(NEAREST_VEHICLES_LIMIT);
    }


    @Override
    public AssignmentTaxiOptimizer createTaxiOptimizer(TaxiOptimizerContext optimContext)
    {
        return new AssignmentTaxiOptimizer(optimContext);
    }
}
