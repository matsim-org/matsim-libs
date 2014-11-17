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

package playground.michalm.taxi.run;

import static org.matsim.contrib.dvrp.run.VrpLauncherUtils.TravelDisutilitySource.*;
import static org.matsim.contrib.dvrp.run.VrpLauncherUtils.TravelTimeSource.*;
import static playground.michalm.taxi.optimizer.TaxiOptimizerConfiguration.Goal.*;
import static playground.michalm.taxi.run.AlgorithmConfig.AlgorithmType.*;

import org.matsim.contrib.dvrp.run.VrpLauncherUtils.TravelDisutilitySource;
import org.matsim.contrib.dvrp.run.VrpLauncherUtils.TravelTimeSource;

import playground.michalm.taxi.optimizer.*;
import playground.michalm.taxi.optimizer.TaxiOptimizerConfiguration.Goal;
import playground.michalm.taxi.optimizer.assignment.APSTaxiOptimizer;
import playground.michalm.taxi.optimizer.fifo.*;
import playground.michalm.taxi.optimizer.mip.MIPTaxiOptimizer;


enum AlgorithmConfig
{
    NOS_TW_TD(NO_SCHEDULING, MIN_WAIT_TIME, FREE_FLOW_SPEED, DISTANCE),

    NOS_TW_FF(NO_SCHEDULING, MIN_WAIT_TIME, FREE_FLOW_SPEED, TIME),

    NOS_TW_15M(NO_SCHEDULING, MIN_WAIT_TIME, EVENTS, TIME),

    NOS_TP_TD(NO_SCHEDULING, MIN_PICKUP_TIME, FREE_FLOW_SPEED, DISTANCE),

    NOS_TP_FF(NO_SCHEDULING, MIN_PICKUP_TIME, FREE_FLOW_SPEED, TIME),

    NOS_TP_15M(NO_SCHEDULING, MIN_PICKUP_TIME, EVENTS, TIME),

    NOS_DSE_TD(NO_SCHEDULING, DEMAND_SUPPLY_EQUIL, FREE_FLOW_SPEED, DISTANCE),

    NOS_DSE_FF(NO_SCHEDULING, DEMAND_SUPPLY_EQUIL, FREE_FLOW_SPEED, TIME),

    NOS_DSE_15M(NO_SCHEDULING, DEMAND_SUPPLY_EQUIL, EVENTS, TIME),

    OTS_TW_FF(ONE_TIME_SCHEDULING, MIN_WAIT_TIME, FREE_FLOW_SPEED, TIME),

    OTS_TW_15M(ONE_TIME_SCHEDULING, MIN_WAIT_TIME, EVENTS, TIME),

    OTS_TP_FF(ONE_TIME_SCHEDULING, MIN_PICKUP_TIME, FREE_FLOW_SPEED, TIME),

    OTS_TP_15M(ONE_TIME_SCHEDULING, MIN_PICKUP_TIME, EVENTS, TIME),

    RES_TW_FF(RE_SCHEDULING, MIN_WAIT_TIME, FREE_FLOW_SPEED, TIME),

    RES_TW_15M(RE_SCHEDULING, MIN_WAIT_TIME, EVENTS, TIME),

    RES_TP_FF(RE_SCHEDULING, MIN_PICKUP_TIME, FREE_FLOW_SPEED, TIME),

    RES_TP_15M(RE_SCHEDULING, MIN_PICKUP_TIME, EVENTS, TIME),

    APS_TW_FF(AP_SCHEDULING, MIN_WAIT_TIME, FREE_FLOW_SPEED, TIME),

    APS_TW_15M(AP_SCHEDULING, MIN_WAIT_TIME, EVENTS, TIME),

    APS_TP_FF(AP_SCHEDULING, MIN_PICKUP_TIME, FREE_FLOW_SPEED, TIME),

    APS_TP_15M(AP_SCHEDULING, MIN_PICKUP_TIME, EVENTS, TIME),

    APS_DSE_FF(AP_SCHEDULING, DEMAND_SUPPLY_EQUIL, FREE_FLOW_SPEED, TIME),

    APS_DSE_15M(AP_SCHEDULING, DEMAND_SUPPLY_EQUIL, EVENTS, TIME),

    MIP_TW_FF(MIP_SCHEDULING, MIN_WAIT_TIME, FREE_FLOW_SPEED, TIME);

    static enum AlgorithmType
    {
        NO_SCHEDULING, //
        ONE_TIME_SCHEDULING, //
        RE_SCHEDULING, //
        AP_SCHEDULING, //
        MIP_SCHEDULING;
    }


    final TravelTimeSource ttimeSource;
    final Goal goal;
    final TravelDisutilitySource tdisSource;
    final AlgorithmType algorithmType;


    AlgorithmConfig(AlgorithmType algorithmType, Goal goal, TravelTimeSource ttimeSource,
            TravelDisutilitySource tdisSource)
    {
        this.ttimeSource = ttimeSource;
        this.goal = goal;
        this.tdisSource = tdisSource;
        this.algorithmType = algorithmType;
    }


    TaxiOptimizer createTaxiOptimizer(TaxiOptimizerConfiguration optimConfig)
    {
        switch (algorithmType) {
            case NO_SCHEDULING:
                return new NOSTaxiOptimizer(optimConfig);

            case ONE_TIME_SCHEDULING:
                return new OTSTaxiOptimizer(optimConfig);

            case RE_SCHEDULING:
                return new RESTaxiOptimizer(optimConfig);

            case AP_SCHEDULING:
                return new APSTaxiOptimizer(optimConfig);

            case MIP_SCHEDULING:
                return new MIPTaxiOptimizer(optimConfig);

            default:
                throw new IllegalStateException();
        }
    }
}
