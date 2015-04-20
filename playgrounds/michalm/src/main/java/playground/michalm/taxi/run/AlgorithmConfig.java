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
import playground.michalm.taxi.optimizer.assignment.AssignmentTaxiOptimizer;
import playground.michalm.taxi.optimizer.fifo.FifoTaxiOptimizer;
import playground.michalm.taxi.optimizer.mip.MIPTaxiOptimizer;
import playground.michalm.taxi.optimizer.rules.RuleBasedTaxiOptimizer;


enum AlgorithmConfig
{
    RULE_TW_TD(RULES, MIN_WAIT_TIME, FREE_FLOW_SPEED, DISTANCE),

    RULE_TW_FF(RULES, MIN_WAIT_TIME, FREE_FLOW_SPEED, TIME),

    RULE_TW_15M(RULES, MIN_WAIT_TIME, EVENTS, TIME),

    RULE_TP_TD(RULES, MIN_PICKUP_TIME, FREE_FLOW_SPEED, DISTANCE),

    RULE_TP_FF(RULES, MIN_PICKUP_TIME, FREE_FLOW_SPEED, TIME),

    RULE_TP_15M(RULES, MIN_PICKUP_TIME, EVENTS, TIME),

    RULE_DSE_TD(RULES, DEMAND_SUPPLY_EQUIL, FREE_FLOW_SPEED, DISTANCE),

    RULE_DSE_FF(RULES, DEMAND_SUPPLY_EQUIL, FREE_FLOW_SPEED, TIME),

    RULE_DSE_15M(RULES, DEMAND_SUPPLY_EQUIL, EVENTS, TIME),

    FIFO_1_S_TW_FF(FIFO_1_SCHEDULING, MIN_WAIT_TIME, FREE_FLOW_SPEED, TIME),

    FIFO_1_S_TW_15M(FIFO_1_SCHEDULING, MIN_WAIT_TIME, EVENTS, TIME),

    FIFO_1_S_TP_FF(FIFO_1_SCHEDULING, MIN_PICKUP_TIME, FREE_FLOW_SPEED, TIME),

    FIFO_1_S_TP_15M(FIFO_1_SCHEDULING, MIN_PICKUP_TIME, EVENTS, TIME),

    FIFO_RES_TW_FF(FIFO_FIFO_RESCHEDULING, MIN_WAIT_TIME, FREE_FLOW_SPEED, TIME),

    FIFO_RES_TW_15M(FIFO_FIFO_RESCHEDULING, MIN_WAIT_TIME, EVENTS, TIME),

    FIFO_RES_TP_FF(FIFO_FIFO_RESCHEDULING, MIN_PICKUP_TIME, FREE_FLOW_SPEED, TIME),

    FIFO_RES_TP_15M(FIFO_FIFO_RESCHEDULING, MIN_PICKUP_TIME, EVENTS, TIME),

    ASSIGN_TW_FF(ASSIGNMENT, MIN_WAIT_TIME, FREE_FLOW_SPEED, TIME),

    ASSIGN_TW_15M(ASSIGNMENT, MIN_WAIT_TIME, EVENTS, TIME),

    ASSIGN_TP_FF(ASSIGNMENT, MIN_PICKUP_TIME, FREE_FLOW_SPEED, TIME),

    ASSIGN_TP_15M(ASSIGNMENT, MIN_PICKUP_TIME, EVENTS, TIME),

    ASSIGN_DSE_FF(ASSIGNMENT, DEMAND_SUPPLY_EQUIL, FREE_FLOW_SPEED, TIME),

    ASSIGN_DSE_15M(ASSIGNMENT, DEMAND_SUPPLY_EQUIL, EVENTS, TIME),

    MIP_TW_FF(MIP, MIN_WAIT_TIME, FREE_FLOW_SPEED, TIME);

    static enum AlgorithmType
    {
        RULES, //
        FIFO_1_SCHEDULING, //
        FIFO_FIFO_RESCHEDULING, //
        ASSIGNMENT, //
        MIP;
    }


    final AlgorithmType algorithmType;
    final Goal goal;
    final TravelTimeSource ttimeSource;
    final TravelDisutilitySource tdisSource;


    AlgorithmConfig(AlgorithmType algorithmType, Goal goal, TravelTimeSource ttimeSource,
            TravelDisutilitySource tdisSource)
    {
        this.algorithmType = algorithmType;
        this.goal = goal;
        this.ttimeSource = ttimeSource;
        this.tdisSource = tdisSource;
    }


    TaxiOptimizer createTaxiOptimizer(TaxiOptimizerConfiguration optimConfig)
    {
        switch (algorithmType) {
            case RULES:
                return new RuleBasedTaxiOptimizer(optimConfig);

            case FIFO_1_SCHEDULING:
                return FifoTaxiOptimizer.createOptimizerWithoutRescheduling(optimConfig);

            case FIFO_FIFO_RESCHEDULING:
                return FifoTaxiOptimizer.createOptimizerWithRescheduling(optimConfig);

            case ASSIGNMENT:
                return new AssignmentTaxiOptimizer(optimConfig);

            case MIP:
                return new MIPTaxiOptimizer(optimConfig);

            default:
                throw new IllegalStateException();
        }
    }
}
