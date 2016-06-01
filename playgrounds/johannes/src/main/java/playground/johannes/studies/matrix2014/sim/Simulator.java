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

import gnu.trove.list.array.TDoubleArrayList;
import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.analysis.UnivariateRealFunction;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.matsim.contrib.common.stats.Discretizer;
import org.matsim.contrib.common.stats.FixedBordersDiscretizer;
import org.matsim.contrib.common.stats.LinearDiscretizer;
import org.matsim.contrib.common.util.XORShiftRandom;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import playground.johannes.gsv.synPop.mid.Route2GeoDistance;
import playground.johannes.gsv.synPop.sim3.ReplaceActTypes;
import playground.johannes.studies.matrix2014.analysis.AnalyzerTaskGroup;
import playground.johannes.studies.matrix2014.analysis.NumericLegAnalyzer;
import playground.johannes.studies.matrix2014.analysis.ZoneMobilityRate;
import playground.johannes.studies.matrix2014.config.ODCalibratorConfigurator;
import playground.johannes.studies.matrix2014.gis.TransferZoneAttribute;
import playground.johannes.studies.matrix2014.gis.ValidateFacilities;
import playground.johannes.studies.matrix2014.gis.ZoneSetLAU2Class;
import playground.johannes.synpop.analysis.*;
import playground.johannes.synpop.data.*;
import playground.johannes.synpop.data.io.PopulationIO;
import playground.johannes.synpop.gis.*;
import playground.johannes.synpop.processing.*;
import playground.johannes.synpop.sim.*;
import playground.johannes.synpop.sim.data.Converters;
import playground.johannes.synpop.sim.data.DoubleConverter;
import playground.johannes.synpop.source.mid2008.MiDKeys;
import playground.johannes.synpop.source.mid2008.MiDValues;
import playground.johannes.synpop.util.Executor;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

/**
 * @author johannes
 */
public class Simulator {

    private static final Logger logger = Logger.getLogger(Simulator.class);

    private static final String MODULE_NAME = "synPopSim";

    private static Discretizer simDistanceDiscretizer;

    public static final boolean USE_WEIGTHS = true;

    public static final Predicate<Segment> DEFAULT_LEG_PREDICATE = new LegAttributePredicate(CommonKeys.LEG_MODE,
            CommonValues.LEG_MODE_CAR);

    public static final String DEFAULT_LEG_PREDICATE_NAME = CommonValues.LEG_MODE_CAR;
    /**
     * @param args
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {
        Logger.getRootLogger().setLevel(Level.TRACE);

        final Config config = new Config();
        ConfigUtils.loadConfig(config, args[0]);

        Random random = new XORShiftRandom(Long.parseLong(config.getParam("global", "randomSeed")));
        /*
        Load and validate reference population
         */
        Set<Person> refPersons = loadRefPopulation(config, random);
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

        simDistanceDiscretizer = buildDistanceDiscretizer();
        AnalyzerTaskComposite<Collection<? extends Person>> task = buildAnalyzer(dataPool, ioContext, refPersons);

        logger.info("Analyzing reference population...");
        ioContext.append("ref");
        AnalyzerTaskRunner.run(refPersons, task, ioContext);
        /*
		Generating simulation population...
		 */
        Set<Person> simPersons = generateSimPopulation(refPersons, dataPool, config, random);
        /*
        Append further analyzers
         */
        extendAnalyzer(task, dataPool, ioContext, config);
		/*
		Setup hamiltonian
		 */
        MarkovEngineListenerComposite engineListeners = new MarkovEngineListenerComposite();
        final HamiltonianComposite hamiltonian = new HamiltonianComposite();
//        final MutableHamiltonianComposite hamiltonian = new MutableHamiltonianComposite();
        TaskRunner.run(new CopyPersonAttToLeg(CommonKeys.PERSON_WEIGHT), refPersons);
        TaskRunner.run(new CopyPersonAttToLeg(CommonKeys.PERSON_WEIGHT), simPersons);
		/*
		Setup distance distribution hamiltonian.
		 */
        UnivariatFrequency distDistrTerm = buildDistDistrTerm(refPersons, simPersons);
        hamiltonian.addComponent(distDistrTerm, Double.parseDouble(config.getParam(MODULE_NAME, "theta_distDistr")));
//        double theta = Double.parseDouble(config.getParam(MODULE_NAME, "theta_distDistr"));
//        ThetaProvider provider = new ThetaProvider(distDistrTerm, 0.0000001, 1000000, theta, theta * 1000000000);
//        hamiltonian.addComponent(distDistrTerm, provider);
//        engineListeners.addComponent(provider);

        UnivariatFrequency distDistrTerm2 = buildDistDistrTerm2(refPersons, simPersons);
        hamiltonian.addComponent(distDistrTerm2, Double.parseDouble(config.getParam(MODULE_NAME, "theta_distDistr")));
		/*
		Setup mean distance LAU2 hamiltonian.
		 */
        BivariatMean meanDistLau2Term = buildMeanDistLau2Term(refPersons, simPersons);
        hamiltonian.addComponent(meanDistLau2Term, Double.parseDouble(config.getParam(MODULE_NAME,
                "theta_distLau2")));
        /*
        Setup matrix calibrator
         */
        ODCalibrator odDistribution = new ODCalibratorConfigurator(dataPool).configure(config.getModule("tomtomCalibrator"));
        odDistribution.setUseWeights(true);
        odDistribution.setPredicate(new CachedModePredicate(CommonKeys.LEG_MODE, CommonValues.LEG_MODE_CAR));
        DelayedHamiltonian odDistributionDelayed = new DelayedHamiltonian(odDistribution, (long) Double.parseDouble(config.getParam
                (MODULE_NAME, "delay_matrix")));
        hamiltonian.addComponent(odDistributionDelayed, Double.parseDouble(config.getParam(MODULE_NAME,
                "theta_matrix")));
		/*
		Setup listeners for changes on geo distance.
		 */
        AttributeChangeListenerComposite geoDistListeners = new AttributeChangeListenerComposite();
        geoDistListeners.addComponent(distDistrTerm);
        geoDistListeners.addComponent(distDistrTerm2);
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



        long dumpInterval = (long) Double.parseDouble(config.getParam(MODULE_NAME, "dumpInterval"));
        engineListeners.addComponent(new AnalyzerListener(task, ioContext, dumpInterval));

        long logInterval = (long) Double.parseDouble(config.getParam(MODULE_NAME, "logInterval"));
        engineListeners.addComponent(new HamiltonianLogger(hamiltonian, logInterval, "SystemTemperature", output));
        engineListeners.addComponent(new HamiltonianLogger(distDistrTerm, logInterval, "DistanceDistribution",
                output));
        engineListeners.addComponent(new HamiltonianLogger(distDistrTerm2, logInterval, "DistanceDistribution2",
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

    private static Set<Person> loadRefPopulation(Config config, Random random) {
        logger.info("Loading persons...");
        Set<Person> refPersons = PopulationIO.loadFromXML(config.findParam(MODULE_NAME, "popInputFile"), new PlainFactory());
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
        ValidateFacilities.validate(dataPool, "tomtom");

        ZoneCollection lau2Zones = ((ZoneData) dataPool.get(ZoneDataLoader.KEY)).getLayer("lau2");
        new ZoneSetLAU2Class().apply(lau2Zones);
    }

    private static AnalyzerTaskComposite<Collection<? extends Person>> buildAnalyzer(DataPool dataPool, FileIOContext ioContext, Collection<Person> persons) {
//        final ConcurrentAnalyzerTask<Collection<? extends Person>> task = new ConcurrentAnalyzerTask<>();
        final AnalyzerTaskComposite<Collection<? extends Person>> task = new AnalyzerTaskComposite<>();
        buildGeoDistanceAnalyzer(task, ioContext, dataPool);


        ZoneCollection lau2Zones = ((ZoneData) dataPool.get(ZoneDataLoader.KEY)).getLayer("lau2");

        ZoneMobilityRate zoneMobilityRate = new ZoneMobilityRate(MiDKeys.PERSON_LAU2_CLASS, lau2Zones, DEFAULT_LEG_PREDICATE, ioContext);
        task.addComponent(zoneMobilityRate);
        task.addComponent(new NumericAnalyzer(new PersonCollector<>(
                new NumericAttributeProvider<Person>(CommonKeys.PERSON_WEIGHT)),
                "weights",
                new HistogramWriter(ioContext, new PassThroughDiscretizerBuilder(new LinearDiscretizer(1), "linear"))));

        task.addComponent(new GeoDistNumTripsTask(ioContext, DEFAULT_LEG_PREDICATE));
        task.addComponent(new TripsPerPersonTask().build(ioContext));
        return task;
    }

    private static Set<Person> generateSimPopulation(Set<Person> refPersons, DataPool dataPool, Config config, Random random) {
        String simPopFile = config.findParam(MODULE_NAME, "simPopulation");
        Set<Person> simPersons;
        if(simPopFile == null) {
            logger.info("Cloning sim persons...");
            int size = (int) Double.parseDouble(config.getParam(MODULE_NAME, "populationSize"));
            simPersons = (Set<Person>) clonePersons(refPersons, size, random);
            logger.info(String.format("Generated %s persons.", simPersons.size()));
        /*
        Initializing simulation population...
         */
            logger.info("Assigning home locations...");
            ZoneCollection lau2Zones = ((ZoneData) dataPool.get(ZoneDataLoader.KEY)).getLayer("lau2");
            ZoneCollection modenaZones = ((ZoneData) dataPool.get(ZoneDataLoader.KEY)).getLayer("modena");

            ZoneMobilityRate zoneMobilityRate = new ZoneMobilityRate(MiDKeys.PERSON_LAU2_CLASS, lau2Zones, DEFAULT_LEG_PREDICATE);
            zoneMobilityRate.analyze(refPersons, null);

            new TransferZoneAttribute().apply(lau2Zones, modenaZones, MiDKeys.PERSON_LAU2_CLASS);

            SetHomeFacilities setHomeFacilities = new SetHomeFacilities(dataPool, "modena", random);
            setHomeFacilities.setZoneWeights(zoneMobilityRate.getMobilityRatePerZone(modenaZones));
            setHomeFacilities.apply(simPersons);

            logger.info("Assigning random activity locations...");
            TaskRunner.run(new SetActivityFacilities((FacilityData) dataPool.get(FacilityDataLoader.KEY)), simPersons);
        } else {
            logger.info("Loading sim population from file...");
            simPersons = PopulationIO.loadFromXML(simPopFile, new PlainFactory());
        }

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
        ConcurrentAnalyzerTask<Collection<? extends Person>> matrixTasks = new ConcurrentAnalyzerTask<>();

//        ModePredicate modePredicate = new ModePredicate(CommonValues.LEG_MODE_CAR);
//        MatrixComparator mAnalyzer = (MatrixComparator) new MatrixAnalyzerConfigurator(config.getModule("matrixAnalyzerITP")
//                , dataPool, ioContext).load();
//        mAnalyzer.setLegPredicate(DEFAULT_LEG_PREDICATE);
//        mAnalyzer.setUseWeights(USE_WEIGTHS);
//        matrixTasks.addComponent(mAnalyzer);
//
////        mAnalyzer = (MatrixAnalyzer) new MatrixAnalyzerConfigurator(config.getModule("matrixAnalyzerITP-2")
////                , dataPool, ioContext).load();
////        mAnalyzer.setLegPredicate(modePredicate);
////        mAnalyzer.setUseWeights(true);
////        task.addComponent(mAnalyzer);
//
//        mAnalyzer = (MatrixComparator) new MatrixAnalyzerConfigurator(config.getModule("matrixAnalyzerTomTom")
//                , dataPool, ioContext).load();
//        mAnalyzer.setLegPredicate(DEFAULT_LEG_PREDICATE);
//        ZoneData zoneData = (ZoneData) dataPool.get(ZoneDataLoader.KEY);
//        ZoneCollection zones = zoneData.getLayer("tomtom");
//        ODPredicate distPredicate = new ZoneDistancePredicate(zones, 100000);
//        mAnalyzer.setNormPredicate(distPredicate);
//        mAnalyzer.setUseWeights(USE_WEIGTHS);
//        matrixTasks.addComponent(mAnalyzer);

//        mAnalyzer = (MatrixAnalyzer) new MatrixAnalyzerConfigurator(config.getModule("matrixAnalyzerTomTom-2")
//                , dataPool, ioContext).load();
//        mAnalyzer.setLegPredicate(modePredicate);
//        mAnalyzer.setODPredicate(distPredicate);
//        mAnalyzer.setUseWeights(true);
//        task.addComponent(mAnalyzer);

//        ActivityFacilities facilities = ((FacilityData) dataPool.get(FacilityDataLoader.KEY)).getAll();
//        MatrixWriter matrixWriter = new MatrixWriter(facilities, zones, ioContext);
//        matrixWriter.setLegPredicate(DEFAULT_LEG_PREDICATE);
//        matrixWriter.setUseWeights(USE_WEIGTHS);
//        matrixTasks.addComponent(matrixWriter);
//
//        AnalyzerTaskGroup<Collection<? extends Person>> group = new AnalyzerTaskGroup<>(matrixTasks, ioContext,
//                "matrix");
//        task.addComponent(group);
//
//        task.addComponent(new PopulationWriter(ioContext));
//
//        HistogramWriter histogramWriter = new HistogramWriter(ioContext, new StratifiedDiscretizerBuilder(100, 100));
//        histogramWriter.addBuilder(new PassThroughDiscretizerBuilder(new LinearDiscretizer(50000), "linear"));
//        histogramWriter.addBuilder(new PassThroughDiscretizerBuilder(new FixedBordersDiscretizer(new double[]{-1,
//                100000, Integer.MAX_VALUE}), "100KM"));
//        histogramWriter.addBuilder(new PassThroughDiscretizerBuilder(simDistanceDiscretizer, "sim"));
//
//        FacilityData fData = (FacilityData) dataPool.get(FacilityDataLoader.KEY);
//        NumericAnalyzer actDist = new ActDistanceBuilder()
//                .setHistogramWriter(histogramWriter)
//                .setLegPredicate(DEFAULT_LEG_PREDICATE, DEFAULT_LEG_PREDICATE_NAME)
//                .setUseWeights(USE_WEIGTHS)
//                .build(fData.getAll());
//        task.addComponent(actDist);
    }

    private static UnivariatFrequency buildDistDistrTerm(Set<Person> refPersons, Set<Person>
            simPersons) {
        Set<Attributable> refLegs = getCarLegs(refPersons);
        Set<Attributable> simLegs = getCarLegs(simPersons);

//        List<Double> values = new LegCollector(new NumericAttributeProvider(CommonKeys.LEG_GEO_DISTANCE)).collect(refPersons);
//        double[] nativeValues = org.matsim.contrib.common.collections.CollectionUtils.toNativeArray(values);
//        Discretizer disc = FixedSampleSizeDiscretizer.create(nativeValues, 50, 100);

        UnivariatFrequency f = new UnivariatFrequency(refLegs, simLegs, CommonKeys.LEG_GEO_DISTANCE,
                simDistanceDiscretizer, USE_WEIGTHS);

        return f;
    }

    private static UnivariatFrequency buildDistDistrTerm2(Set<Person> refPersons, Set<Person>
            simPersons) {
        Set<Attributable> refLegs = getCarLegs(refPersons);
        Set<Attributable> simLegs = getCarLegs(simPersons);

//        List<Double> values = new LegCollector(new NumericAttributeProvider(CommonKeys.LEG_GEO_DISTANCE)).collect(refPersons);
//        double[] nativeValues = org.matsim.contrib.common.collections.CollectionUtils.toNativeArray(values);
        Discretizer disc = new FixedBordersDiscretizer(new double[]{-1, 100000, Integer.MAX_VALUE});

        UnivariatFrequency f = new UnivariatFrequency(refLegs, simLegs, CommonKeys.LEG_GEO_DISTANCE, disc, USE_WEIGTHS);

        return f;
    }

    private static Set<Attributable> getCarLegs(Set<? extends Person> persons) {
//        Predicate<Segment> carPredicate = new ModePredicate(CommonValues.LEG_MODE_CAR);
        Set<Attributable> legs = new HashSet<>();
        for (Person p : persons) {
            Episode e = p.getEpisodes().get(0);
            for (Segment leg : e.getLegs()) {
                if (DEFAULT_LEG_PREDICATE.test(leg)) legs.add(leg);
            }
        }

        return legs;
    }

    private static BivariatMean buildMeanDistLau2Term(Set<Person> refPersons, Set<Person> simPersons) {
        TaskRunner.run(new CopyPersonAttToLeg(MiDKeys.PERSON_LAU2_CLASS), refPersons);
        TaskRunner.run(new CopyPersonAttToLeg(MiDKeys.PERSON_LAU2_CLASS), simPersons);

        Set<Attributable> refLegs = getCarLegs(refPersons);
        Set<Attributable> simLegs = getCarLegs(simPersons);

        Converters.register(MiDKeys.PERSON_LAU2_CLASS, DoubleConverter.getInstance());
        BivariatMean bm = new BivariatMean(refLegs, simLegs, MiDKeys.PERSON_LAU2_CLASS, CommonKeys.LEG_GEO_DISTANCE,
                new LinearDiscretizer(1.0), USE_WEIGTHS);

        return bm;
    }

    public static class Route2GeoDistFunction implements UnivariateRealFunction {

        @Override
        public double value(double x) throws FunctionEvaluationException {
            double routDist = x / 1000.0;
//            double factor = 0.77 - Math.exp(-0.017 * Math.max(10, routDist) - 1.48);
//            double factor = 0.7 - Math.exp(-0.017 * Math.max(10, routDist) - 1.48);
//            double factor = 0.6 - Math.exp(-0.008 * Math.max(20, routDist) - 2);
            double factor = 0.55;
            return routDist * factor * 1000;
        }
    }

    private static AnalyzerTask<Collection<? extends Person>> buildGeoDistanceAnalyzer
            (AnalyzerTaskComposite<Collection<? extends Person>> tasks, FileIOContext ioContext, DataPool dataPool) {
//        AnalyzerTaskComposite<Collection<? extends Person>> composite = new AnalyzerTaskComposite<>();
        ConcurrentAnalyzerTask<Collection<? extends Person>> composite = new ConcurrentAnalyzerTask<>();

        HistogramWriter histogramWriter = new HistogramWriter(ioContext, new StratifiedDiscretizerBuilder(100, 100));
        histogramWriter.addBuilder(new PassThroughDiscretizerBuilder(new LinearDiscretizer(50000), "linear"));
        histogramWriter.addBuilder(new PassThroughDiscretizerBuilder(new FixedBordersDiscretizer(new double[]{-1,
                100000, Integer.MAX_VALUE}), "100KM"));
        histogramWriter.addBuilder(new PassThroughDiscretizerBuilder(simDistanceDiscretizer, "sim"));

//        Predicate<Segment> modePredicate = new ModePredicate(CommonValues.LEG_MODE_CAR);
        composite.addComponent(NumericLegAnalyzer.create(CommonKeys.LEG_ROUTE_DISTANCE, USE_WEIGTHS, DEFAULT_LEG_PREDICATE,
                DEFAULT_LEG_PREDICATE_NAME,
                histogramWriter));
        composite.addComponent(NumericLegAnalyzer.create(CommonKeys.LEG_GEO_DISTANCE, USE_WEIGTHS, DEFAULT_LEG_PREDICATE,
                DEFAULT_LEG_PREDICATE_NAME,
                histogramWriter));

        for (int klass = 0; klass < 6; klass++) {
            Predicate<Segment> lauPred = new LegPersonAttributePredicate(MiDKeys.PERSON_LAU2_CLASS, String.valueOf(klass));
            Predicate<Segment> predicate = PredicateAndComposite.create(DEFAULT_LEG_PREDICATE, lauPred);
            String label = String.format("car.lau%s", klass);
            composite.addComponent(NumericLegAnalyzer.create(CommonKeys.LEG_GEO_DISTANCE, USE_WEIGTHS, predicate, label,
                    histogramWriter));
        }

        Predicate<Segment> inTown = new LegAttributePredicate(MiDKeys.LEG_DESTINATION, MiDValues.IN_TOWN);
        Predicate<Segment> predicate = PredicateAndComposite.create(DEFAULT_LEG_PREDICATE, inTown);
        composite.addComponent(NumericLegAnalyzer.create(CommonKeys.LEG_GEO_DISTANCE, USE_WEIGTHS, predicate,
                DEFAULT_LEG_PREDICATE_NAME + ".inTown",
                histogramWriter));

        Predicate<Segment> outOfTown = new LegAttributePredicate(MiDKeys.LEG_DESTINATION, MiDValues.OUT_OF_TOWN);
        predicate = PredicateAndComposite.create(DEFAULT_LEG_PREDICATE, outOfTown);
        composite.addComponent(NumericLegAnalyzer.create(CommonKeys.LEG_GEO_DISTANCE, USE_WEIGTHS, predicate,
                DEFAULT_LEG_PREDICATE_NAME + ".outOfTown", histogramWriter));

//        LegCollector<String> purposeCollector = new LegCollector<>(new AttributeProvider<Segment>(CommonKeys.LEG_PURPOSE));
//        purposeCollector.setLegPredicate(modePredicate);
//        Set<String> purposes = new HashSet<>(purposeCollector.collect(persons));
//        purposes.remove(null);
//        for (String purpose : purposes) {
//            Predicate<Segment> purposePredicate = new LegAttributePredicate(CommonKeys.LEG_PURPOSE, purpose);
//            predicate = PredicateAndComposite.create(modePredicate, purposePredicate);
//            tasks.addComponent(NumericLegAnalyzer.create(CommonKeys.LEG_GEO_DISTANCE, true, predicate, "car." + purpose, histogramWriter));
//        }

        AnalyzerTaskGroup<Collection<? extends Person>> group = new AnalyzerTaskGroup<>(composite, ioContext,
                "geoDistance");

        tasks.addComponent(group);

        return tasks;
    }

    private static Collection<Person> clonePersons(Collection<? extends Person> refPersons, int size, Random random) {
        double wsum1 = 0;
        double wsum2 = 0;

        Set<Person> persons1 = new HashSet<>();
        Set<Person> persons2 = new HashSet<>();

        TripsCounter counter = new TripsCounter(DEFAULT_LEG_PREDICATE);
        for (Person p : refPersons) {
            double w = Double.parseDouble(p.getAttribute(CommonKeys.PERSON_WEIGHT));
            if (counter.get(p.getEpisodes().get(0)) == 1) {
                wsum2 += w;
                persons2.add(p);
            } else {
                wsum1 += w;
                persons1.add(p);
            }
        }

        int n1 = size / 2;
        int n2 = size - n1;
        Set<? extends Person> simPersons1 = PersonUtils.weightedCopy(persons1, new PlainFactory(), n1, random);
        Set<? extends Person> simPersons2 = PersonUtils.weightedCopy(persons2, new PlainFactory(), n2, random);
//        Set<? extends Person> simPersons2 = PersonUtils.weightedCopy(persons2, new PlainFactory(), size, random);

        double w1 = (wsum1 * n2) / (wsum2 * n1);
        double w2 = 1.0;

        for (Person p : simPersons1) p.setAttribute(CommonKeys.PERSON_WEIGHT, String.valueOf(w1));
        for (Person p : simPersons2) p.setAttribute(CommonKeys.PERSON_WEIGHT, String.valueOf(w2));

        Set<Person> all = new HashSet<>();
        all.addAll(simPersons1);
        all.addAll(simPersons2);

        return all;
    }

    private static Discretizer buildDistanceDiscretizer() {
        TDoubleArrayList borders = new TDoubleArrayList();
        borders.add(-1);
        for(int d = 2000; d < 10000; d += 2000) borders.add(d);
        for(int d = 10000; d < 50000; d += 10000) borders.add(d);
        for(int d = 50000; d < 500000; d += 50000) borders.add(d);
        for(int d = 500000; d < 1000000; d += 100000) borders.add(d);
        borders.add(Double.MAX_VALUE);
        return new FixedBordersDiscretizer(borders.toArray());
    }
}
