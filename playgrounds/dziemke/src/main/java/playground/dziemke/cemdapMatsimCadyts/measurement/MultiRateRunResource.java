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

package playground.dziemke.cemdapMatsimCadyts.measurement;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.cadyts.general.CadytsConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;

public class MultiRateRunResource {

//    enum CountLocations {
//        Real, Random;
//    }
//
//    private static final CountLocations COUNT_LOCATIONS = CountLocations.Real;
//
//    private final String WD;
//
//    private final String regime;
//    private final String alternative;
//
//    public MultiRateRunResource(String wd, String regime, String alternative) {
//        this.WD = wd;
//        this.regime = regime;
//        this.alternative = alternative;
//    }
//
//    private final static int TIME_BIN_SIZE = 60 * 60;
//    private final static int MAX_TIME = 30 * TIME_BIN_SIZE - 1;
//
//    Collection<String> getRates() {
//        final List<String> rates = new ArrayList<>();
//        rates.add("5.0");
//        return rates;
//    }
//
//    public RunResource getBaseRun() {
//        return new RegimeResource(WD + "/src", regime).getBaseRun();
//    }
//
//    public void twoRates(String string) {
//        final Scenario baseScenario = getBaseRun().getLastIteration().getExperiencedPlansAndNetwork();
//        final double rate = Integer.parseInt(string);
//        for (Person person : baseScenario.getPopulation().getPersons().values()) {
//            if (CountWorkers.isWorker(person)) {
//                person.getCustomAttributes().put("phonerate", 50.0);
//            } else {
//                person.getCustomAttributes().put("phonerate", rate);
//            }
//        }
//        ZoneTracker.LinkToZoneResolver linkToZoneResolver = new LinkIsZone();
//        CallBehavior callBehavior = new PhoneRateAttributeCallBehavior(baseScenario);
//        ReplayEvents.Results results = ReplayEvents.run(
//                baseScenario.getConfig(),
//                getBaseRun().getLastIteration().getEventsFileName(),
//                new ScenarioByInstanceModule(baseScenario),
//                new VolumesAnalyzerModule(),
//                new CollectSightingsModule(),
//                new CallBehaviorModule(callBehavior, linkToZoneResolver));
//
//
//        final Sightings sightings = results.get(Sightings.class);
//        final VolumesAnalyzer groundTruthVolumes = results.get(VolumesAnalyzer.class);
//
//        String rateDir = WD + "/rates/" + string;
//        new File(rateDir).mkdirs();
//
//        new SightingsWriter(sightings).write(rateDir + "/sightings.txt");
//        final Counts allCounts = CompareMain.volumesToCounts(baseScenario.getNetwork(), groundTruthVolumes, 1.0);
//        allCounts.setYear(2012);
//        new CountsWriter(allCounts).write(rateDir + "/all_counts.xml.gz");
//        final Counts someCounts = filterCounts(allCounts);
//        someCounts.setYear(2012);
//        new CountsWriter(someCounts).write(rateDir + "/calibration_counts.xml.gz");
//    }
//
//
//    public Sightings getSightings(String rate) {
//        final Sightings allSightings = new SightingsImpl();
//        new SightingsReader(allSightings).read(IOUtils.getInputStream(WD + "/rates/" + rate + "/sightings.txt"));
//        return allSightings;
//    }
//
//    Counts filterCounts(Counts<Link> allCounts) {
//        Counts someCounts = new Counts();
//        if (COUNT_LOCATIONS == CountLocations.Random) {
//            for (Map.Entry<Id<Link>, Count<Link>> entry : allCounts.getCounts().entrySet()) {
//                if (Math.random() < 0.05) {
//                    someCounts.getCounts().put(entry.getKey(), entry.getValue());
//                }
//            }
//        } else if (COUNT_LOCATIONS == CountLocations.Real) {
//            final Counts originalCounts = new Counts();
//            new CountsReaderMatsimV1(originalCounts).parse(getBaseRun().getWd() + "/2kW.15.output_counts.xml");
//            for (Map.Entry<Id<Link>, Count<Link>> entry : allCounts.getCounts().entrySet()) {
//                if (originalCounts.getCounts().keySet().contains(entry.getKey())) {
//                    someCounts.getCounts().put(entry.getKey(), entry.getValue());
//                }
//            }
//        } else {
//            throw new RuntimeException();
//        }
//        return someCounts;
//    }

    public static Config phoneConfig(int lastIteration, double cloneFactor) {
        Config config = ConfigUtils.createConfig();
        config.global().setNumberOfThreads(8);
        config.controler().setLastIteration(lastIteration);
        ActivityParams sightingParam = new ActivityParams("sighting");
        sightingParam.setTypicalDuration(30.0 * 60);
        config.controler().setWritePlansInterval(100);
        config.planCalcScore().addActivityParams(sightingParam);
        config.planCalcScore().setPerforming_utils_hr(0);
        config.planCalcScore().getModes().get("car").setMarginalUtilityOfTraveling(-6);
        config.planCalcScore().getModes().get("car").setConstant(0);
        config.planCalcScore().getModes().get("car").setMonetaryDistanceRate(0);
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
//            StrategySettings stratSets = new StrategyConfigGroup.StrategySettings(Id.create(3, StrategySettings.class));
//            stratSets.setStrategyName("SelectRandom");
//            stratSets.setWeight(0.1 / cloneFactor);
//            stratSets.setDisableAfter((int) (lastIteration * 0.5));
//            config.strategy().addStrategySettings(stratSets);
//        }
//        {
//            StrategyConfigGroup.StrategySettings stratSets = new StrategyConfigGroup.StrategySettings(Id.create(2, StrategySettings.class));
//            stratSets.setStrategyName("ReRealize");
//            stratSets.setWeight(0.1 / cloneFactor);
//            stratSets.setDisableAfter((int) (lastIteration * 0.8));
//            config.strategy().addStrategySettings(stratSets);
//        }

        return config;

    }

//    public void errors() {
//        final String filename = WD + "/errors.txt";
//        final Scenario baseScenario = getBaseRun().getLastIteration().getExperiencedPlansAndNetwork();
//        EventsManager events = EventsUtils.createEventsManager();
//        final VolumesAnalyzer baseVolumes = new VolumesAnalyzer(TIME_BIN_SIZE, MAX_TIME, baseScenario.getNetwork());
//        events.addHandler(baseVolumes);
//        new MatsimEventsReader(events).readFile(getBaseRun().getLastIteration().getEventsFileName());
//        FileIO.writeToFile(filename, new StreamingOutput() {
//            @Override
//            public void write(PrintWriter pw) throws IOException {
//                pw.printf("regime\trate\tclonefactor\tmre\tmae\tmab\n");
//                for (String rate : getRates()) {
//                    Collection<String> cloneFactors = getCloneFactors(rate);
//                    for (String cloneFactor : cloneFactors) {
//                        final IterationResource lastIteration = getRateRun(rate, cloneFactor).getLastIteration();
//                        EventsManager events1 = EventsUtils.createEventsManager();
//                        VolumesAnalyzer volumes = new VolumesAnalyzer(TIME_BIN_SIZE, MAX_TIME, baseScenario.getNetwork());
//                        events1.addHandler(volumes);
//                        new MatsimEventsReader(events1).readFile(lastIteration.getEventsFileName());
//                        double meanRelativeError = 0;
//                        double meanAbsoluteError = meanAbsoluteError(baseVolumes, volumes);
//                        double meanAbsoluteBias = meanAbsoluteBias(baseVolumes, volumes);
//                        pw.printf("%s\t%s\t%s\t%f\t%f\t%f\n",
//                                regime, rate, cloneFactor, meanRelativeError, meanAbsoluteError, meanAbsoluteBias);
//                        pw.flush();
//                    }
//                }
//            }
//        });
//    }
//
//    private Collection<String> getCloneFactors(String rate) {
//        return Arrays.asList("10");
//    }
//
//    private double meanAbsoluteError(VolumesAnalyzer baseVolumes, VolumesAnalyzer volumes) {
//        double result = 0.0;
//        double num = 0.0;
//        for (Id id : baseVolumes.getLinkIds()) {
//            int[] us = getVolumesForLink(baseVolumes, id);
//            int[] vs = getVolumesForLink(volumes, id);
//            for (int i = 0; i < us.length; i++) {
//                result += Math.abs(us[i] - vs[i]);
//            }
//            num += us.length;
//        }
//        return result / num;
//    }
//
//    private double meanAbsoluteBias(VolumesAnalyzer baseVolumes, VolumesAnalyzer volumes) {
//        double result = 0.0;
//        double num = 0.0;
//        for (Id id : baseVolumes.getLinkIds()) {
//            int[] us = getVolumesForLink(baseVolumes, id);
//            int[] vs = getVolumesForLink(volumes, id);
//            for (int i = 0; i < us.length; i++) {
//                result += vs[i] - us[i];
//            }
//            num += us.length;
//        }
//        return result / num;
//    }
//
//    public void summary() {
//        final String filename = WD + "/summary.txt";
//        final Scenario baseScenario = getBaseRun().getLastIteration().getExperiencedPlansAndNetwork();
//        FileIO.writeToFile(filename, new StreamingOutput() {
//            @Override
//            public void write(PrintWriter pw) throws IOException {
//                pw.printf("rate\tkilometers\tpeople\n");
//                for (String rate : getRates()) {
//                    Scenario scenario = getRateRun(rate, "3").getOutputScenario();
//                    dumpSums(pw, rate, scenario);
//                }
//                dumpSums(pw, "base", baseScenario);
//            }
//        });
//    }
//
//    private void dumpSums(PrintWriter pw, String rate, Scenario scenario) {
//        Map<Id, Double> km = PowerPlans.travelledDistancePerPerson(scenario.getPopulation(), scenario.getNetwork());
//        double kmSum = 0.0;
//        int nPeople = 0;
//        for (double ikm : km.values()) {
//            kmSum += ikm;
//            if (ikm != 0.0) {
//                nPeople++;
//            }
//        }
//        pw.printf("%s\t%d\t%d\n", rate, (int) (kmSum / 1000.0), nPeople);
//    }
//
//    public RunResource getRateRun(String rate, String variant) {
//        return new RunResource(WD + "/rates/" + rate + "/" + variant, null);
//    }
//
//
//    private static int[] getVolumesForLink(VolumesAnalyzer volumesAnalyzer1, Id linkId) {
//        int maxSlotIndex = (MAX_TIME / TIME_BIN_SIZE) + 1;
//        int[] maybeVolumes = volumesAnalyzer1.getVolumesForLink(linkId);
//        if (maybeVolumes == null) {
//            return new int[maxSlotIndex + 1];
//        }
//        return maybeVolumes;
//    }
//
//    public void cutoffRate(double worker, double nonworker) {
//        final Scenario baseScenario = getBaseRun().getLastIteration().getExperiencedPlansAndNetwork();
//        for (Person person : baseScenario.getPopulation().getPersons().values()) {
//            double shareOfOftenCallers = CountWorkers.isWorker(person) ? worker : nonworker;
//            person.getCustomAttributes().put("phonerate", Math.random() < shareOfOftenCallers ? 50 : 0);
//        }
//        ZoneTracker.LinkToZoneResolver linkToZoneResolver = new LinkIsZone();
//        CallBehavior phonerate = new PhoneRateAttributeCallBehavior(baseScenario);
//        ReplayEvents.Results results = ReplayEvents.run(
//                baseScenario.getConfig(),
//                getBaseRun().getLastIteration().getEventsFileName(),
//                new ScenarioByInstanceModule(baseScenario),
//                new VolumesAnalyzerModule(),
//                new CollectSightingsModule(),
//                new CallBehaviorModule(phonerate, linkToZoneResolver));
//
//
//        final Sightings sightings = results.get(Sightings.class);
//        final VolumesAnalyzer groundTruthVolumes = results.get(VolumesAnalyzer.class);
//
//        String rateDir = WD + "/rates/" + (int) (worker * 100) + "-" + (int) (nonworker * 100);
//        new File(rateDir).mkdirs();
//
//        new SightingsWriter(sightings).write(rateDir + "/sightings.txt");
//        final Counts allCounts = CompareMain.volumesToCounts(baseScenario.getNetwork(), groundTruthVolumes, 1.0);
//        allCounts.setYear(2012);
//        new CountsWriter(allCounts).write(rateDir + "/all_counts.xml.gz");
//        final Counts someCounts = filterCounts(allCounts);
//        someCounts.setYear(2012);
//        new CountsWriter(someCounts).write(rateDir + "/calibration_counts.xml.gz");
//    }
//
//    public void cutOffExact(double worker, double nonworker) {
//        final Scenario baseScenario = getBaseRun().getLastIteration().getExperiencedPlansAndNetwork();
//        for (Person person : baseScenario.getPopulation().getPersons().values()) {
//            double shareOfOftenCallers = CountWorkers.isWorker(person) ? worker : nonworker;
//            person.getCustomAttributes().put("phonerate", Math.random() < shareOfOftenCallers ? 50 : 0);
//        }
//        CallBehavior callBehavior = new CallBehavior() {
//
//            @Override
//            public boolean makeACall(ActivityEndEvent event) {
//                Person person = baseScenario.getPopulation().getPersons().get(event.getPersonId());
//                return (Integer) person.getCustomAttributes().get("phonerate") == 50;
//            }
//
//            @Override
//            public boolean makeACall(ActivityStartEvent event) {
//                Person person = baseScenario.getPopulation().getPersons().get(event.getPersonId());
//                return (Integer) person.getCustomAttributes().get("phonerate") == 50;
//            }
//
//            @Override
//            public boolean makeACallAtMorningAndNight(Id<Person> id) {
//                Person person = baseScenario.getPopulation().getPersons().get(id);
//                return (Integer) person.getCustomAttributes().get("phonerate") == 50;
//            }
//
//        };
//        ZoneTracker.LinkToZoneResolver linkToZoneResolver = new LinkIsZone();
//        ReplayEvents.Results results = ReplayEvents.run(
//                baseScenario.getConfig(),
//                getBaseRun().getLastIteration().getEventsFileName(),
//                new ScenarioByInstanceModule(baseScenario),
//                new VolumesAnalyzerModule(),
//                new CollectSightingsModule(),
//                new CallBehaviorModule(callBehavior, linkToZoneResolver));
//
//
//        final Sightings sightings = results.get(Sightings.class);
//        final VolumesAnalyzer groundTruthVolumes = results.get(VolumesAnalyzer.class);
//
//        String rateDir = WD + "/rates/" + (int) (worker * 100) + "-" + (int) (nonworker * 100);
//        new File(rateDir).mkdirs();
//
//        new SightingsWriter(sightings).write(rateDir + "/sightings.txt");
//        final Counts allCounts = CompareMain.volumesToCounts(baseScenario.getNetwork(), groundTruthVolumes, 1.0);
//        allCounts.setYear(2012);
//        new CountsWriter(allCounts).write(rateDir + "/all_counts.xml.gz");
//        final Counts someCounts = filterCounts(allCounts);
//        someCounts.setYear(2012);
//        new CountsWriter(someCounts).write(rateDir + "/calibration_counts.xml.gz");
//    }
//
//    private static class PhoneRateAttributeCallBehavior implements CallBehavior {
//
//        private final Scenario baseScenario;
//
//        public PhoneRateAttributeCallBehavior(Scenario baseScenario) {
//            this.baseScenario = baseScenario;
//        }
//
//        @Override
//        public boolean makeACall(ActivityEndEvent event) {
//            return false;
//        }
//
//        @Override
//        public boolean makeACall(ActivityStartEvent event) {
//            return false;
//        }
//
//        @Override
//        public boolean makeACallAtMorningAndNight(Id<Person> id) {
//            return false;
//        }
//
//    }

}
