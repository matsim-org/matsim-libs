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


    /*package*/static final AlgorithmConfig NOS_SL = new AlgorithmConfig(//
            "NOS_SL",//
            FREE_FLOW_SPEED, // does not matter (since ttCost: DISTANCE)
            STRAIGHT_LINE, // ????? Let's assume that taxi drivers choose the shortest-length path!!!
            NO_SCHEDULING);//

    /*package*/static final AlgorithmConfig NOS_TD = new AlgorithmConfig(//
            "NOS_TD", //
            FREE_FLOW_SPEED, // does not matter (since ttCost: DISTANCE)
            DISTANCE, //
            NO_SCHEDULING);//

    /*package*/static final AlgorithmConfig NOS_FF = new AlgorithmConfig(//
            "NOS_FF", //
            FREE_FLOW_SPEED, //
            TIME, //
            NO_SCHEDULING);//

    /*package*/static final AlgorithmConfig NOS_24H = new AlgorithmConfig(//
            "NOS_24H", //
            EVENTS_24_H, //
            TIME, //
            NO_SCHEDULING);//

    /*package*/static final AlgorithmConfig NOS_15M = new AlgorithmConfig(//
            "NOS_15M", //
            EVENTS_15_MIN, //
            TIME, //
            NO_SCHEDULING);//

    /*package*/static final AlgorithmConfig NOS_DSE_SL = new AlgorithmConfig(//
            "NOS_DSE_SL",//
            FREE_FLOW_SPEED, // does not matter (since ttCost: DISTANCE)
            STRAIGHT_LINE, // ????? Let's assume that taxi drivers choose the shortest-length path!!!
            NO_SCHEDULING_DEMAND_SUPPLY_EQUILIBRIUM);//

    /*package*/static final AlgorithmConfig NOS_DSE_TD = new AlgorithmConfig(//
            "NOS_DSE_TD", //
            FREE_FLOW_SPEED, // does not matter (since ttCost: DISTANCE)
            DISTANCE, //
            NO_SCHEDULING_DEMAND_SUPPLY_EQUILIBRIUM);//

    /*package*/static final AlgorithmConfig NOS_DSE_FF = new AlgorithmConfig(//
            "NOS_DSE_FF", //
            FREE_FLOW_SPEED, //
            TIME, //
            NO_SCHEDULING_DEMAND_SUPPLY_EQUILIBRIUM);//

    /*package*/static final AlgorithmConfig NOS_DSE_24H = new AlgorithmConfig(//
            "NOS_DSE_24H", //
            EVENTS_24_H, //
            TIME, //
            NO_SCHEDULING_DEMAND_SUPPLY_EQUILIBRIUM);//

    /*package*/static final AlgorithmConfig NOS_DSE_15M = new AlgorithmConfig(//
            "NOS_DSE_15M", //
            EVENTS_15_MIN, //
            TIME, //
            NO_SCHEDULING_DEMAND_SUPPLY_EQUILIBRIUM);//

    /*package*/static final AlgorithmConfig OTS_FF = new AlgorithmConfig(//
            "OTS_FF", //
            FREE_FLOW_SPEED, //
            TIME, //
            ONE_TIME_SCHEDULING);//

    /*package*/static final AlgorithmConfig OTS_24H = new AlgorithmConfig(//
            "OTS_24H", //
            EVENTS_24_H, //
            TIME, //
            ONE_TIME_SCHEDULING);//

    /*package*/static final AlgorithmConfig OTS_15M = new AlgorithmConfig(//
            "OTS_15M", //
            EVENTS_15_MIN, //
            TIME, //
            ONE_TIME_SCHEDULING);//

    /*package*/static final AlgorithmConfig RES_FF = new AlgorithmConfig(//
            "RES_FF", //
            FREE_FLOW_SPEED, //
            TIME, //
            RE_SCHEDULING);//

    /*package*/static final AlgorithmConfig RES_24H = new AlgorithmConfig(//
            "RES_24H", //
            EVENTS_24_H, //
            TIME, //
            RE_SCHEDULING);//

    /*package*/static final AlgorithmConfig RES_15M = new AlgorithmConfig(//
            "RES_15M", //
            EVENTS_15_MIN, //
            TIME, //
            RE_SCHEDULING);//

    /*package*/static final AlgorithmConfig APS_FF = new AlgorithmConfig(//
            "APS_FF", //
            FREE_FLOW_SPEED, //
            TIME, //
            AP_SCHEDULING);//

    /*package*/static final AlgorithmConfig APS_24H = new AlgorithmConfig(//
            "APS_24H", //
            EVENTS_24_H, //
            TIME, //
            AP_SCHEDULING);//

    /*package*/static final AlgorithmConfig APS_15M = new AlgorithmConfig(//
            "APS_15M", //
            EVENTS_15_MIN, //
            TIME, //
            AP_SCHEDULING);//

    /*package*/static final AlgorithmConfig[] ALL = {//
    NOS_SL,//
            NOS_TD,//
            NOS_FF,//
            NOS_24H,//
            NOS_15M,//
            NOS_DSE_SL,//
            NOS_DSE_TD,//
            NOS_DSE_FF,//
            NOS_DSE_24H,//
            NOS_DSE_15M,//
            OTS_FF,//
            OTS_24H,//
            OTS_15M,//
            RES_FF,//
            RES_24H,//
            RES_15M, //
            APS_FF,//
            APS_24H,//
            APS_15M //
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
                        calculator, tdisSource), false);

            case NO_SCHEDULING_DEMAND_SUPPLY_EQUILIBRIUM:
                return new NOSTaxiOptimizer(scheduler, context, new IdleVehicleFinder(context,
                        calculator, tdisSource), true);

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
