package playground.gregor.confluent;
/* *********************************************************************** *
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
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.ScenarioUtils;

import java.util.ArrayList;
import java.util.List;


public class ConfluentRunner {

    private static String NET_FILE = "/Users/laemmel/scenarios/padang/output_network.xml.gz";
    private static String POP_FILE = "/Users/laemmel/scenarios/padang/10p_sample.plans.xml.gz";
    private static String OUT_DIR = "/tmp/output/";
    private static boolean HEADLESS = false;
    private static Integer ITERATIONS = 500;

    public static void main(String[] args) {

        if (args.length == 5) {
            NET_FILE = args[0];
            POP_FILE = args[1];
            OUT_DIR = args[2];
            HEADLESS = Boolean.parseBoolean(args[3]);
            ITERATIONS = Integer.parseInt(args[4]);

        }


        Config c = ConfigUtils.createConfig();
        Scenario sc = ScenarioUtils.createScenario(c);

        configure(c);
        loadsc(sc);

        Controler controler = new Controler(sc);


        SimulatedAnnealingTravelDisutility tc = new SimulatedAnnealingTravelDisutility();

        controler.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {
                bind(SimulatedAnnealingTravelDisutility.class).toInstance(tc);
                addControlerListenerBinding().toInstance(tc);
                addEventHandlerBinding().toInstance(tc);
                bindCarTravelDisutilityFactory().to(SimulatedAnnealingTravelDisutilityFactory.class);
            }
        });


        controler.run();
    }

    private static void loadsc(Scenario sc) {
        new MatsimNetworkReader(sc.getNetwork()).readFile(NET_FILE);
        new PopulationReader(sc).readFile(POP_FILE);
        for (Person pers : sc.getPopulation().getPersons().values()) {
            List<Plan> rm = new ArrayList<>();

            Plan selected = pers.getSelectedPlan();
            pers.getPlans().removeIf(plan -> plan != selected);

            ((Leg) pers.getPlans().get(0).getPlanElements().get(1)).setRoute(null);
        }
    }

    private static void configure(Config c) {

        boolean verbose = false;
        c.global().setRandomSeed(4711L);
        c.global().setNumberOfThreads(6);

        c.controler().setLastIteration(ITERATIONS);
        c.controler().setOutputDirectory(OUT_DIR);
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

        c.controler().setCreateGraphs(!HEADLESS);
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
