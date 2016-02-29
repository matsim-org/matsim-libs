/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,       *
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
package playground.johannes.studies.matrix2014.sim.run;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.matsim.contrib.common.util.XORShiftRandom;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ConfigUtils;
import playground.johannes.studies.matrix2014.analysis.AnalyzerTaskGroup;
import playground.johannes.studies.matrix2014.sim.CachedModePredicate;
import playground.johannes.studies.matrix2014.sim.CopyPersonAttToLeg;
import playground.johannes.studies.matrix2014.sim.FacilityMutatorBuilder;
import playground.johannes.synpop.analysis.*;
import playground.johannes.synpop.data.*;
import playground.johannes.synpop.gis.DataPool;
import playground.johannes.synpop.processing.TaskRunner;
import playground.johannes.synpop.sim.*;
import playground.johannes.synpop.util.Executor;

import java.util.*;

/**
 * @author jillenberger
 */
public class Simulator {

    private static final Logger logger = Logger.getLogger(Simulator.class);

    static final String MODULE_NAME = "synPopSim";

    private static final boolean USE_WEIGHTS = true;

    private static final Predicate<Segment> DEFAULT_LEG_PREDICATE = new LegAttributePredicate(
            CommonKeys.LEG_MODE, CommonValues.LEG_MODE_CAR);

    private static final String DEFAULT_PREDICATE_NAME = "car";

    private AnalyzerTaskComposite<Collection<? extends Person>> analyzerTasks;

    private AnalyzerTaskComposite<Collection<? extends Person>> hamiltonianAnalyzers;

    private HamiltonianComposite hamiltonian;

    private MarkovEngineListenerComposite engineListeners;

    private Set<? extends Person> refPersons;

    private Set<? extends Person> simPersons;

    private Map<String, AttributeChangeListenerComposite> attributeListeners;

    private long loggingInterval;

    private FileIOContext ioContext;

    private DataPool dataPool;

    private Random random;

    AnalyzerTaskComposite<Collection<? extends Person>> getAnalyzerTasks() {
        return analyzerTasks;
    }

    AnalyzerTaskComposite<Collection<? extends Person>> getHamiltonianAnalyzers() {
        return hamiltonianAnalyzers;
    }

    HamiltonianComposite getHamiltonian() {
        return hamiltonian;
    }

    MarkovEngineListenerComposite getEngineListeners() {
        return engineListeners;
    }

    boolean getUseWeights() {
        return USE_WEIGHTS;
    }

    Predicate<Segment> getLegPredicate() {
        return DEFAULT_LEG_PREDICATE;
    }

    String getLegPredicateName() {
        return DEFAULT_PREDICATE_NAME;
    }

    Set<? extends Person> getRefPersons() {
        return refPersons;
    }

    Set<? extends Person> getSimPersons() {
        return simPersons;
    }

    Map<String, AttributeChangeListenerComposite> getAttributeListeners() {
        return attributeListeners;
    }

    long getLoggingInterval() {
        return loggingInterval;
    }

    FileIOContext getIOContext() {
        return ioContext;
    }

    DataPool getDataPool() {
        return dataPool;
    }

    Random getRandom() {
        return random;
    }

    public MarkovEngine build(Config config) {
        ConfigGroup configGroup = config.getModule(MODULE_NAME);
        /*
        Initialize composites...
         */
        hamiltonian = new HamiltonianComposite();
        analyzerTasks = new AnalyzerTaskComposite<>();
        engineListeners = new MarkovEngineListenerComposite();
        attributeListeners = new HashMap<>();
        /*
        Load parameters...
         */
        random = new XORShiftRandom(Long.parseLong(config.getParam("global", "randomSeed")));
        loggingInterval = (long) Double.parseDouble(configGroup.getValue("logInterval"));
        long dumpInterval = (long) Double.parseDouble(config.getParam(MODULE_NAME, "dumpInterval"));
        ioContext = new FileIOContext(configGroup.getValue("output"));
        /*
        Load GIS data...
         */
        dataPool = new DataPool();
        DataPoolLoader.load(this, config);
        /*
        Load reference population...
         */
        refPersons = RefPopulationBuilder.build(this, config);
        /*
        Generate the simulation population...
         */
        simPersons = SimPopulationBuilder.build(this, config);
        /*
		Setup listeners for changes on facilities and geo distance.
		 */
        attributeListeners.put(CommonKeys.LEG_GEO_DISTANCE, new AttributeChangeListenerComposite());
        attributeListeners.put(CommonKeys.ACTIVITY_FACILITY, new AttributeChangeListenerComposite());

        GeoDistanceUpdater geoDistanceUpdater = new GeoDistanceUpdater(attributeListeners.get(CommonKeys.LEG_GEO_DISTANCE));
        geoDistanceUpdater.setPredicate(new CachedModePredicate(CommonKeys.LEG_MODE, CommonValues.LEG_MODE_CAR));

        attributeListeners.get(CommonKeys.ACTIVITY_FACILITY).addComponent(geoDistanceUpdater);
        /*
        Build default analyzer...
         */
        DefaultAnalyzerBuilder.build(this, config);
		/*
        Build hamiltonians...
         */
        if(getUseWeights()) {
            TaskRunner.run(new CopyPersonAttToLeg(CommonKeys.PERSON_WEIGHT), refPersons);
            TaskRunner.run(new CopyPersonAttToLeg(CommonKeys.PERSON_WEIGHT), simPersons);
        }

        hamiltonianAnalyzers = new ConcurrentAnalyzerTask<>();
        analyzerTasks.addComponent(new AnalyzerTaskGroup<>(hamiltonianAnalyzers, ioContext, "hamiltonian"));

        GeoDistanceZoneDensityHamiltonian.build(this, config);
//        GeoDistanceZoneHamiltonian.build(this, config);
//        GeoDistanceHamiltonian.build(this, config);
//        GeoDistanceLAU2Hamiltonian.build(this, config);
//        MeanDistanceHamiltonian.build(this, config);
//        MeanZoneDistanceHamiltonian.build(this, config);
        ODCalibratorHamiltonian.build(this, config);

        engineListeners.addComponent(new HamiltonianLogger(hamiltonian,
                loggingInterval,
                "SystemTemperature",
                ioContext.getRoot()));
        engineListeners.addComponent(new TransitionLogger(loggingInterval));
        /*
        Analyze reference population...
         */
        logger.info("Analyzing reference population...");
        ioContext.append("ref");
        AnalyzerTaskRunner.run(refPersons, analyzerTasks, ioContext);
        /*
        Extend the analyzer
         */
        ExtendedAnalyzerBuilder.build(this, config);

        engineListeners.addComponent(new AnalyzerListener(analyzerTasks, ioContext, dumpInterval));
        /*
		Setup the facility mutator...
		 */
        FacilityMutatorBuilder mutatorBuilder = new FacilityMutatorBuilder(dataPool, random);
        mutatorBuilder.addToBlacklist(ActivityTypes.HOME);
        mutatorBuilder.setListener(attributeListeners.get(CommonKeys.ACTIVITY_FACILITY));
        mutatorBuilder.setProximityProbability(Double.parseDouble(configGroup.getValue("proximityProba")));
        Mutator<? extends Attributable> mutator = mutatorBuilder.build();
        /*
        Create the markov engine...
         */
        MarkovEngine engine = new MarkovEngine(simPersons, hamiltonian, mutator, random);
        engine.setListener(engineListeners);

        return engine;
    }

    public static void main(String args[]) {
        Logger.getRootLogger().setLevel(Level.TRACE);

        final Config config = new Config();
        ConfigUtils.loadConfig(config, args[0]);

        long iterations = (long) Double.parseDouble(config.getParam(Simulator.MODULE_NAME, "iterations"));

        MarkovEngine engine = new Simulator().build(config);
        logger.info("Start sampling...");
        engine.run(iterations);
        logger.info("End sampling.");

        Executor.shutdown();
    }
}
