/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * MultiRateRunResource.java
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

import com.google.common.collect.ImmutableMap;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.multibindings.MapBinder;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Names;
import org.matsim.analysis.VolumesAnalyzer;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.contrib.cadyts.general.CadytsConfigGroup;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.ControlerListener;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.mobsim.framework.MobsimFactory;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.counts.Count;
import org.matsim.counts.Counts;
import org.matsim.counts.CountsReaderMatsimV1;
import org.matsim.counts.CountsWriter;
import playground.mzilske.cadyts.CadytsModule;
import playground.mzilske.cdr.*;
import playground.mzilske.cdranalysis.FileIO;
import playground.mzilske.cdranalysis.IterationResource;
import playground.mzilske.cdranalysis.Reading;
import playground.mzilske.cdranalysis.StreamingOutput;
import playground.mzilske.clones.ClonesModule;
import playground.mzilske.controller.Controller;
import playground.mzilske.controller.ControllerModule;
import playground.mzilske.d4d.Sighting;
import playground.mzilske.util.IterationSummaryFileControlerListener;

import javax.inject.Inject;
import javax.inject.Provider;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

class MultiRateRunResource {

    private String WD;

    private String regime;

    private String alternative;

    public MultiRateRunResource(String wd, String regime, String alternative) {
        this.WD = wd;
        this.regime = regime;
        this.alternative = alternative;
    }

    final static int TIME_BIN_SIZE = 60*60;
    final static int MAX_TIME = 30 * TIME_BIN_SIZE - 1;

    public Collection<String> getRates() {
        final List<String> RATES = new ArrayList<String>();
        RATES.add("0");
        RATES.add("5");
        RATES.add("50");
        return RATES;
    }

    private RunResource getBaseRun() {
        return new RegimeResource(WD + "/../..", regime).getBaseRun();
    }

    public void rate(String string) {
        Scenario scenario = getBaseRun().getLastIteration().getExperiencedPlansAndNetwork();
        if (string.equals("actevents")) {
            runPhoneOnActivityStartEnd(scenario);
        } else {
            int rate = Integer.parseInt(string);
            runRate(scenario, rate);
        }
    }

    public void twoRates(String string) {
        final Scenario baseScenario = getBaseRun().getLastIteration().getExperiencedPlansAndNetwork();
        final int rate = Integer.parseInt(string);
        for (Person person : baseScenario.getPopulation().getPersons().values()) {
            if (CountWorkers.isWorker(person)) {
                person.getCustomAttributes().put("phonerate", 50);
            } else {
                person.getCustomAttributes().put("phonerate", rate);
            }
        }
        EventsManager events = EventsUtils.createEventsManager();
        ZoneTracker.LinkToZoneResolver linkToZoneResolver = new ZoneTracker.LinkToZoneResolver() {

            @Override
            public Id resolveLinkToZone(Id linkId) {
                return linkId;
            }

            public IdImpl chooseLinkInZone(String zoneId) {
                return new IdImpl(zoneId);
            }

        };
        final CompareMain compareMain = new CompareMain(baseScenario, events, new CallBehavior() {

            @Override
            public boolean makeACall(ActivityEndEvent event) {
                return false;
            }

            @Override
            public boolean makeACall(ActivityStartEvent event) {
                return false;
            }

            @Override
            public boolean makeACall(Id id, double time) {
                Person person = baseScenario.getPopulation().getPersons().get(id);
                double secondlyProbability = (Integer) person.getCustomAttributes().get("phonerate") / (double) (24*60*60);
                return Math.random() < secondlyProbability;
            }

            @Override
            public boolean makeACallAtMorningAndNight() {
                return false;
            }

        }, linkToZoneResolver);
        new MatsimEventsReader(events).readFile(getBaseRun().getLastIteration().getEventsFileName());
        compareMain.close();


        final Map<Id, List<Sighting>> allSightings = compareMain.getSightingsPerPerson();

        final Config config = phoneConfig();
        config.controler().setOutputDirectory(WD + "/rates/twotimes_" + rate);

        final ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(config);
        scenario.setNetwork(baseScenario.getNetwork());
//        PopulationFromSightings.createPopulationWithEndTimesAtLastSightings(scenario, linkToZoneResolver, allSightings);

        PopulationFromSightings.createPopulationWithRandomEndTimesInPermittedWindow(scenario, linkToZoneResolver, allSightings);
        PopulationFromSightings.preparePopulation(scenario, linkToZoneResolver, allSightings);

        String rateDir = WD + "/rates/twotimes_" + rate;
        new File(rateDir).mkdirs();

        new PopulationWriter(scenario.getPopulation(), scenario.getNetwork()).write(rateDir + "/input_population.xml.gz");
        final Counts allCounts = CompareMain.volumesToCounts(baseScenario.getNetwork(), compareMain.getGroundTruthVolumes());
        allCounts.setYear(2012);
        new CountsWriter(allCounts).write(rateDir + "/all_counts.xml.gz");
        final Counts someCounts = filterCounts(allCounts);
        someCounts.setYear(2012);
        new CountsWriter(someCounts).write(rateDir + "/calibration_counts.xml.gz");

    }

    public void twoRatesRolling(String string) {
        final int cloneFactor = 10;
        final Scenario baseScenario = getBaseRun().getLastIteration().getExperiencedPlansAndNetwork();
        final int rate = Integer.parseInt(string);
        for (Person person : baseScenario.getPopulation().getPersons().values()) {
            if (CountWorkers.isWorker(person)) {
                person.getCustomAttributes().put("phonerate", 50);
            } else {
                person.getCustomAttributes().put("phonerate", rate);
            }
        }
        EventsManager events = EventsUtils.createEventsManager();
        ZoneTracker.LinkToZoneResolver linkToZoneResolver = new ZoneTracker.LinkToZoneResolver() {

            @Override
            public Id resolveLinkToZone(Id linkId) {
                return linkId;
            }

            public IdImpl chooseLinkInZone(String zoneId) {
                return new IdImpl(zoneId);
            }

        };
        final CompareMain compareMain = new CompareMain(baseScenario, events, new CallBehavior() {

            @Override
            public boolean makeACall(ActivityEndEvent event) {
                return false;
            }

            @Override
            public boolean makeACall(ActivityStartEvent event) {
                return false;
            }

            @Override
            public boolean makeACall(Id id, double time) {
                Person person = baseScenario.getPopulation().getPersons().get(id);
                double secondlyProbability = (Integer) person.getCustomAttributes().get("phonerate") / (double) (24*60*60);
                return Math.random() < secondlyProbability;
            }

            @Override
            public boolean makeACallAtMorningAndNight() {
                return false;
            }

        }, linkToZoneResolver);
        new MatsimEventsReader(events).readFile(getBaseRun().getLastIteration().getEventsFileName());
        compareMain.close();


        final Map<Id, List<Sighting>> allSightings = compareMain.getSightingsPerPerson();




        final Config phoneConfig = phoneConfig();


        final ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(phoneConfig);
        scenario.setNetwork(baseScenario.getNetwork());



        PopulationFromSightings.createClonedPopulationWithRandomEndTimesInPermittedWindow(scenario, linkToZoneResolver, allSightings, cloneFactor);
        PopulationFromSightings.preparePopulation(scenario, linkToZoneResolver, allSightings);


        if(cloneFactor == 1) {
            phoneConfig.controler().setLastIteration(0);
            phoneConfig.planCalcScore().setWriteExperiencedPlans(true);
        }
        phoneConfig.controler().setOutputDirectory(WD + "/rates/rtwo_" + rate + "/" + cloneFactor);

        final Counts allCounts = CompareMain.volumesToCounts(baseScenario.getNetwork(), compareMain.getGroundTruthVolumes());
        allCounts.setYear(2012);
        final Counts someCounts = filterCounts(allCounts);
        someCounts.setYear(2012);

        simulate(cloneFactor, scenario, allCounts, someCounts);
    }

    private void simulate(final double cloneFactor, final ScenarioImpl scenario, final Counts allCounts, final Counts someCounts) {
        List<Module> modules = new ArrayList<Module>();
        modules.add(new ControllerModule());
        modules.add(new CadytsModule());
        modules.add(new ClonesModule());
        modules.add(new AbstractModule() {
            @Override
            protected void configure() {
                bind(Config.class).toInstance(scenario.getConfig());
                bind(Scenario.class).toInstance(scenario);
                bind(ScoringFunctionFactory.class).to(CharyparNagelCadytsScoringFunctionFactory.class);
                bind(Counts.class).annotatedWith(Names.named("allCounts")).toInstance(allCounts);
                bind(Counts.class).annotatedWith(Names.named("calibrationCounts")).toInstance(someCounts);
                bind(Double.class).annotatedWith(Names.named("clonefactor")).toInstance(cloneFactor);
                bind(Boolean.class).annotatedWith(Names.named("alreadyCloned")).toInstance(true);
            }
        });
        Injector injector2 = Guice.createInjector(modules);
        Controller controler2 = injector2.getInstance(Controller.class);
        controler2.run();
    }

    public void allRates() {
        for (String rate : getRates()) {
            rate(rate);
        }
    }


    private void runPhoneOnActivityStartEnd(Scenario baseScenario) {
        EventsManager events = EventsUtils.createEventsManager();
        ZoneTracker.LinkToZoneResolver linkToZoneResolver = new ZoneTracker.LinkToZoneResolver() {

            @Override
            public Id resolveLinkToZone(Id linkId) {
                return linkId;
            }

            public IdImpl chooseLinkInZone(String zoneId) {
                return new IdImpl(zoneId);
            }

        };
        CompareMain compareMain = new CompareMain(baseScenario, events, new CallBehavior() {

            @Override
            public boolean makeACall(ActivityEndEvent event) {
                return true;
            }

            @Override
            public boolean makeACall(ActivityStartEvent event) {
                return true;
            }

            @Override
            public boolean makeACall(Id id, double time) {
                return false;
            }

            @Override
            public boolean makeACallAtMorningAndNight() {
                return true;
            }

        }, linkToZoneResolver);
        new MatsimEventsReader(events).readFile(getBaseRun().getLastIteration().getEventsFileName());

        compareMain.close();


        final Map<Id, List<Sighting>> allSightings = compareMain.getSightingsPerPerson();

        final Config config = phoneConfig();
        config.controler().setOutputDirectory(WD + "/rates/actevents");

        final ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(config);
        scenario.setNetwork(baseScenario.getNetwork());
        PopulationFromSightings.createPopulationWithEndTimesAtLastSightings(scenario, linkToZoneResolver, allSightings);
        PopulationFromSightings.preparePopulation(scenario, linkToZoneResolver, allSightings);

        String rateDir = WD + "/rates/actevents";
        new File(rateDir).mkdirs();

        new PopulationWriter(scenario.getPopulation(), scenario.getNetwork()).write(rateDir + "/input_population.xml.gz");
        final Counts allCounts = CompareMain.volumesToCounts(baseScenario.getNetwork(), compareMain.getGroundTruthVolumes());
        allCounts.setYear(2012);
        new CountsWriter(allCounts).write(rateDir + "/all_counts.xml.gz");
        final Counts someCounts = filterCounts(allCounts);
        someCounts.setYear(2012);
        new CountsWriter(someCounts).write(rateDir + "/calibration_counts.xml.gz");

    }

    private void runRate(final Scenario baseScenario, final int dailyRate) {
        EventsManager events = EventsUtils.createEventsManager();
        ZoneTracker.LinkToZoneResolver linkToZoneResolver = new ZoneTracker.LinkToZoneResolver() {

            @Override
            public Id resolveLinkToZone(Id linkId) {
                return linkId;
            }

            public IdImpl chooseLinkInZone(String zoneId) {
                return new IdImpl(zoneId);
            }

        };
        final CompareMain compareMain = new CompareMain(baseScenario, events, new CallBehavior() {

            @Override
            public boolean makeACall(ActivityEndEvent event) {
                return false;
            }

            @Override
            public boolean makeACall(ActivityStartEvent event) {
                return false;
            }

            @Override
            public boolean makeACall(Id id, double time) {
                double secondlyProbability = dailyRate / (double) (24*60*60);
                return Math.random() < secondlyProbability;
            }

            @Override
            public boolean makeACallAtMorningAndNight() {
                return false;
            }

        }, linkToZoneResolver);
        new MatsimEventsReader(events).readFile(getBaseRun().getLastIteration().getEventsFileName());
        compareMain.close();


        final Map<Id, List<Sighting>> allSightings = compareMain.getSightingsPerPerson();

        final Config config = phoneConfig();
        config.controler().setOutputDirectory(WD + "/rates/" + dailyRate);

        final ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(config);
        scenario.setNetwork(baseScenario.getNetwork());
        PopulationFromSightings.createPopulationWithEndTimesAtLastSightings(scenario, linkToZoneResolver, allSightings);
        PopulationFromSightings.preparePopulation(scenario, linkToZoneResolver, allSightings);

        String rateDir = WD + "/rates/" + dailyRate;
        new File(rateDir).mkdirs();

        new PopulationWriter(scenario.getPopulation(), scenario.getNetwork()).write(rateDir + "/input_population.xml.gz");
        final Counts allCounts = CompareMain.volumesToCounts(baseScenario.getNetwork(), compareMain.getGroundTruthVolumes());
        allCounts.setYear(2012);
        new CountsWriter(allCounts).write(rateDir + "/all_counts.xml.gz");
        final Counts someCounts = filterCounts(allCounts);
        someCounts.setYear(2012);
        new CountsWriter(someCounts).write(rateDir + "/calibration_counts.xml.gz");


    }

    public void simulateRate(String rate, final int cloneFactor) {
        final Config config = phoneConfig();

        if(cloneFactor == 1) {
            config.controler().setLastIteration(0);
            config.planCalcScore().setWriteExperiencedPlans(true);
        }
        config.controler().setOutputDirectory(WD + "/rates/" + rate + "/" + cloneFactor);

        Scenario baseScenario = getBaseRun().getConfigAndNetwork();
        final ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(config);
        scenario.setNetwork(baseScenario.getNetwork());
        new MatsimPopulationReader(scenario).readFile(WD + "/rates/" + rate + "/input_population.xml.gz");
        final Counts allCounts = new Counts();
        new CountsReaderMatsimV1(allCounts).parse(WD + "/rates/" + rate + "/all_counts.xml.gz");
        final Counts someCounts = new Counts();
        new CountsReaderMatsimV1(someCounts).parse(WD + "/rates/" + rate + "/calibration_counts.xml.gz");


        List<Module> modules = new ArrayList<Module>();
        modules.add(new ControllerModule());
        modules.add(new CadytsModule());
        modules.add(new ClonesModule());
        modules.add(new AbstractModule() {
            @Override
            protected void configure() {
                bind(Config.class).toInstance(scenario.getConfig());
                bind(Scenario.class).toInstance(scenario);
                bind(ScoringFunctionFactory.class).to(CharyparNagelCadytsScoringFunctionFactory.class);
                bind(Counts.class).annotatedWith(Names.named("allCounts")).toInstance(allCounts);
                bind(Counts.class).annotatedWith(Names.named("calibrationCounts")).toInstance(someCounts);
                bind(Double.class).annotatedWith(Names.named("clonefactor")).toInstance((double) cloneFactor);
                Multibinder<ControlerListener> controlerListenerBinder = Multibinder.newSetBinder(binder(), ControlerListener.class);
//                controlerListenerBinder.addBinding().toProvider(MyControlerListenerProvider.class);
                MapBinder<String, MobsimFactory> mobsimFactoryMapBinder = MapBinder.newMapBinder(binder(), String.class, MobsimFactory.class);
            }
        });

        Injector injector2 = Guice.createInjector(modules);
        Controller controler2 = injector2.getInstance(Controller.class);
        controler2.run();
    }


    static class MyControlerListenerProvider implements Provider<ControlerListener> {

        @Inject Scenario scenario;
        @Inject OutputDirectoryHierarchy controlerIO;
        @Override
        public ControlerListener get() {
            return new IterationSummaryFileControlerListener(controlerIO,
                    ImmutableMap.<String, IterationSummaryFileControlerListener.Writer>of(
                            "clone-stats.txt",
                            new IterationSummaryFileControlerListener.Writer() {
                                @Override
                                public StreamingOutput notifyStartup(StartupEvent event) {
                                    return new StreamingOutput() {
                                        @Override
                                        public void write(PrintWriter pw) throws IOException {
                                            pw.printf("iteration\tnonemptyplans\tnonemptyplanswithoutoffset\n");
                                        }
                                    };
                                }

                                @Override
                                public StreamingOutput notifyIterationEnds(final IterationEndsEvent event) {
                                    return new StreamingOutput() {
                                        @Override
                                        public void write(PrintWriter pw) throws IOException {
                                            int nNonEmptySelectedPlans = 0;
                                            int nNonEmptySelectedPlansWithoutOffset = 0;
                                            Population plans = scenario.getPopulation();
                                            for (Person person : plans.getPersons().values()) {
                                                Plan plan = person.getSelectedPlan();
                                                if (plan.getPlanElements().size() > 1) {
                                                    nNonEmptySelectedPlans++;
                                                    if (plan.getScore() != null && plan.getScore() == 0.0) {
                                                        nNonEmptySelectedPlansWithoutOffset++;
                                                    }
                                                }

                                            }
                                            pw.printf("%d\t%d\t%d\n", event.getIteration(), nNonEmptySelectedPlans, nNonEmptySelectedPlansWithoutOffset);
                                            pw.flush();
                                        }
                                    };
                                }
                            }
                    ));
        }
    }

    private Counts filterCounts(Counts allCounts) {
        Counts someCounts = new Counts();
        for (Map.Entry<Id, Count> entry: allCounts.getCounts().entrySet()) {
            if (Math.random() < 0.05) {
                someCounts.getCounts().put(entry.getKey(), entry.getValue());
            }
        }
        return someCounts;
    }

    private Config phoneConfig() {
        Config config = ConfigUtils.createConfig();

        config.controler().setLastIteration(100);
        ActivityParams sightingParam = new ActivityParams("sighting");
        sightingParam.setTypicalDuration(30.0 * 60);
        config.controler().setWritePlansInterval(10);
        config.planCalcScore().addActivityParams(sightingParam);
        config.planCalcScore().setTraveling_utils_hr(-6);
        config.planCalcScore().setPerforming_utils_hr(0);
        config.planCalcScore().setTravelingOther_utils_hr(-6);
        config.planCalcScore().setConstantCar(0);
        config.planCalcScore().setMonetaryDistanceCostRateCar(0);
        config.planCalcScore().setWriteExperiencedPlans(true);
        CadytsConfigGroup cadytsConfig = ConfigUtils.addOrGetModule(config, CadytsConfigGroup.GROUP_NAME, CadytsConfigGroup.class);
        cadytsConfig.setVarianceScale(0.001);
        cadytsConfig.setMinFlowStddev_vehPerHour(2.0);


        cadytsConfig.setPreparatoryIterations(1);

        QSimConfigGroup tmp = config.qsim();
        tmp.setFlowCapFactor(100);
        tmp.setStorageCapFactor(100);
        tmp.setRemoveStuckVehicles(false);
//        tmp.setStuckTime(10.0);


//        config.controler().setMobsim("JDEQSim");
//        config.setParam("JDEQSim", "squeezeTime", "10.0");
//        config.setParam("JDEQSim", "flowCapacityFactor", "100");
//        config.setParam("JDEQSim", "storageCapacityFactor", "100");


        {
            StrategySettings stratSets = new StrategySettings(new IdImpl(1));
//            stratSets.setModuleName("ccs");
            stratSets.setModuleName("SelectExpBeta");

            stratSets.setProbability(1.0);
            config.strategy().addStrategySettings(stratSets);
        }
        {
            StrategySettings stratSets = new StrategySettings(new IdImpl(2));
            stratSets.setModuleName("SelectRandom");
            stratSets.setProbability(0.1);
            stratSets.setDisableAfter(30);
            config.strategy().addStrategySettings(stratSets);
        }
        return config;

    }

    public void errors() {
        final String filename = WD + "/errors.txt";
        final Scenario baseScenario = getBaseRun().getLastIteration().getExperiencedPlansAndNetwork();
        EventsManager events = EventsUtils.createEventsManager();
        final VolumesAnalyzer baseVolumes = new VolumesAnalyzer(TIME_BIN_SIZE, MAX_TIME, baseScenario.getNetwork());
        events.addHandler(baseVolumes);
        new MatsimEventsReader(events).readFile(getBaseRun().getLastIteration().getEventsFileName());
        FileIO.writeToFile(filename, new StreamingOutput() {
            @Override
            public void write(PrintWriter pw) throws IOException {
                pw.printf("regime\trate\tclonefactor\tmre\tmae\tmab\n");
                for (String rate : getRates()) {
                    Collection<String> cloneFactors = getCloneFactors(rate);
                    for (String cloneFactor : cloneFactors) {
                        final IterationResource lastIteration = getRateRun(rate, cloneFactor).getLastIteration();
                        EventsManager events1 = EventsUtils.createEventsManager();
                        VolumesAnalyzer volumes = new VolumesAnalyzer(TIME_BIN_SIZE, MAX_TIME, baseScenario.getNetwork());
                        events1.addHandler(volumes);
                        new MatsimEventsReader(events1).readFile(lastIteration.getEventsFileName());
                        double meanRelativeError = 0;
                        double meanAbsoluteError = meanAbsoluteError(baseVolumes, volumes);
                        double meanAbsoluteBias = meanAbsoluteBias(baseVolumes, volumes);
                        pw.printf("%s\t%s\t%s\t%f\t%f\t%f\n",
                                regime, rate, cloneFactor, meanRelativeError, meanAbsoluteError, meanAbsoluteBias);
                        pw.flush();
                    }
                }
            }
        });
    }

    private Collection<String> getCloneFactors(String rate) {
        return Arrays.asList("10");
    }

    private double meanAbsoluteError(VolumesAnalyzer baseVolumes, VolumesAnalyzer volumes) {
        double result = 0.0;
        double num = 0.0;
        for (Id id : baseVolumes.getLinkIds()) {
            int[] us = getVolumesForLink(baseVolumes, id);
            int[] vs = getVolumesForLink(volumes, id);
            for (int i = 0; i < us.length; i++) {
                result += Math.abs(us[i] - vs[i]);
            }
            num += us.length;
        }
        return result / num;
    }

    private double meanAbsoluteBias(VolumesAnalyzer baseVolumes, VolumesAnalyzer volumes) {
        double result = 0.0;
        double num = 0.0;
        for (Id id : baseVolumes.getLinkIds()) {
            int[] us = getVolumesForLink(baseVolumes, id);
            int[] vs = getVolumesForLink(volumes, id);
            for (int i = 0; i < us.length; i++) {
                result += vs[i] - us[i];
            }
            num += us.length;
        }
        return result / num;
    }

    public void distances2() {
        final String filename = WD + "/distances2.txt";
        final Scenario baseScenario = getBaseRun().getLastIteration().getExperiencedPlansAndNetwork();
        final double baseKm = sum(PowerPlans.travelledDistancePerPerson(baseScenario.getPopulation(), baseScenario.getNetwork()).values());
        FileIO.writeToFile(filename, new StreamingOutput() {
            @Override
            public void write(PrintWriter pw) throws IOException {
                pw.printf("regime\tcallrate\troutesum\trouterate\n");
                pw.printf("%s\t%s\t%f\t%f\n",
                        regime, "base", baseKm, baseKm/baseKm);
                for (String rate : getRates()) {
                    Collection<String> cloneFactors = getCloneFactors(rate);
                    for (String cloneFactor : cloneFactors) {
                        Scenario scenario = getRateRun(rate, cloneFactor).getOutputScenario();
                        double km = sum(PowerPlans.travelledDistancePerPerson(scenario.getPopulation(), baseScenario.getNetwork()).values());
                        pw.printf("%s\t%s\t%f\t%f\n",
                                regime, rate, km, km / baseKm);
                        pw.flush();
                    }
                }
            }
        });
    }

    public void persodisthisto() {
        final String filename = WD + "/perso-dist-histo.txt";
        final Scenario baseScenario = getBaseRun().getLastIteration().getExperiencedPlansAndNetwork();
        FileIO.writeToFile(filename, new StreamingOutput() {
            @Override
            public void write(PrintWriter pw) throws IOException {
                pw.printf("person\trate\tCase\tstatus\tkilometers\n");
                for (String rate : getRates()) {
                    final Map<Id, Double> baseKm = PowerPlans.travelledDistancePerPerson(baseScenario.getPopulation(), baseScenario.getNetwork());
                    dumpnonzero(pw, rate, "base", baseKm, baseScenario);
                    {
                        Scenario scenario = getRateRun(rate, "10").getOutputScenario();
                        Map<Id, Double> km = PowerPlans.travelledDistancePerPerson(scenario.getPopulation(), baseScenario.getNetwork());
                        dumpnonzero(pw, rate, "calibrated-CDR", km, baseScenario);
                    }
                    {
                        ArrayList<Person> it0 = new ArrayList<Person>(getRateRun(rate, "10").getIteration(0).getPlans().getPersons().values());
                        for (Iterator<Person> i = it0.iterator(); i.hasNext(); ) {
                            Person person = i.next();
                            if (person.getId().toString().startsWith("I")) {
                                i.remove();
                            } else {
                                person.setSelectedPlan(person.getPlans().get(0));
                            }
                        }
                        Map<Id, Double> km = PowerPlans.travelledDistancePerPerson(baseScenario.getNetwork(), it0);
                        dumpnonzero(pw, rate, "raw-CDR", km, baseScenario);
                    }
                }
            }
        });
    }

    private void dumpnonzero(PrintWriter pw, String rate, String ccase, Map<Id, Double> baseKm, Scenario baseScenario) {
        for (Map.Entry<Id, Double> entry : baseKm.entrySet()) {
            Double km = entry.getValue();
            if (km != 0.0) {
                String id = entry.getKey().toString();
                String originalId;
                if (id.startsWith("I"))
                    originalId = id.substring(id.indexOf("_")+1);
                else
                    originalId = id;
                pw.printf("%s\t%s\t%s\t%s\t%f\n", entry.getKey().toString(), rate, ccase, CountWorkers.isWorker(baseScenario.getPopulation().getPersons().get(new IdImpl(originalId))) ? "workers" : "non-workers", km);
                pw.printf("%s\t%s\t%s\t%s\t%f\n", entry.getKey().toString(), rate, ccase, "all", km);
            }
        }
    }

    public RunResource getRateRun(String rate, String variant) {
        return new RunResource(WD + "/rates/" + rate + "/" + variant, null);
    }


    static int[] getVolumesForLink(VolumesAnalyzer volumesAnalyzer1, Id linkId) {
        int maxSlotIndex = (MAX_TIME / TIME_BIN_SIZE) + 1;
        int[] maybeVolumes = volumesAnalyzer1.getVolumesForLink(linkId);
        if(maybeVolumes == null) {
            return new int[maxSlotIndex + 1];
        }
        return maybeVolumes;
    }


    private double sum(Collection<Double> values) {
        double result = 0.0;
        for (double summand : values) {
            result += summand;
        }
        return result;
    }



    private static Double zeroForNull(Double maybeDouble) {
        if (maybeDouble == null) {
            return 0.0;
        }
        return maybeDouble;
    }

    public void putPersonKilometers(final BufferedReader personKilometers) {
        final String filename = WD + "/person-kilometers.txt";
        FileIO.writeToFile(filename, new StreamingOutput() {
            @Override
            public void write(final PrintWriter pw) throws IOException {
                FileIO.readFromInput(personKilometers, new Reading() {
                    @Override
                    public void read(BufferedReader br) throws IOException {
                        String line = br.readLine();
                        while (br != null) {
                            pw.println(line);
                        }
                    }
                });
            }
        });
    }

    public StreamingOutput getPersonKilometers() {
        final String filename = WD + "/person-kilometers.txt";
        return new StreamingOutput() {
            @Override
            public void write(final PrintWriter pw) throws IOException {
                FileIO.readFromFile(filename, new Reading() {
                    @Override
                    public void read(BufferedReader br) throws IOException {
                        String line = br.readLine();
                        while (line != null) {
                            pw.println(line);
                            line = br.readLine();
                        }
                    }
                });
            }
        };
    }

}
