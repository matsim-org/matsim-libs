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

import org.matsim.analysis.VolumesAnalyzer;
import org.matsim.analysis.VolumesAnalyzerModule;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.cadyts.general.CadytsConfigGroup;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.StrategyConfigGroup;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.ControlerDefaultsModule;
import org.matsim.core.controler.ReplayEvents;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.counts.Count;
import org.matsim.counts.Counts;
import org.matsim.counts.CountsReaderMatsimV1;
import org.matsim.counts.CountsWriter;
import playground.mzilske.ant2014.FileIO;
import playground.mzilske.ant2014.IterationResource;
import playground.mzilske.ant2014.StreamingOutput;
import playground.mzilske.cadyts.CadytsModule;
import playground.mzilske.cdr.*;
import playground.mzilske.clones.ClonesConfigGroup;
import playground.mzilske.clones.ClonesModule;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

public class MultiRateRunResource {

    private static final int LAST_ITERATION = 100;
    private final String WD;

    private final String regime;
    private final String alternative;

    public MultiRateRunResource(String wd, String regime, String alternative) {
        this.WD = wd;
        this.regime = regime;
        this.alternative = alternative;
    }

    private final static int TIME_BIN_SIZE = 60 * 60;
    private final static int MAX_TIME = 30 * TIME_BIN_SIZE - 1;

    Collection<String> getRates() {
        if (alternative.equals("cutoff")) {
            final List<String> rates = new ArrayList<>();
            rates.add("70-30");
            rates.add("100-100");
            rates.add("50-50");
            rates.add("90-10");
            rates.add("100-0");
            return rates;
        } else if (alternative.equals("random") || alternative.equals("brute") || alternative.equals("cadyts")) {
            final List<String> rates = new ArrayList<>();
            rates.add("0");
            rates.add("5");
            return rates;
        } else {
            throw new RuntimeException("Unknown alternative.");
        }
    }

    public RunResource getBaseRun() {
        return new RegimeResource(WD + "/../..", regime).getBaseRun();
    }

    public void twoRates(String string) {
        final Scenario baseScenario = getBaseRun().getLastIteration().getExperiencedPlansAndNetwork();
        final double rate = Integer.parseInt(string);
        for (Person person : baseScenario.getPopulation().getPersons().values()) {
            if (CountWorkers.isWorker(person)) {
                person.getCustomAttributes().put("phonerate", 50.0);
            } else {
                person.getCustomAttributes().put("phonerate", rate);
            }
        }
        ZoneTracker.LinkToZoneResolver linkToZoneResolver = new LinkIsZone();
        CallBehavior callBehavior = new PhoneRateAttributeCallBehavior(baseScenario);
        ReplayEvents.Results results = ReplayEvents.run(
                baseScenario,
                getBaseRun().getLastIteration().getEventsFileName(),
                new VolumesAnalyzerModule(),
                new CollectSightingsModule(),
                new CallBehaviorModule(callBehavior, linkToZoneResolver));


        final Sightings sightings = results.get(Sightings.class);
        final VolumesAnalyzer groundTruthVolumes = results.get(VolumesAnalyzer.class);

        String rateDir = WD + "/rates/" + string;
        new File(rateDir).mkdirs();

        new SightingsWriter(sightings).write(rateDir + "/sightings.txt");
        final Counts allCounts = CompareMain.volumesToCounts(baseScenario.getNetwork(), groundTruthVolumes, 1.0);
        allCounts.setYear(2012);
        new CountsWriter(allCounts).write(rateDir + "/all_counts.xml.gz");
        final Counts someCounts = filterCounts(allCounts);
        someCounts.setYear(2012);
        new CountsWriter(someCounts).write(rateDir + "/calibration_counts.xml.gz");
    }

    public void simulateRate(String rate, final int cloneFactor, final double cadytsWeight) {
        final Config config = phoneConfig(cloneFactor);

        config.controler().setOutputDirectory(WD + "/rates/" + rate + "/" + cloneFactor);

        Scenario baseScenario = getBaseRun().getConfigAndNetwork();
        final ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(config);
        scenario.setNetwork(baseScenario.getNetwork());

        final Sightings allSightings = getSightings(rate);
        final ZoneTracker.LinkToZoneResolver linkToZoneResolver = new LinkIsZone();

        PopulationFromSightings.createPopulationWithRandomRealization(scenario, allSightings, linkToZoneResolver);

        final Counts allCounts = new Counts();
        new CountsReaderMatsimV1(allCounts).parse(WD + "/rates/" + rate + "/all_counts.xml.gz");
        final Counts someCounts = new Counts();
        new CountsReaderMatsimV1(someCounts).parse(WD + "/rates/" + rate + "/calibration_counts.xml.gz");

        scenario.addScenarioElement(Counts.ELEMENT_NAME, allCounts);
        scenario.addScenarioElement("calibrationCounts", someCounts);

        ClonesConfigGroup clonesConfig = ConfigUtils.addOrGetModule(config, ClonesConfigGroup.NAME, ClonesConfigGroup.class);
        clonesConfig.setCloneFactor(cloneFactor);

        Controler controler = new Controler(scenario);
        controler.setOverwriteFiles(true);
        controler.setModules(
                new ControlerDefaultsModule(),
                new CadytsModule(),
                new ClonesModule(),
                new TrajectoryReRealizerModule(),
                new AbstractModule() {
                    @Override
                    public void install() {
                        bind(ZoneTracker.LinkToZoneResolver.class).toInstance(linkToZoneResolver);
                        bind(Sightings.class).toInstance(allSightings);
                    }
                });
        CadytsAndCloneScoringFunctionFactory factory = new CadytsAndCloneScoringFunctionFactory();
        factory.setCadytsweight(cadytsWeight);
        controler.setScoringFunctionFactory(factory);
        controler.run();
    }

    public Sightings getSightings(String rate) {
        final Sightings allSightings = new SightingsImpl();
        new SightingsReader(allSightings).read(IOUtils.getInputStream(WD + "/rates/" + rate + "/sightings.txt"));
        return allSightings;
    }

    Counts filterCounts(Counts allCounts) {
        Counts someCounts = new Counts();
        if (alternative.startsWith("randomcountlocations")) {
            for (Map.Entry<Id<Link>, Count> entry : allCounts.getCounts().entrySet()) {
                if (Math.random() < 0.05) {
                    someCounts.getCounts().put(entry.getKey(), entry.getValue());
                }
            }
        } else if (alternative.startsWith("realcountlocations")) {
            final Counts originalCounts = new Counts();
            new CountsReaderMatsimV1(originalCounts).parse(getBaseRun().getWd() + "/counts.xml");
            for (Map.Entry<Id<Link>, Count> entry : allCounts.getCounts().entrySet()) {
                if (originalCounts.getCounts().keySet().contains(entry.getKey())) {
                    someCounts.getCounts().put(entry.getKey(), entry.getValue());
                }
            }
        } else {
            throw new RuntimeException();
        }
        return someCounts;
    }

    private Config phoneConfig(int cloneFactor) {
        Config config = ConfigUtils.createConfig();

        config.controler().setLastIteration(LAST_ITERATION);
        ActivityParams sightingParam = new ActivityParams("sighting");
        sightingParam.setTypicalDuration(30.0 * 60);
        config.controler().setWritePlansInterval(100);
        config.planCalcScore().addActivityParams(sightingParam);
        config.planCalcScore().setTraveling_utils_hr(-6);
        config.planCalcScore().setPerforming_utils_hr(0);
        config.planCalcScore().setTravelingOther_utils_hr(-6);
        config.planCalcScore().setConstantCar(0);
        config.planCalcScore().setMonetaryDistanceCostRateCar(0);
        CadytsConfigGroup cadytsConfig = ConfigUtils.addOrGetModule(config, CadytsConfigGroup.GROUP_NAME, CadytsConfigGroup.class);
        cadytsConfig.setVarianceScale(0.001);
        cadytsConfig.setMinFlowStddev_vehPerHour(2.0);


        cadytsConfig.setPreparatoryIterations(1);

        config.qsim().setFlowCapFactor(100);
        config.qsim().setStorageCapFactor(100);
        config.qsim().setRemoveStuckVehicles(false);

        {
            StrategySettings stratSets = new StrategySettings(Id.create(1, StrategySettings.class));
            stratSets.setStrategyName("SelectExpBeta");
            stratSets.setWeight(1.0);
            config.strategy().addStrategySettings(stratSets);
        }
//        {
//            StrategySettings stratSets = new StrategySettings(new IdImpl(2));
//            stratSets.setModuleName("SelectRandom");
//            stratSets.setProbability(0.1);
//            stratSets.setDisableAfter(30);
//            config.strategy().addStrategySettings(stratSets);
//        }
        {
            StrategyConfigGroup.StrategySettings stratSets = new StrategyConfigGroup.StrategySettings(Id.create(2, StrategySettings.class));
            stratSets.setStrategyName("ReRealize");
            stratSets.setWeight(0.3 / cloneFactor);
            stratSets.setDisableAfter((int) (LAST_ITERATION * 0.8));
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

    public void summary() {
        final String filename = WD + "/summary.txt";
        final Scenario baseScenario = getBaseRun().getLastIteration().getExperiencedPlansAndNetwork();
        FileIO.writeToFile(filename, new StreamingOutput() {
            @Override
            public void write(PrintWriter pw) throws IOException {
                pw.printf("rate\tkilometers\tpeople\n");
                for (String rate : getRates()) {
                    Scenario scenario = getRateRun(rate, "3").getOutputScenario();
                    dumpSums(pw, rate, scenario);
                }
                dumpSums(pw, "base", baseScenario);
            }
        });
    }

    private void dumpSums(PrintWriter pw, String rate, Scenario scenario) {
        Map<Id, Double> km = PowerPlans.travelledDistancePerPerson(scenario.getPopulation(), scenario.getNetwork());
        double kmSum = 0.0;
        int nPeople = 0;
        for (double ikm : km.values()) {
            kmSum += ikm;
            if (ikm != 0.0) {
                nPeople++;
            }
        }
        pw.printf("%s\t%d\t%d\n", rate, (int) (kmSum / 1000.0), nPeople);
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
                    dumpnonzero(pw, rate, "truth", baseKm, baseScenario);
                    {
                        Scenario scenario = getRateRun(rate, "3").getOutputScenario();
                        Map<Id, Double> km = PowerPlans.travelledDistancePerPerson(scenario.getPopulation(), baseScenario.getNetwork());
                        dumpnonzero(pw, rate, "calibrated", km, baseScenario);
                    }
                    {
                        ArrayList<Person> it0 = new ArrayList<>(getRateRun(rate, "3").getIteration(0).getPlans().getPersons().values());
                        for (Iterator<Person> i = it0.iterator(); i.hasNext(); ) {
                            Person person = i.next();
                            if (person.getId().toString().startsWith("I")) {
                                i.remove();
                            } else {
                                person.setSelectedPlan(person.getPlans().get(0));
                            }
                        }
                        Map<Id, Double> km = PowerPlans.travelledDistancePerPerson(baseScenario.getNetwork(), it0);
                        dumpnonzero(pw, rate, "initial", km, baseScenario);
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
                    originalId = id.substring(id.indexOf("_") + 1);
                else
                    originalId = id;
                pw.printf("%s\t%s\t%s\t%s\t%f\n", entry.getKey().toString(), rate, ccase, CountWorkers.isWorker(baseScenario.getPopulation().getPersons().get(Id.create(originalId, Person.class))) ? "workers" : "non-workers", km);
                pw.printf("%s\t%s\t%s\t%s\t%f\n", entry.getKey().toString(), rate, ccase, "all", km);
            }
        }
    }

    RunResource getRateRun(String rate, String variant) {
        return new RunResource(WD + "/rates/" + rate + "/" + variant, null);
    }


    private static int[] getVolumesForLink(VolumesAnalyzer volumesAnalyzer1, Id linkId) {
        int maxSlotIndex = (MAX_TIME / TIME_BIN_SIZE) + 1;
        int[] maybeVolumes = volumesAnalyzer1.getVolumesForLink(linkId);
        if (maybeVolumes == null) {
            return new int[maxSlotIndex + 1];
        }
        return maybeVolumes;
    }

    public void cutoffRate(double worker, double nonworker) {
        final Scenario baseScenario = getBaseRun().getLastIteration().getExperiencedPlansAndNetwork();
        for (Person person : baseScenario.getPopulation().getPersons().values()) {
            double shareOfOftenCallers = CountWorkers.isWorker(person) ? worker : nonworker;
            person.getCustomAttributes().put("phonerate", Math.random() < shareOfOftenCallers ? 50 : 0);
        }
        ZoneTracker.LinkToZoneResolver linkToZoneResolver = new LinkIsZone();
        CallBehavior phonerate = new PhoneRateAttributeCallBehavior(baseScenario);
        ReplayEvents.Results results = ReplayEvents.run(
                baseScenario,
                getBaseRun().getLastIteration().getEventsFileName(),
                new VolumesAnalyzerModule(),
                new CollectSightingsModule(),
                new CallBehaviorModule(phonerate, linkToZoneResolver));


        final Sightings sightings = results.get(Sightings.class);
        final VolumesAnalyzer groundTruthVolumes = results.get(VolumesAnalyzer.class);

        String rateDir = WD + "/rates/" + (int) (worker * 100) + "-" + (int) (nonworker * 100);
        new File(rateDir).mkdirs();

        new SightingsWriter(sightings).write(rateDir + "/sightings.txt");
        final Counts allCounts = CompareMain.volumesToCounts(baseScenario.getNetwork(), groundTruthVolumes, 1.0);
        allCounts.setYear(2012);
        new CountsWriter(allCounts).write(rateDir + "/all_counts.xml.gz");
        final Counts someCounts = filterCounts(allCounts);
        someCounts.setYear(2012);
        new CountsWriter(someCounts).write(rateDir + "/calibration_counts.xml.gz");
    }

    public void cutOffExact(double worker, double nonworker) {
        final Scenario baseScenario = getBaseRun().getLastIteration().getExperiencedPlansAndNetwork();
        for (Person person : baseScenario.getPopulation().getPersons().values()) {
            double shareOfOftenCallers = CountWorkers.isWorker(person) ? worker : nonworker;
            person.getCustomAttributes().put("phonerate", Math.random() < shareOfOftenCallers ? 50 : 0);
        }
        CallBehavior callBehavior = new CallBehavior() {

            @Override
            public boolean makeACall(ActivityEndEvent event) {
                Person person = baseScenario.getPopulation().getPersons().get(event.getPersonId());
                return (Integer) person.getCustomAttributes().get("phonerate") == 50;
            }

            @Override
            public boolean makeACall(ActivityStartEvent event) {
                Person person = baseScenario.getPopulation().getPersons().get(event.getPersonId());
                return (Integer) person.getCustomAttributes().get("phonerate") == 50;
            }

            @Override
            public boolean makeACallAtMorningAndNight(Id<Person> id) {
                Person person = baseScenario.getPopulation().getPersons().get(id);
                return (Integer) person.getCustomAttributes().get("phonerate") == 50;
            }

        };
        ZoneTracker.LinkToZoneResolver linkToZoneResolver = new LinkIsZone();
        ReplayEvents.Results results = ReplayEvents.run(
                baseScenario,
                getBaseRun().getLastIteration().getEventsFileName(),
                new VolumesAnalyzerModule(),
                new CollectSightingsModule(),
                new CallBehaviorModule(callBehavior, linkToZoneResolver));


        final Sightings sightings = results.get(Sightings.class);
        final VolumesAnalyzer groundTruthVolumes = results.get(VolumesAnalyzer.class);

        String rateDir = WD + "/rates/" + (int) (worker * 100) + "-" + (int) (nonworker * 100);
        new File(rateDir).mkdirs();

        new SightingsWriter(sightings).write(rateDir + "/sightings.txt");
        final Counts allCounts = CompareMain.volumesToCounts(baseScenario.getNetwork(), groundTruthVolumes, 1.0);
        allCounts.setYear(2012);
        new CountsWriter(allCounts).write(rateDir + "/all_counts.xml.gz");
        final Counts someCounts = filterCounts(allCounts);
        someCounts.setYear(2012);
        new CountsWriter(someCounts).write(rateDir + "/calibration_counts.xml.gz");
    }

    public void twoRatesRandom(String string) {
        final Scenario baseScenario = getBaseRun().getLastIteration().getExperiencedPlansAndNetwork();
        final int rate = Integer.parseInt(string);
        List<Person> persons = new ArrayList<>(baseScenario.getPopulation().getPersons().values());
        Collections.shuffle(persons, new Random(42));
        int i = 0;
        for (Person person : persons) {
            if (i < 9523) { // number of workers
                person.getCustomAttributes().put("phonerate", 50);
            } else {
                person.getCustomAttributes().put("phonerate", rate);
            }
            i++;
        }
        ZoneTracker.LinkToZoneResolver linkToZoneResolver = new LinkIsZone();
        CallBehavior phonerate = new PhoneRateAttributeCallBehavior(baseScenario);
        ReplayEvents.Results results = ReplayEvents.run(
                baseScenario,
                getBaseRun().getLastIteration().getEventsFileName(),
                new VolumesAnalyzerModule(),
                new CollectSightingsModule(),
                new CallBehaviorModule(phonerate, linkToZoneResolver));


        final Sightings sightings = results.get(Sightings.class);
        final VolumesAnalyzer groundTruthVolumes = results.get(VolumesAnalyzer.class);

        String rateDir = WD + "/rates/" + rate;
        new File(rateDir).mkdirs();

        new SightingsWriter(sightings).write(rateDir + "/sightings.txt");
        final Counts allCounts = CompareMain.volumesToCounts(baseScenario.getNetwork(), groundTruthVolumes, 1.0);
        allCounts.setYear(2012);
        new CountsWriter(allCounts).write(rateDir + "/all_counts.xml.gz");
        final Counts someCounts = filterCounts(allCounts);
        someCounts.setYear(2012);
        new CountsWriter(someCounts).write(rateDir + "/calibration_counts.xml.gz");
    }

    private static class PhoneRateAttributeCallBehavior implements CallBehavior {

        private final Scenario baseScenario;

        public PhoneRateAttributeCallBehavior(Scenario baseScenario) {
            this.baseScenario = baseScenario;
        }

        @Override
        public boolean makeACall(ActivityEndEvent event) {
            return false;
        }

        @Override
        public boolean makeACall(ActivityStartEvent event) {
            return false;
        }

        @Override
        public boolean makeACallAtMorningAndNight(Id<Person> id) {
            return false;
        }

    }

}
