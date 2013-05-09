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

package playground.michalm.vrp.run.online;

import static pl.poznan.put.vrp.dynamic.optimizer.taxi.immediaterequest.TaxiOptimizationPolicy.*;
import static playground.michalm.vrp.run.online.AlgorithmConfig.AlgorithmType.*;
import static playground.michalm.vrp.run.online.OnlineDvrpLauncherUtils.TravelCostSource.*;
import static playground.michalm.vrp.run.online.OnlineDvrpLauncherUtils.TravelTimeSource.*;
import pl.poznan.put.vrp.dynamic.data.VrpData;
import pl.poznan.put.vrp.dynamic.optimizer.taxi.immediaterequest.*;
import playground.michalm.vrp.run.online.OnlineDvrpLauncherUtils.TravelCostSource;
import playground.michalm.vrp.run.online.OnlineDvrpLauncherUtils.TravelTimeSource;


/*package*/class AlgorithmConfig
{
    /*package*/static enum AlgorithmType
    {
        NO_SCHEDULING, // only idle vehicles
        ONE_TIME_SCHEDULING, // formerly "optimistic"
        RE_SCHEDULING; // formerly "pessimistic"
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
    /*package*/final TravelCostSource tcostSource;
    /*package*/final AlgorithmType algorithmType;
    /*package*/final TaxiOptimizationPolicy optimizationPolicy;


    /*package*/AlgorithmConfig(TravelTimeSource ttimeSource, TravelCostSource tcostSource,
            AlgorithmType algorithmType, TaxiOptimizationPolicy optimizationPolicy)
    {
        this.ttimeSource = ttimeSource;
        this.tcostSource = tcostSource;
        this.algorithmType = algorithmType;
        this.optimizationPolicy = optimizationPolicy;
    }


    /*package*/ImmediateRequestTaxiOptimizer createTaxiOptimizer(VrpData data)
    {
        switch (algorithmType) {
            case NO_SCHEDULING:
                return new NOSTaxiOptimizerWithoutDestination(data, optimizationPolicy, this == NOS_STRAIGHT_LINE);

            case ONE_TIME_SCHEDULING:
                return new OTSTaxiOptimizerWithoutDestination(data, optimizationPolicy);

            case RE_SCHEDULING:
                return new RESTaxiOptimizerWithoutDestination(data, optimizationPolicy);

            default:
                throw new IllegalStateException();
        }
    }
}
