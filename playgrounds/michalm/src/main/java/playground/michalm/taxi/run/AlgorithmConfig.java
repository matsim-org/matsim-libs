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
import static playground.michalm.taxi.optimizer.immediaterequest.TaxiOptimizationPolicy.*;
import static playground.michalm.taxi.run.AlgorithmConfig.AlgorithmType.*;

import org.matsim.contrib.dvrp.MatsimVrpContext;
import org.matsim.contrib.dvrp.router.VrpPathCalculator;
import org.matsim.contrib.dvrp.run.VrpLauncherUtils.TravelDisutilitySource;
import org.matsim.contrib.dvrp.run.VrpLauncherUtils.TravelTimeSource;

import playground.michalm.taxi.optimizer.immediaterequest.*;


/*package*/class AlgorithmConfig
{
    /*package*/static enum AlgorithmType
    {
        NO_SCHEDULING("NOS"), // only idle vehicles
        NO_SCHEDULING_BEST_REQUEST("NOS-BR"), //
        ONE_TIME_SCHEDULING("OTS"), // formerly "optimistic"
        RE_SCHEDULING("RES"); // formerly "pessimistic"

        /*package*/final String shortcut;


        private AlgorithmType(String shortcut)
        {
            this.shortcut = shortcut;
        }
    }


    /*package*/static final AlgorithmConfig NOS_STRAIGHT_LINE = new AlgorithmConfig(//
            FREE_FLOW_SPEED, // does not matter (since ttCost: DISTANCE)
            DISTANCE, // ????? Let's assume that taxi drivers choose the shortest-length path!!!
            NO_SCHEDULING,//
            AFTER_REQUEST);//

    /*package*/static final AlgorithmConfig NOS_TRAVEL_DISTANCE = new AlgorithmConfig(//
            FREE_FLOW_SPEED, // does not matter (since ttCost: DISTANCE)
            DISTANCE, //
            NO_SCHEDULING,//
            AFTER_REQUEST);//

    /*package*/static final AlgorithmConfig NOS_FREE_FLOW = new AlgorithmConfig(//
            FREE_FLOW_SPEED, //
            TIME, //
            NO_SCHEDULING,//
            AFTER_REQUEST);//

    /*package*/static final AlgorithmConfig NOS_24_H = new AlgorithmConfig(//
            EVENTS_24_H, //
            TIME, //
            NO_SCHEDULING,//
            AFTER_REQUEST);//

    /*package*/static final AlgorithmConfig NOS_15_MIN = new AlgorithmConfig(//
            EVENTS_15_MIN, //
            TIME, //
            NO_SCHEDULING,//
            AFTER_REQUEST);//

    /*package*/static final AlgorithmConfig OTS_REQ_FREE_FLOW = new AlgorithmConfig(//
            FREE_FLOW_SPEED, //
            TIME, //
            ONE_TIME_SCHEDULING,//
            AFTER_REQUEST);//

    /*package*/static final AlgorithmConfig OTS_REQ_24_H = new AlgorithmConfig(//
            EVENTS_24_H, //
            TIME, //
            ONE_TIME_SCHEDULING,//
            AFTER_REQUEST);//

    /*package*/static final AlgorithmConfig OTS_REQ_15_MIN = new AlgorithmConfig(//
            EVENTS_15_MIN, //
            TIME, //
            ONE_TIME_SCHEDULING,//
            AFTER_REQUEST);//

    /*package*/static final AlgorithmConfig OTS_DRV_FREE_FLOW = new AlgorithmConfig(//
            FREE_FLOW_SPEED, //
            TIME, //
            ONE_TIME_SCHEDULING,//
            AFTER_DRIVE_TASKS);//

    /*package*/static final AlgorithmConfig OTS_DRV_24_H = new AlgorithmConfig(//
            EVENTS_24_H, //
            TIME, //
            ONE_TIME_SCHEDULING,//
            AFTER_DRIVE_TASKS);//

    /*package*/static final AlgorithmConfig OTS_DRV_15_MIN = new AlgorithmConfig(//
            EVENTS_15_MIN, //
            TIME, //
            ONE_TIME_SCHEDULING,//
            AFTER_DRIVE_TASKS);//

    /*package*/static final AlgorithmConfig RES_REQ_FREE_FLOW = new AlgorithmConfig(//
            FREE_FLOW_SPEED, //
            TIME, //
            RE_SCHEDULING,//
            AFTER_REQUEST);//

    /*package*/static final AlgorithmConfig RES_REQ_24_H = new AlgorithmConfig(//
            EVENTS_24_H, //
            TIME, //
            RE_SCHEDULING,//
            AFTER_REQUEST);//

    /*package*/static final AlgorithmConfig RES_REQ_15_MIN = new AlgorithmConfig(//
            EVENTS_15_MIN, //
            TIME, //
            RE_SCHEDULING,//
            AFTER_REQUEST);//

    /*package*/static final AlgorithmConfig RES_DRV_FREE_FLOW = new AlgorithmConfig(//
            FREE_FLOW_SPEED, //
            TIME, //
            RE_SCHEDULING,//
            AFTER_DRIVE_TASKS);//

    /*package*/static final AlgorithmConfig RES_DRV_24_H = new AlgorithmConfig(//
            EVENTS_24_H, //
            TIME, //
            RE_SCHEDULING,//
            AFTER_DRIVE_TASKS);//

    /*package*/static final AlgorithmConfig RES_DRV_15_MIN = new AlgorithmConfig(//
            EVENTS_15_MIN, //
            TIME, //
            RE_SCHEDULING,//
            AFTER_DRIVE_TASKS);//

    /*package*/static final AlgorithmConfig[] ALL = {//
    NOS_STRAIGHT_LINE,//
            NOS_TRAVEL_DISTANCE,//
            NOS_FREE_FLOW,//
            NOS_24_H,//
            NOS_15_MIN,//
            OTS_REQ_FREE_FLOW,//
            OTS_REQ_24_H,//
            OTS_REQ_15_MIN,//
            OTS_DRV_FREE_FLOW,//
            OTS_DRV_24_H,//
            OTS_DRV_15_MIN,//
            RES_REQ_FREE_FLOW,//
            RES_REQ_24_H,//
            RES_REQ_15_MIN,//
            RES_DRV_FREE_FLOW,//
            RES_DRV_24_H,//
            RES_DRV_15_MIN //
    };

    /*package*/final TravelTimeSource ttimeSource;
    /*package*/final TravelDisutilitySource tdisSource;
    /*package*/final AlgorithmType algorithmType;
    /*package*/final TaxiOptimizationPolicy optimizationPolicy;


    /*package*/AlgorithmConfig(TravelTimeSource ttimeSource, TravelDisutilitySource tdisSource,
            AlgorithmType algorithmType, TaxiOptimizationPolicy optimizationPolicy)
    {
        this.ttimeSource = ttimeSource;
        this.tdisSource = tdisSource;
        this.algorithmType = algorithmType;
        this.optimizationPolicy = optimizationPolicy;
    }


    /*package*/ImmediateRequestTaxiOptimizer createTaxiOptimizer(MatsimVrpContext context,
            VrpPathCalculator calculator, ImmediateRequestParams params)
    {
        switch (algorithmType) {
            case NO_SCHEDULING:
                return new NOSTaxiOptimizer(context, calculator, params, new IdleVehicleFinder(
                        context, calculator, this == NOS_STRAIGHT_LINE), false);

            case NO_SCHEDULING_BEST_REQUEST:
                return new NOSTaxiOptimizer(context, calculator, params, new IdleVehicleFinder(
                        context, calculator, this == NOS_STRAIGHT_LINE), true);

            case ONE_TIME_SCHEDULING:
                return new OTSTaxiOptimizer(context, calculator, params, optimizationPolicy);

            case RE_SCHEDULING:
                return new RESTaxiOptimizer(context, calculator, params, optimizationPolicy);

            default:
                throw new IllegalStateException();
        }
    }
}
