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

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.name.Names;
import org.matsim.analysis.VolumesAnalyzer;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.cadyts.general.CadytsConfigGroup;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.ScoringFunctionFactory;
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
import playground.mzilske.clones.ClonesModule;
import playground.mzilske.controller.Controller;
import playground.mzilske.controller.ControllerModule;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

class MultiRateRunResource {

    private String WD;

    private String regime;

    public MultiRateRunResource(String wd, String regime) {
        this.WD = wd;
        this.regime = regime;
    }

    final static int TIME_BIN_SIZE = 60*60;
    final static int MAX_TIME = 30 * TIME_BIN_SIZE - 1;

    public Collection<String> getRates() {
        final List<String> RATES = new ArrayList<String>();
        RATES.add("0");
        RATES.add("5");
        return RATES;
    }

    private RunResource getBaseRun() {
        return new RegimeResource(WD + "/../..", regime).getBaseRun();
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

            @Override
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


        final Sightings allSightings = new SightingsImpl(compareMain.getSightingsPerPerson());

        final Config config = phoneConfig();
        config.controler().setOutputDirectory(WD + "/rates/" + rate);

        final ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(config);
        scenario.setNetwork(baseScenario.getNetwork());

        // PopulationFromSightings.createPopulationWithRandomEndTimesInPermittedWindow(scenario, linkToZoneResolver, allSightings);
        // PopulationFromSightings.preparePopulation(scenario, linkToZoneResolver, allSightings);

        String rateDir = WD + "/rates/" + rate;
        new File(rateDir).mkdirs();

        // new PopulationWriter(scenario.getPopulation(), scenario.getNetwork()).write(rateDir + "/input_population.xml.gz");
        new SightingsWriter(allSightings).write(rateDir + "/sightings.txt");
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

        // new MatsimPopulationReader(scenario).readFile(WD + "/rates/" + rate + "/input_population.xml.gz");
        Sightings allSightings = new SightingsImpl(new HashMap<Id, List<Sighting>>());
        new SightingsReader(allSightings).read(IOUtils.getInputStream(WD + "/rates/" + rate + "/sightings.txt"));
        ZoneTracker.LinkToZoneResolver linkToZoneResolver = new ZoneTracker.LinkToZoneResolver() {

            @Override
            public Id resolveLinkToZone(Id linkId) {
                return linkId;
            }

            public IdImpl chooseLinkInZone(String zoneId) {
                return new IdImpl(zoneId);
            }

        };
        PopulationFromSightings.createPopulationWithRandomEndTimesInPermittedWindow(scenario, linkToZoneResolver, allSightings);
        PopulationFromSightings.preparePopulation(scenario, linkToZoneResolver, allSightings);


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
            }
        });

        Injector injector2 = Guice.createInjector(modules);
        Controller controler2 = injector2.getInstance(Controller.class);
        controler2.run();
    }

    static Counts filterCounts(Counts allCounts) {
        Counts someCounts = new Counts();
        for (Map.Entry<Id<Link>, Count> entry: allCounts.getCounts().entrySet()) {
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

        {
            StrategySettings stratSets = new StrategySettings(new IdImpl(1));
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

}
