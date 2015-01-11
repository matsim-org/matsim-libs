/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * CadytsRandomTrips.java
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) 2014 by the members listed in the COPYING, *
 *  *                   LICENSE and WARRANTY file.                            *
 *  * email           : info at matsim dot org                                *
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  *   This program is free software; you can redistribute it and/or modify  *
 *  *   it under the terms of the GNU General Public License as published by  *
 *  *   the Free Software Foundation; either version 2 of the License, or     *
 *  *   (at your option) any later version.                                   *
 *  *   See also COPYING, LICENSE and WARRANTY file                           *
 *  *                                                                         *
 *  * ***********************************************************************
 */

package playground.mzilske.populationsize;

import org.apache.log4j.Logger;
import org.matsim.analysis.CalcLegTimes;
import org.matsim.analysis.VolumesAnalyzer;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.cadyts.general.CadytsConfigGroup;
import org.matsim.contrib.multimodal.config.MultiModalConfigGroup;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.StrategyConfigGroup;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.ControlerDefaultsModule;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.PlanStrategyImpl;
import org.matsim.core.replanning.modules.AbstractMultithreadedModule;
import org.matsim.core.replanning.selectors.RandomPlanSelector;
import org.matsim.core.router.PlanRouter;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.counts.Counts;
import org.matsim.counts.CountsReaderMatsimV1;
import org.matsim.counts.CountsWriter;
import org.matsim.population.algorithms.PlanAlgorithm;
import playground.mzilske.cadyts.CadytsModule;
import playground.mzilske.cdr.CompareMain;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.*;


public class CadytsRandomTrips {

    public static final String USERS_MICHAELZILSKE_RUNS_SVN_SYNTHETIC_CDR_TRANSPORTATION_BERLIN_REGIMES_UNCONGESTED = "/Users/michaelzilske/runs-svn/synthetic-cdr/transportation/berlin/regimes/uncongested";
    static String rateDir = "/Users/michaelzilske/runs-svn/synthetic-cdr/transportation/berlin/regimes/uncongested/alternatives/only-histogram";

    public static void main(String[] args) {
        // filterCounts();
        simulate();
    }

    private static Config randomConfig() {
        Config config = ConfigUtils.createConfig();
        config.controler().setOutputDirectory("wurst");
        config.controler().setLastIteration(1000);
        PlanCalcScoreConfigGroup.ActivityParams sightingParam = new PlanCalcScoreConfigGroup.ActivityParams("sighting");
        sightingParam.setTypicalDuration(30.0 * 60);
        // config.controler().setMobsim("my-qsim");
        config.controler().setWritePlansInterval(10);
        config.counts().setWriteCountsInterval(10);
        config.global().setNumberOfThreads(8);
        config.planCalcScore().addActivityParams(sightingParam);
        config.planCalcScore().setTraveling_utils_hr(-6);
        config.planCalcScore().setPerforming_utils_hr(0);
        config.planCalcScore().setTravelingOther_utils_hr(-6);
        config.planCalcScore().setConstantCar(0);
        config.planCalcScore().setMonetaryDistanceCostRateCar(0);
        config.planCalcScore().setWriteExperiencedPlans(true);
        config.strategy().setFractionOfIterationsToDisableInnovation(0.8);
        CadytsConfigGroup cadytsConfig = ConfigUtils.addOrGetModule(config, CadytsConfigGroup.GROUP_NAME, CadytsConfigGroup.class);
        cadytsConfig.setVarianceScale(0.001);
        cadytsConfig.setMinFlowStddev_vehPerHour(2.0);


        cadytsConfig.setPreparatoryIterations(1);


        config.qsim().setMainModes(Arrays.asList("car"));
        config.qsim().setFlowCapFactor(100);
        config.qsim().setStorageCapFactor(100);
        config.qsim().setRemoveStuckVehicles(false);
        config.qsim().setNumberOfThreads(1);

        {
            StrategyConfigGroup.StrategySettings stratSets = new StrategyConfigGroup.StrategySettings(Id.create(1, StrategySettings.class));
            stratSets.setStrategyName("SelectExpBeta");

            stratSets.setWeight(1.0);
            config.strategy().addStrategySettings(stratSets);
        }
        {
            StrategyConfigGroup.StrategySettings stratSets = new StrategyConfigGroup.StrategySettings(Id.create(2, StrategySettings.class));
            stratSets.setStrategyName("NewRandomTrip");
            stratSets.setWeight(0.1);
//            stratSets.setDisableAfter(25);
            config.strategy().addStrategySettings(stratSets);
        }

        MultiModalConfigGroup multiModal = ConfigUtils.addOrGetModule(config, MultiModalConfigGroup.GROUP_NAME, MultiModalConfigGroup.class);
        multiModal.setSimulatedModes("car");

        return config;

    }

    private static void simulate() {
        final Counts allCounts = new Counts();
        new CountsReaderMatsimV1(allCounts).parse(rateDir + "/all_counts.xml.gz");
        final Counts someCounts = new Counts();
        new CountsReaderMatsimV1(someCounts).parse(rateDir + "/calibration_counts.xml.gz");


        RunResource uncongested = new RegimeResource(USERS_MICHAELZILSKE_RUNS_SVN_SYNTHETIC_CDR_TRANSPORTATION_BERLIN_REGIMES_UNCONGESTED, "uncongested").getBaseRun();
        Scenario baseScenario = uncongested.getOutputScenario();

        EventsManager events = EventsUtils.createEventsManager(baseScenario.getConfig());
        CalcLegTimes legTimes = new CalcLegTimes();
        events.addHandler(legTimes);
        new MatsimEventsReader(events).readFile(uncongested.getLastIteration().getEventsFileName());


        final ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(randomConfig());
        scenario.setNetwork(baseScenario.getNetwork());
        initialDemand(scenario);

        TripLengthDistribution tripLengthDistribution = new TripLengthDistribution(legTimes.getLegStats());
        scenario.addScenarioElement("expectedTripLengthDistribution", tripLengthDistribution);

        TripLengthDistribution actualTripLengthDistribution = new TripLengthDistribution(legTimes.getLegStats());
        scenario.addScenarioElement("actualTripLengthDistribution", actualTripLengthDistribution);

        StringBuilder stringbuilder = new StringBuilder();
        int l = legTimes.getLegStats().values().iterator().next().length;
        for (int i = 0; i < l; ++i) {
            int n=0;
            for (int[] bins : legTimes.getLegStats().values()) {
                n += bins[i];
            }
            stringbuilder.append("\t" + n);
        }
        Logger.getLogger(CadytsRandomTrips.class).info(stringbuilder.toString());

        scenario.addScenarioElement(Counts.ELEMENT_NAME, allCounts);
        scenario.addScenarioElement("calibrationCounts", someCounts);


        Controler controler = new Controler(scenario);
        controler.setOverwriteFiles(true);
        controler.setModules(
                new ControlerDefaultsModule(),
                new CadytsModule(),
                new AbstractModule() {
                    @Override
                    public void install() {
                        addControlerListener(LegTimesHistogramControlerListener.class);
                        addPlanStrategyBinding("NewRandomTrip").toProvider(RandomTripProvider.class);
                    }
                });
        controler.setScoringFunctionFactory(new CadytsAndLegHistogramScoringFunctionFactory());
        controler.run();

    }

    private static void initialDemand(Scenario scenario) {
        Population population = scenario.getPopulation();
        RandomLinkGetter random = new RandomLinkGetter(scenario.getNetwork());
        for(int i=0; i<62749; i++) {
            Person person = population.getFactory().createPerson(Id.create(Integer.toString(i),Person.class));
            Plan plan = population.getFactory().createPlan();
            person.addPlan(plan);
            randomTrip(scenario, random, plan);
            population.addPerson(person);
        }
    }

    private static void randomTrip(Scenario scenario, RandomLinkGetter random, Plan plan) {
        plan.getPlanElements().clear();
        Link startLink = random.nextLink();
        Link endLink = random.nextLink();
        Activity start = scenario.getPopulation().getFactory().createActivityFromLinkId("sighting", startLink.getId());
        Activity end = scenario.getPopulation().getFactory().createActivityFromLinkId("sighting", endLink.getId());
        start.setEndTime(random.random.nextInt(24*60*60));
        plan.addActivity(start);
        plan.addLeg(scenario.getPopulation().getFactory().createLeg("car"));
        plan.addActivity(end);
    }

    private static class RandomLinkGetter {
        private final List<Id> ids;
        private final Network network;
        final Random random = new Random( 1234 );

        public RandomLinkGetter( final Network network ) {
            this.network = network;
            this.ids = new ArrayList<Id>( network.getLinks().keySet() );
            Collections.sort(ids);
        }

        public Link nextLink() {
            return network.getLinks().get(
                    ids.get( random.nextInt( ids.size() ) ) );
        }
    }

    private static void filterCounts() {
        int TIME_BIN_SIZE = 60*60;
        int MAX_TIME = 24 * TIME_BIN_SIZE - 1;
        String rateDir = "/Users/michaelzilske/runs-svn/synthetic-cdr/transportation/berlin/regimes/uncongested/alternatives/only-cadyts";
        RunResource uncongested = new RegimeResource(USERS_MICHAELZILSKE_RUNS_SVN_SYNTHETIC_CDR_TRANSPORTATION_BERLIN_REGIMES_UNCONGESTED, "uncongested").getBaseRun();
        Scenario baseScenario = uncongested.getOutputScenario();

        EventsManager events = EventsUtils.createEventsManager(baseScenario.getConfig());
        VolumesAnalyzer groundTruthVolumes = new VolumesAnalyzer(TIME_BIN_SIZE, MAX_TIME, baseScenario.getNetwork());
        events.addHandler(groundTruthVolumes);
        new MatsimEventsReader(events).readFile(uncongested.getLastIteration().getEventsFileName());

        final Counts allCounts = CompareMain.volumesToCounts(baseScenario.getNetwork(), groundTruthVolumes, 1.0);
        allCounts.setYear(2012);
        new CountsWriter(allCounts).write(rateDir + "/all_counts.xml.gz");
        final Counts someCounts = allCounts;
        someCounts.setYear(2012);
        new CountsWriter(someCounts).write(rateDir + "/calibration_counts.xml.gz");
    }

    private static class RandomTripProvider implements Provider<PlanStrategy> {

        @Inject
        Scenario scenario;

        @Override
        public PlanStrategy get() {
            final RandomLinkGetter random = new RandomLinkGetter(scenario.getNetwork());
            PlanStrategyImpl planStrategy = new PlanStrategyImpl(new RandomPlanSelector<Plan, Person>());
            planStrategy.addStrategyModule(new AbstractMultithreadedModule(Runtime.getRuntime().availableProcessors()) {

                @Override
                public PlanAlgorithm getPlanAlgoInstance() {
                    return new PlanAlgorithm() {
                        @Override
                        public void run(Plan plan) {
                            randomTrip(scenario, random, plan);
                            new PlanRouter(getReplanningContext().getTripRouter()).run(plan);
                        }
                    };
                }
            });
            return planStrategy;
        }
    }
}
