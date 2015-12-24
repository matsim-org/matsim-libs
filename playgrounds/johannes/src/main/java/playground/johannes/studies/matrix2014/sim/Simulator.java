/* *********************************************************************** *
 * project: org.matsim.*
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

package playground.johannes.studies.matrix2014.sim;

import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.analysis.UnivariateRealFunction;
import org.apache.log4j.Logger;
import org.matsim.contrib.common.stats.Discretizer;
import org.matsim.contrib.common.stats.FixedSampleSizeDiscretizer;
import org.matsim.contrib.common.stats.LinearDiscretizer;
import org.matsim.contrib.common.util.XORShiftRandom;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import playground.johannes.gsv.synPop.mid.Route2GeoDistance;
import playground.johannes.gsv.synPop.sim3.ReplaceActTypes;
import playground.johannes.studies.matrix2014.analysis.MatrixAnalyzer;
import playground.johannes.studies.matrix2014.analysis.NumericLegAnalyzer;
import playground.johannes.studies.matrix2014.analysis.ZoneMobilityRate;
import playground.johannes.studies.matrix2014.config.MatrixAnalyzerConfigurator;
import playground.johannes.studies.matrix2014.gis.TransferZoneAttribute;
import playground.johannes.studies.matrix2014.gis.ValidateFacilities;
import playground.johannes.studies.matrix2014.gis.ZoneSetLAU2Class;
import playground.johannes.synpop.analysis.*;
import playground.johannes.synpop.data.*;
import playground.johannes.synpop.data.io.PopulationIO;
import playground.johannes.synpop.gis.*;
import playground.johannes.synpop.matrix.NumericMatrix;
import playground.johannes.synpop.matrix.NumericMatrixTxtIO;
import playground.johannes.synpop.processing.*;
import playground.johannes.synpop.sim.*;
import playground.johannes.synpop.sim.data.Converters;
import playground.johannes.synpop.sim.data.DoubleConverter;
import playground.johannes.synpop.source.mid2008.MiDKeys;
import playground.johannes.synpop.source.mid2008.MiDValues;
import playground.johannes.synpop.util.Executor;

import java.io.IOException;
import java.util.*;

/**
 * @author johannes
 */
public class Simulator {

    private static final Logger logger = Logger.getLogger(Simulator.class);

    private static final String MODULE_NAME = "synPopSim";

    /**
     * @param args
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {
        final Config config = new Config();
        ConfigUtils.loadConfig(config, args[0]);

        Random random = new XORShiftRandom(Long.parseLong(config.getParam("global", "randomSeed")));
        /*
        Load and validate reference population
         */
        Set<PlainPerson> refPersons = loadRefPopulation(config, random);
        /*
        Load and validate GIS data
         */
        DataPool dataPool = new DataPool();
        loadGISData(dataPool, config, random);
        /*
        Setup analyzer and analyze reference population
		 */
        final String output = config.getParam(MODULE_NAME, "output");
        FileIOContext ioContext = new FileIOContext(output);

        AnalyzerTaskComposite<Collection<? extends Person>> task = buildAnalyzer(dataPool, ioContext);

        logger.info("Analyzing reference population...");
        ioContext.append("ref");
        AnalyzerTaskRunner.run(refPersons, task, ioContext);
		/*
		Generating simulation population...
		 */
        Set<PlainPerson> simPersons = generateSimPopulation(refPersons, dataPool, config, random);
        /*
        Append further analyzers
         */
        extendAnalyzer(task, dataPool, ioContext, config);
		/*
		Setup hamiltonian
		 */
        final HamiltonianComposite hamiltonian = new HamiltonianComposite();
        TaskRunner.run(new CopyPersonAttToLeg(CommonKeys.PERSON_WEIGHT), refPersons);
		/*
		Setup distance distribution hamiltonian.
		 */
        UnivariatFrequency distDistrTerm = buildDistDistrTerm(refPersons, simPersons);
        hamiltonian.addComponent(distDistrTerm, Double.parseDouble(config.getParam(MODULE_NAME, "theta_distDistr")));
		/*
		Setup mean distance LAU2 hamiltonian.
		 */
        BivariatMean meanDistLau2Term = buildMeanDistLau2Term(refPersons, simPersons);
        hamiltonian.addComponent(meanDistLau2Term, Double.parseDouble(config.getParam(MODULE_NAME, "theta_distLau2")));
        /*
        Setup matrix calibrator
         */
        NumericMatrix refMatrix = new NumericMatrix();
        NumericMatrixTxtIO.read(refMatrix, config.getParam(MODULE_NAME, "calibrationMatrix"));
        String layerName = config.getParam(MODULE_NAME, "calibrationLayerName");
//        ODDistribution odDistribution = new ODDistribution(simPersons, refMatrix, dataPool, layerName, "NO", 100000);
        ODCalibrator odDistribution = new ODCalibratorBuilder().build(refMatrix, dataPool, layerName, "NO", 100000, new CachedModePredicate(CommonKeys.LEG_MODE, CommonValues.LEG_MODE_CAR));
        long delay = (long) Double.parseDouble(config.getParam(MODULE_NAME, "calibrationDelay"));
        DelayedHamiltonian odDistributionDelayed = new DelayedHamiltonian(odDistribution, delay);
        hamiltonian.addComponent(odDistributionDelayed, Double.parseDouble(config.getParam(MODULE_NAME,
                "theta_matrix")));
		/*
		Setup listeners for changes on geo distance.
		 */
        AttributeChangeListenerComposite geoDistListeners = new AttributeChangeListenerComposite();
        geoDistListeners.addComponent(distDistrTerm);
        geoDistListeners.addComponent(meanDistLau2Term);
		/*
		Setup the facility mutator.
		 */
        FacilityMutatorBuilder mutatorBuilder = new FacilityMutatorBuilder(dataPool, random);
        mutatorBuilder.addToBlacklist(ActivityTypes.HOME);
        GeoDistanceUpdater geoDistanceUpdater = new GeoDistanceUpdater(geoDistListeners);
        geoDistanceUpdater.setPredicate(new CachedModePredicate(CommonKeys.LEG_MODE, CommonValues.LEG_MODE_CAR));
        AttributeChangeListenerComposite mutatorListenerComposite = new AttributeChangeListenerComposite();
        mutatorListenerComposite.addComponent(geoDistanceUpdater);
        mutatorListenerComposite.addComponent(odDistribution);
        mutatorBuilder.setListener(mutatorListenerComposite);
        Mutator<? extends Attributable> mutator = mutatorBuilder.build();
		/*
		Setup the sampler.
		 */
        MarkovEngine sampler = new MarkovEngine(simPersons, hamiltonian, mutator, random);

        MarkovEngineListenerComposite engineListeners = new MarkovEngineListenerComposite();

        long dumpInterval = (long) Double.parseDouble(config.getParam(MODULE_NAME, "dumpInterval"));
        engineListeners.addComponent(new AnalyzerListener(task, ioContext, dumpInterval));

        long logInterval = (long) Double.parseDouble(config.getParam(MODULE_NAME, "logInterval"));
        engineListeners.addComponent(new HamiltonianLogger(hamiltonian, logInterval, "SystemTemperature", output));
        engineListeners.addComponent(new HamiltonianLogger(distDistrTerm, logInterval, "DistanceDistribution",
                output));
        engineListeners.addComponent(new HamiltonianLogger(meanDistLau2Term, logInterval, "MeanDistanceLAU2",
                output));
        engineListeners.addComponent(new HamiltonianLogger(odDistributionDelayed, logInterval, "ODCalibrator", output));
        engineListeners.addComponent(new TransitionLogger(logInterval));
        engineListeners.addComponent(odDistributionDelayed);

        sampler.setListener(engineListeners);

        logger.info("Begin sampling...");
        sampler.run((long) Double.parseDouble(config.getParam(MODULE_NAME, "iterations")));
        logger.info("Done.");
        Executor.shutdown();
    }

    private static Set<PlainPerson> loadRefPopulation(Config config, Random random) {
        logger.info("Loading persons...");
        Set<PlainPerson> refPersons = PopulationIO.loadFromXML(config.findParam(MODULE_NAME, "popInputFile"), new PlainFactory());
        logger.info(String.format("Loaded %s persons.", refPersons.size()));

        logger.info("Preparing reference simulation...");
        TaskRunner.validatePersons(new ValidateMissingAttribute(CommonKeys.PERSON_WEIGHT), refPersons);
        TaskRunner.validatePersons(new ValidatePersonWeight(), refPersons);
        TaskRunner.run(new ReplaceActTypes(), refPersons);
        new GuessMissingActTypes(random).apply(refPersons);
        TaskRunner.run(new Route2GeoDistance(new Route2GeoDistFunction()), refPersons);

        return refPersons;
    }

    private static void loadGISData(DataPool dataPool, Config config, Random random) {
        logger.info("Registering data loaders...");
        dataPool.register(new FacilityDataLoader(config.getParam(MODULE_NAME, "facilities"), random), FacilityDataLoader.KEY);
        dataPool.register(new ZoneDataLoader(config.getModule(MODULE_NAME)), ZoneDataLoader.KEY);

        ValidateFacilities.validate(dataPool, "modena");
        ValidateFacilities.validate(dataPool, "lau2");
        ValidateFacilities.validate(dataPool, "nuts3");

        ZoneCollection lau2Zones = ((ZoneData) dataPool.get(ZoneDataLoader.KEY)).getLayer("lau2");
        new ZoneSetLAU2Class().apply(lau2Zones);
    }

    private static AnalyzerTaskComposite<Collection<? extends Person>> buildAnalyzer(DataPool dataPool, FileIOContext ioContext) {
        final ConcurrentAnalyzerTask<Collection<? extends Person>> task = new ConcurrentAnalyzerTask<>();

        task.addComponent(buildGeoDistanceAnalyzer(ioContext));

        ZoneCollection lau2Zones = ((ZoneData) dataPool.get(ZoneDataLoader.KEY)).getLayer("lau2");

        ZoneMobilityRate zoneMobilityRate = new ZoneMobilityRate(MiDKeys.PERSON_LAU2_CLASS, lau2Zones, new
                ModePredicate(CommonValues.LEG_MODE_CAR), ioContext);
        task.addComponent(zoneMobilityRate);
//        task.addComponent(new NumericAnalyzer(new PersonCollector<Double>(new NumericAttributeProvider<Person>(CommonKeys.PERSON_WEIGHT)), "weights", new HistogramWriter(ioContext, new StratifiedDiscretizerBuilder(50, 1))));

        task.addComponent(new GeoDistNumTripsTask(ioContext, new ModePredicate(CommonValues.LEG_MODE_CAR)));
        task.addComponent(new TripsPerPersonTask().build(ioContext));
        return task;
    }

    private static Set<PlainPerson> generateSimPopulation(Set<PlainPerson> refPersons, DataPool dataPool, Config config, Random random) {
        logger.info("Cloning sim persons...");
        int size = (int) Double.parseDouble(config.getParam(MODULE_NAME, "populationSize"));
        Set<PlainPerson> simPersons = (Set<PlainPerson>) PersonUtils.weightedCopy(refPersons, new PlainFactory(), size,
                random);
        logger.info(String.format("Generated %s persons.", simPersons.size()));
        /*
        Initializing simulation population...
         */
        logger.info("Assigning home locations...");
        ZoneCollection lau2Zones = ((ZoneData) dataPool.get(ZoneDataLoader.KEY)).getLayer("lau2");
        ZoneCollection modenaZones = ((ZoneData) dataPool.get(ZoneDataLoader.KEY)).getLayer("modena");

        ZoneMobilityRate zoneMobilityRate = new ZoneMobilityRate(MiDKeys.PERSON_LAU2_CLASS, lau2Zones, new ModePredicate(CommonValues.LEG_MODE_CAR));
        zoneMobilityRate.analyze(refPersons, null);

        new TransferZoneAttribute().apply(lau2Zones, modenaZones, MiDKeys.PERSON_LAU2_CLASS);

        SetHomeFacilities setHomeFacilities = new SetHomeFacilities(dataPool, "modena", random);
        setHomeFacilities.setZoneWeights(zoneMobilityRate.getMobilityRatePerZone(modenaZones));
        setHomeFacilities.apply(simPersons);

        logger.info("Assigning random activity locations...");
        TaskRunner.run(new SetActivityFacilities((FacilityData) dataPool.get(FacilityDataLoader.KEY)), simPersons);

        logger.info("Recalculate geo distances...");
        TaskRunner.run(new LegAttributeRemover(CommonKeys.LEG_GEO_DISTANCE), simPersons);
        TaskRunner.run(new CalculateGeoDistance((FacilityData) dataPool.get(FacilityDataLoader.KEY)), simPersons);

        logger.info("Resetting LAU2Class attributes...");
        SetLAU2Attribute lTask = new SetLAU2Attribute(dataPool, "lau2");
        TaskRunner.run(lTask, simPersons);
        if (lTask.getErrors() > 0)
            logger.warn(String.format("Cannot set LAU2Class attribute for %s persons.", lTask.getErrors()));

        return simPersons;
    }

    private static void extendAnalyzer(AnalyzerTaskComposite<Collection<? extends Person>> task, DataPool dataPool, FileIOContext ioContext, Config config) {
        MatrixAnalyzer mAnalyzer = (MatrixAnalyzer) new MatrixAnalyzerConfigurator(config.getModule("matrixAnalyzer")
                , dataPool, ioContext).load();
        mAnalyzer.setPredicate(new ModePredicate(CommonValues.LEG_MODE_CAR));
        task.addComponent(mAnalyzer);

        task.addComponent(new PopulationWriter(ioContext));
    }

    private static UnivariatFrequency buildDistDistrTerm(Set<PlainPerson> refPersons, Set<PlainPerson>
            simPersons) {
        Set<Attributable> refLegs = getCarLegs(refPersons);
        Set<Attributable> simLegs = getCarLegs(simPersons);

        List<Double> values = new LegCollector(new NumericAttributeProvider(CommonKeys.LEG_GEO_DISTANCE)).collect(refPersons);
        double[] nativeValues = org.matsim.contrib.common.collections.CollectionUtils.toNativeArray(values);
        Discretizer disc = FixedSampleSizeDiscretizer.create(nativeValues, 50, 100);

        UnivariatFrequency f = new UnivariatFrequency(refLegs, simLegs, CommonKeys.LEG_GEO_DISTANCE, disc, true);

        return f;
    }

    private static Set<Attributable> getCarLegs(Set<? extends Person> persons) {
        Predicate<Segment> carPredicate = new ModePredicate(CommonValues.LEG_MODE_CAR);
        Set<Attributable> legs = new HashSet<>();
        for (Person p : persons) {
            Episode e = p.getEpisodes().get(0);
            for (Segment leg : e.getLegs()) {
                if (carPredicate.test(leg)) legs.add(leg);
            }
        }

        return legs;
    }

    private static BivariatMean buildMeanDistLau2Term(Set<PlainPerson> refPersons, Set<PlainPerson> simPersons) {
        TaskRunner.run(new CopyPersonAttToLeg(MiDKeys.PERSON_LAU2_CLASS), refPersons);
        TaskRunner.run(new CopyPersonAttToLeg(MiDKeys.PERSON_LAU2_CLASS), simPersons);

//        copyLau2ClassAttribute(refPersons);
//        copyLau2ClassAttribute(simPersons);

        Set<Attributable> refLegs = getCarLegs(refPersons);
        Set<Attributable> simLegs = getCarLegs(simPersons);

        Converters.register(MiDKeys.PERSON_LAU2_CLASS, DoubleConverter.getInstance());
        BivariatMean bm = new BivariatMean(refLegs, simLegs, MiDKeys.PERSON_LAU2_CLASS, CommonKeys.LEG_GEO_DISTANCE,
                new LinearDiscretizer(1.0), true);

        return bm;
    }

    private static void copyLau2ClassAttribute(Set<PlainPerson> persons) {
        for (Person p : persons) {
            String lau2Class = p.getAttribute(MiDKeys.PERSON_LAU2_CLASS);
            for (Episode e : p.getEpisodes()) {
                for (Segment leg : e.getLegs()) {
                    leg.setAttribute(MiDKeys.PERSON_LAU2_CLASS, lau2Class);
                }
            }
        }
    }


    public static class Route2GeoDistFunction implements UnivariateRealFunction {

        @Override
        public double value(double x) throws FunctionEvaluationException {
            double routDist = x / 1000.0;
            double factor = 0.77 - Math.exp(-0.17 * Math.max(20, routDist) - 1.48);
            return routDist * factor * 1000;
        }
    }

    private static AnalyzerTask<Collection<? extends Person>> buildGeoDistanceAnalyzer(FileIOContext ioContext) {
        AnalyzerTaskComposite<Collection<? extends Person>> tasks = new AnalyzerTaskComposite<>();

        HistogramWriter histogramWriter = new HistogramWriter(ioContext, new StratifiedDiscretizerBuilder(100, 100));
        histogramWriter.addBuilder( new PassThroughDiscretizerBuilder(new LinearDiscretizer(50000), "linear"));

        Predicate<Segment> modePredicate = new ModePredicate(CommonValues.LEG_MODE_CAR);

        Map<String, Predicate<Segment>> predicates = new HashMap<>();
        predicates.put(CommonValues.LEG_MODE_CAR, modePredicate);

        for (int klass = 0; klass < 6; klass++) {
            Predicate<Segment> lauPred = new LegPersonAttributePredicate(MiDKeys.PERSON_LAU2_CLASS, String.valueOf(klass));
            Predicate<Segment> predicate = PredicateAndComposite.create(modePredicate, lauPred);
            String label = String.format("car.lau%s", klass);
            tasks.addComponent(NumericLegAnalyzer.create(CommonKeys.LEG_GEO_DISTANCE, true, predicate, label, histogramWriter));
        }

        Predicate<Segment> inTown = new LegAttributePredicate(MiDKeys.LEG_DESTINATION, MiDValues.IN_TOWN);
        Predicate<Segment> predicate = PredicateAndComposite.create(modePredicate, inTown);
        tasks.addComponent(NumericLegAnalyzer.create(CommonKeys.LEG_GEO_DISTANCE, true, predicate, "car.inTown", histogramWriter));

        Predicate<Segment> outOfTown = new LegAttributePredicate(MiDKeys.LEG_DESTINATION, MiDValues.OUT_OF_TOWN);
        predicate = PredicateAndComposite.create(modePredicate, outOfTown);
        tasks.addComponent(NumericLegAnalyzer.create(CommonKeys.LEG_GEO_DISTANCE, true, predicate, "car.outOfTown", histogramWriter));

        return tasks;
    }
}
