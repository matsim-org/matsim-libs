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
import static playground.michalm.taxi.run.AlgorithmConfig.AlgorithmType.*;

import java.util.EnumSet;

import org.matsim.contrib.dvrp.MatsimVrpContext;
import org.matsim.contrib.dvrp.router.VrpPathCalculator;
import org.matsim.contrib.dvrp.run.VrpLauncherUtils.TravelDisutilitySource;
import org.matsim.contrib.dvrp.run.VrpLauncherUtils.TravelTimeSource;

import playground.michalm.taxi.optimizer.TaxiOptimizer;
import playground.michalm.taxi.optimizer.assignment.APSTaxiOptimizer;
import playground.michalm.taxi.optimizer.immediaterequest.*;
import playground.michalm.taxi.optimizer.query.*;
import playground.michalm.taxi.vehreqpath.VehicleRequestPathFinder;


/*package*/enum AlgorithmConfig
{
    NOS_SL(//
            FREE_FLOW_SPEED, // does not matter (since ttCost: DISTANCE)
            STRAIGHT_LINE, // ????? Let's assume that taxi drivers choose the shortest-length path!!!
            NO_SCHEDULING), //

    NOS_TD(//
            FREE_FLOW_SPEED, // does not matter (since ttCost: DISTANCE)
            DISTANCE, //
            NO_SCHEDULING), //

    NOS_FF(//
            FREE_FLOW_SPEED, //
            TIME, //
            NO_SCHEDULING), //

    NOS_24H(//
            EVENTS_24_H, //
            TIME, //
            NO_SCHEDULING), //

    NOS_15M(//
            EVENTS_15_MIN, //
            TIME, //
            NO_SCHEDULING), //

    NOS_DSE_SL(//
            FREE_FLOW_SPEED, // does not matter (since ttCost: DISTANCE)
            STRAIGHT_LINE, // ????? Let's assume that taxi drivers choose the shortest-length path!!!
            NO_SCHEDULING_DEMAND_SUPPLY_EQUILIBRIUM), //

    NOS_DSE_TD(//
            FREE_FLOW_SPEED, // does not matter (since ttCost: DISTANCE)
            DISTANCE, //
            NO_SCHEDULING_DEMAND_SUPPLY_EQUILIBRIUM), //

    NOS_DSE_FF(//
            FREE_FLOW_SPEED, //
            TIME, //
            NO_SCHEDULING_DEMAND_SUPPLY_EQUILIBRIUM), //

    NOS_DSE_24H(//
            EVENTS_24_H, //
            TIME, //
            NO_SCHEDULING_DEMAND_SUPPLY_EQUILIBRIUM), //

    NOS_DSE_15M(//
            EVENTS_15_MIN, //
            TIME, //
            NO_SCHEDULING_DEMAND_SUPPLY_EQUILIBRIUM), //

    OTS_FF(//
            FREE_FLOW_SPEED, //
            TIME, //
            ONE_TIME_SCHEDULING), //

    OTS_24H(//
            EVENTS_24_H, //
            TIME, //
            ONE_TIME_SCHEDULING), //

    OTS_15M(//
            EVENTS_15_MIN, //
            TIME, //
            ONE_TIME_SCHEDULING), //

    RES_FF(//
            FREE_FLOW_SPEED, //
            TIME, //
            RE_SCHEDULING), //

    RES_24H(//
            EVENTS_24_H, //
            TIME, //
            RE_SCHEDULING), //

    RES_15M(//
            EVENTS_15_MIN, //
            TIME, //
            RE_SCHEDULING), //

    APS_FF(//
            FREE_FLOW_SPEED, //
            TIME, //
            AP_SCHEDULING), //

    APS_24H(//
            EVENTS_24_H, //
            TIME, //
            AP_SCHEDULING), //

    APS_15M(//
            EVENTS_15_MIN, //
            TIME, //
            AP_SCHEDULING), //

    APS_DSE_FF(//
            FREE_FLOW_SPEED, //
            TIME, //
            AP_SCHEDULING_DEMAND_SUPPLY_EQUILIBRIUM), //

    APS_DSE_24H(//
            EVENTS_24_H, //
            TIME, //
            AP_SCHEDULING_DEMAND_SUPPLY_EQUILIBRIUM), //

    APS_DSE_15M(//
            EVENTS_15_MIN, //
            TIME, //
            AP_SCHEDULING_DEMAND_SUPPLY_EQUILIBRIUM);//

    /*package*/static final EnumSet<AlgorithmConfig> NOS = EnumSet.of(NOS_SL, NOS_TD, NOS_FF,
            NOS_24H, NOS_15M, NOS_DSE_SL, NOS_DSE_TD, NOS_DSE_FF, NOS_DSE_24H, NOS_DSE_15M);

    /*package*/static final EnumSet<AlgorithmConfig> NON_NOS = EnumSet.complementOf(NOS);


    /*package*/static enum AlgorithmType
    {
        NO_SCHEDULING, //
        NO_SCHEDULING_DEMAND_SUPPLY_EQUILIBRIUM, //
        ONE_TIME_SCHEDULING, //
        RE_SCHEDULING, //
        AP_SCHEDULING, //
        AP_SCHEDULING_DEMAND_SUPPLY_EQUILIBRIUM;
    }


    /*package*/final TravelTimeSource ttimeSource;
    /*package*/final TravelDisutilitySource tdisSource;
    /*package*/final AlgorithmType algorithmType;


    /*package*/AlgorithmConfig(TravelTimeSource ttimeSource, TravelDisutilitySource tdisSource,
            AlgorithmType algorithmType)
    {
        this.ttimeSource = ttimeSource;
        this.tdisSource = tdisSource;
        this.algorithmType = algorithmType;
    }


    /*package*/TaxiOptimizer createTaxiOptimizer(MatsimVrpContext context,
            VrpPathCalculator calculator, ImmediateRequestParams params)
    {
        TaxiScheduler scheduler = new TaxiScheduler(context, calculator, params);
        VehicleRequestPathFinder vrpFinder = new VehicleRequestPathFinder(calculator, scheduler);

        OptimizerConfiguration optimConfig = new OptimizerConfiguration(context, params,
                calculator, scheduler, vrpFinder);

        switch (algorithmType) {
            case NO_SCHEDULING:
                return new NOSTaxiOptimizer(optimConfig,
                        new StraightLineNearestVehicleFinder(optimConfig.scheduler),
                        new StraightLineNearestRequestFinder(optimConfig.scheduler),
                        false);

            case NO_SCHEDULING_DEMAND_SUPPLY_EQUILIBRIUM:
                return new NOSTaxiOptimizer(optimConfig,
                        new StraightLineNearestVehicleFinder(optimConfig.scheduler),
                        new StraightLineNearestRequestFinder(optimConfig.scheduler),
                        true);

            case ONE_TIME_SCHEDULING:
                return new OTSTaxiOptimizer(optimConfig);

            case RE_SCHEDULING:
                return new RESTaxiOptimizer(optimConfig);

            case AP_SCHEDULING:
                return new APSTaxiOptimizer(optimConfig, false);

            case AP_SCHEDULING_DEMAND_SUPPLY_EQUILIBRIUM:
                return new APSTaxiOptimizer(optimConfig, true);

            default:
                throw new IllegalStateException();
        }
    }
}
