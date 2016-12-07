package playground.gregor.confluent;/* *********************************************************************** *
 * project: org.matsim.*
 *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.costcalculators.TravelDisutilityModule;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.scenario.ScenarioUtils;


public class ConfluentRunner {

    public static void main(String[] args) {
        Config c = ConfigUtils.createConfig();
        Scenario sc = ScenarioUtils.createScenario(c);

        configure(c);
        loadsc(sc);

        Controler controler = new Controler(sc);


        //that doesn't work
        controler.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {
                this.bind(TravelDisutilityModule.class).to(MSATravelDisutilityModule.class);
            }
        });
//
//        //that neither
//        controler.addOverridingModule( new AbstractModule(){
//       			@Override public void install() {
//                    addEventHandlerBinding().to(MSATravelDisutility.class);
//                    this.bind(TravelDisutility.class).to(MSATravelDisutility.class);
//       			}
//       		});
//        //none of them work
//        controler.addOverridingModule( new AbstractModule(){
//       			@Override public void install() {
//                    addEventHandlerBinding().to(MSATravelDisutility.class);
//                    this.bind(TravelDisutilityFactory.class).to(MSATravelDisutilityFactory.class);
//       			}
//       		});

        controler.run();
    }

    private static void loadsc(Scenario sc) {
        new MatsimNetworkReader(sc.getNetwork()).readFile("examples/scenarios/equil/network.xml");
        new PopulationReader(sc).readFile("examples/scenarios/equil/plans1.xml");
    }

    private static void configure(Config c) {

        boolean verbose = true;
        c.global().setRandomSeed(4711L);
        c.global().setNumberOfThreads(6);

        c.controler().setLastIteration(1);
        c.controler().setOutputDirectory("/tmp/output/");
        c.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);

        c.qsim().setFlowCapFactor(.1);
        c.qsim().setStorageCapFactor(.1);
        c.qsim().setInsertingWaitingVehiclesBeforeDrivingVehicles(true);
        c.qsim().setEndTime(24 * 3600);
        c.qsim().setNumberOfThreads(6);

        c.strategy().setMaxAgentPlanMemorySize(1);
//               c.strategy().addParam("ModuleDisableAfterIteration_1", "30");
        c.strategy().addParam("Module_1", "ReRoute");
        c.strategy().addParam("ModuleProbability_1", "0.1");
//               c.strategy().addParam("Module_2", "ChangeExpBeta");
//               c.strategy().addParam("ModuleProbability_2", "0.9");

        c.travelTimeCalculator().setTravelTimeCalculatorType("TravelTimeCalculatorHashMap");
        //        c.travelTimeCalculator().setTravelTimeAggregatorType("experimental_LastMile");
        c.travelTimeCalculator().setTraveltimeBinSize(24 * 3600);

        c.controler().setCreateGraphs(verbose);
        c.controler().setDumpDataAtEnd(verbose);
        c.controler().setWriteEventsInterval(verbose ? 1 : 0);
        c.controler().setWritePlansInterval(verbose ? 1 : 0);


        PlanCalcScoreConfigGroup.ActivityParams pre = new PlanCalcScoreConfigGroup.ActivityParams("h");
        pre.setTypicalDuration(49); // needs to be geq 49, otherwise when
        // running a simulation one gets
        // "java.lang.RuntimeException: zeroUtilityDuration of type pre-evac must be greater than 0.0. Did you forget to specify the typicalDuration?"
        // the reason is the double precision. see also comment in
        // ActivityUtilityParameters.java (gl)
        pre.setMinimalDuration(49);
        pre.setClosingTime(49);
        pre.setEarliestEndTime(49);
        pre.setLatestStartTime(49);
        pre.setOpeningTime(49);

        PlanCalcScoreConfigGroup.ActivityParams post = new PlanCalcScoreConfigGroup.ActivityParams("w");
        post.setTypicalDuration(49); // dito
        post.setMinimalDuration(49);
        post.setClosingTime(49);
        post.setEarliestEndTime(49);
        post.setLatestStartTime(49);
        post.setOpeningTime(49);
        c.planCalcScore().addActivityParams(pre);
        c.planCalcScore().addActivityParams(post);

        c.planCalcScore().setLateArrival_utils_hr(0.);
        c.planCalcScore().setPerforming_utils_hr(0.);
    }
}
