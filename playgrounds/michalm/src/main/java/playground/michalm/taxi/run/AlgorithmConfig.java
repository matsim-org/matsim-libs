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

import org.matsim.contrib.dvrp.MatsimVrpContext;
import org.matsim.contrib.dvrp.router.VrpPathCalculator;
import org.matsim.contrib.dvrp.run.VrpLauncherUtils.TravelDisutilitySource;
import org.matsim.contrib.dvrp.run.VrpLauncherUtils.TravelTimeSource;

import playground.michalm.taxi.optimizer.assignment.APSTaxiOptimizer;
import playground.michalm.taxi.optimizer.immediaterequest.*;


/*package*/class AlgorithmConfig
{
    /*package*/static enum AlgorithmType
    {
        NO_SCHEDULING("NOS"), //
        NO_SCHEDULING_DEMAND_SUPPLY_EQUILIBRIUM("NOS_DS_EQ"), //
        ONE_TIME_SCHEDULING("OTS"), //
        RE_SCHEDULING("RES"),//
        AP_SCHEDULING("APS");

        /*package*/final String shortcut;


        private AlgorithmType(String shortcut)
        {
            this.shortcut = shortcut;
        }
    }


    /*package*/static final AlgorithmConfig NOS_STRAIGHT_LINE = new AlgorithmConfig(//
            "NOS_STRAIGHT_LINE",//
            FREE_FLOW_SPEED, // does not matter (since ttCost: DISTANCE)
            DISTANCE, // ????? Let's assume that taxi drivers choose the shortest-length path!!!
            NO_SCHEDULING);//

    /*package*/static final AlgorithmConfig NOS_TRAVEL_DISTANCE = new AlgorithmConfig(//
            "NOS_TRAVEL_DISTANCE", //
            FREE_FLOW_SPEED, // does not matter (since ttCost: DISTANCE)
            DISTANCE, //
            NO_SCHEDULING);//

    /*package*/static final AlgorithmConfig NOS_FREE_FLOW = new AlgorithmConfig(//
            "NOS_FREE_FLOW", //
            FREE_FLOW_SPEED, //
            TIME, //
            NO_SCHEDULING);//

    /*package*/static final AlgorithmConfig NOS_24_H = new AlgorithmConfig(//
            "NOS_24_H", //
            EVENTS_24_H, //
            TIME, //
            NO_SCHEDULING);//

    /*package*/static final AlgorithmConfig NOS_15_MIN = new AlgorithmConfig(//
            "NOS_15_MIN", //
            EVENTS_15_MIN, //
            TIME, //
            NO_SCHEDULING);//

    /*package*/static final AlgorithmConfig NOS_DS_EQ_STRAIGHT_LINE = new AlgorithmConfig(//
            "NOS_DS_EQ_STRAIGHT_LINE",//
            FREE_FLOW_SPEED, // does not matter (since ttCost: DISTANCE)
            DISTANCE, // ????? Let's assume that taxi drivers choose the shortest-length path!!!
            NO_SCHEDULING_DEMAND_SUPPLY_EQUILIBRIUM);//

    /*package*/static final AlgorithmConfig NOS_DS_EQ_TRAVEL_DISTANCE = new AlgorithmConfig(//
            "NOS_DS_EQ_TRAVEL_DISTANCE", //
            FREE_FLOW_SPEED, // does not matter (since ttCost: DISTANCE)
            DISTANCE, //
            NO_SCHEDULING_DEMAND_SUPPLY_EQUILIBRIUM);//

    /*package*/static final AlgorithmConfig NOS_DS_EQ_FREE_FLOW = new AlgorithmConfig(//
            "NOS_DS_EQ_FREE_FLOW", //
            FREE_FLOW_SPEED, //
            TIME, //
            NO_SCHEDULING_DEMAND_SUPPLY_EQUILIBRIUM);//

    /*package*/static final AlgorithmConfig NOS_DS_EQ_24_H = new AlgorithmConfig(//
            "NOS_DS_EQ_24_H", //
            EVENTS_24_H, //
            TIME, //
            NO_SCHEDULING_DEMAND_SUPPLY_EQUILIBRIUM);//

    /*package*/static final AlgorithmConfig NOS_DS_EQ_15_MIN = new AlgorithmConfig(//
            "NOS_DS_EQ_15_MIN", //
            EVENTS_15_MIN, //
            TIME, //
            NO_SCHEDULING_DEMAND_SUPPLY_EQUILIBRIUM);//

    /*package*/static final AlgorithmConfig OTS_FREE_FLOW = new AlgorithmConfig(//
            "OTS_FREE_FLOW", //
            FREE_FLOW_SPEED, //
            TIME, //
            ONE_TIME_SCHEDULING);//

    /*package*/static final AlgorithmConfig OTS_24_H = new AlgorithmConfig(//
            "OTS_24_H", //
            EVENTS_24_H, //
            TIME, //
            ONE_TIME_SCHEDULING);//

    /*package*/static final AlgorithmConfig OTS_15_MIN = new AlgorithmConfig(//
            "OTS_15_MIN", //
            EVENTS_15_MIN, //
            TIME, //
            ONE_TIME_SCHEDULING);//

    /*package*/static final AlgorithmConfig RES_FREE_FLOW = new AlgorithmConfig(//
            "RES_FREE_FLOW", //
            FREE_FLOW_SPEED, //
            TIME, //
            RE_SCHEDULING);//

    /*package*/static final AlgorithmConfig RES_24_H = new AlgorithmConfig(//
            "RES_24_H", //
            EVENTS_24_H, //
            TIME, //
            RE_SCHEDULING);//

    /*package*/static final AlgorithmConfig RES_15_MIN = new AlgorithmConfig(//
            "RES_15_MIN", //
            EVENTS_15_MIN, //
            TIME, //
            RE_SCHEDULING);//

    /*package*/static final AlgorithmConfig APS_FREE_FLOW = new AlgorithmConfig(//
            "APS_FREE_FLOW", //
            FREE_FLOW_SPEED, //
            TIME, //
            AP_SCHEDULING);//

    /*package*/static final AlgorithmConfig APS_24_H = new AlgorithmConfig(//
            "APS_24_H", //
            EVENTS_24_H, //
            TIME, //
            AP_SCHEDULING);//

    /*package*/static final AlgorithmConfig APS_15_MIN = new AlgorithmConfig(//
            "APS_15_MIN", //
            EVENTS_15_MIN, //
            TIME, //
            AP_SCHEDULING);//

    /*package*/static final AlgorithmConfig[] ALL = {//
    NOS_STRAIGHT_LINE,//
            NOS_TRAVEL_DISTANCE,//
            NOS_FREE_FLOW,//
            NOS_24_H,//
            NOS_15_MIN,//
            NOS_DS_EQ_STRAIGHT_LINE,//
            NOS_DS_EQ_TRAVEL_DISTANCE,//
            NOS_DS_EQ_FREE_FLOW,//
            NOS_DS_EQ_24_H,//
            NOS_DS_EQ_15_MIN,//
            OTS_FREE_FLOW,//
            OTS_24_H,//
            OTS_15_MIN,//
            RES_FREE_FLOW,//
            RES_24_H,//
            RES_15_MIN, //
            APS_FREE_FLOW,//
            APS_24_H,//
            APS_15_MIN //
    };

    /*package*/final String name;
    /*package*/final TravelTimeSource ttimeSource;
    /*package*/final TravelDisutilitySource tdisSource;
    /*package*/final AlgorithmType algorithmType;


    /*package*/AlgorithmConfig(String name, TravelTimeSource ttimeSource,
            TravelDisutilitySource tdisSource, AlgorithmType algorithmType)
    {
        this.name = name;
        this.ttimeSource = ttimeSource;
        this.tdisSource = tdisSource;
        this.algorithmType = algorithmType;
    }


    /*package*/ImmediateRequestTaxiOptimizer createTaxiOptimizer(MatsimVrpContext context,
            VrpPathCalculator calculator, ImmediateRequestParams params)
    {
        TaxiScheduler scheduler = new TaxiScheduler(context, calculator, params);

        switch (algorithmType) {
            case NO_SCHEDULING:
                return new NOSTaxiOptimizer(scheduler, context, new IdleVehicleFinder(context,
                        calculator, this == NOS_STRAIGHT_LINE), false);

            case NO_SCHEDULING_DEMAND_SUPPLY_EQUILIBRIUM:
                return new NOSTaxiOptimizer(scheduler, context, new IdleVehicleFinder(context,
                        calculator, this == NOS_STRAIGHT_LINE), true);

            case ONE_TIME_SCHEDULING:
                return new OTSTaxiOptimizer(scheduler, context);

            case RE_SCHEDULING:
                return new RESTaxiOptimizer(scheduler, context);

            case AP_SCHEDULING:
                return new APSTaxiOptimizer(scheduler, context);

            default:
                throw new IllegalStateException();
        }
    }
}
