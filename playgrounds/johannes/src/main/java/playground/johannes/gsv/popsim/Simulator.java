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

package playground.johannes.gsv.popsim;

import gnu.trove.TDoubleArrayList;
import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.analysis.UnivariateRealFunction;
import org.apache.log4j.Logger;
import org.matsim.contrib.common.stats.Discretizer;
import org.matsim.contrib.common.stats.FixedSampleSizeDiscretizer;
import org.matsim.contrib.common.stats.LinearDiscretizer;
import org.matsim.contrib.socnetgen.socialnetworks.utils.XORShiftRandom;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import playground.johannes.gsv.synPop.analysis.AnalyzerTaskComposite;
import playground.johannes.gsv.synPop.analysis.LegGeoDistanceTask;
import playground.johannes.gsv.synPop.analysis.ProxyAnalyzer;
import playground.johannes.gsv.synPop.mid.Route2GeoDistance;
import playground.johannes.gsv.synPop.sim3.ReplaceActTypes;
import playground.johannes.synpop.data.*;
import playground.johannes.synpop.data.io.PopulationIO;
import playground.johannes.synpop.gis.DataPool;
import playground.johannes.synpop.gis.FacilityData;
import playground.johannes.synpop.gis.FacilityDataLoader;
import playground.johannes.synpop.gis.ZoneDataLoader;
import playground.johannes.synpop.processing.CalculateGeoDistance;
import playground.johannes.synpop.processing.GuessMissingActTypes;
import playground.johannes.synpop.processing.LegAttributeRemover;
import playground.johannes.synpop.processing.TaskRunner;
import playground.johannes.synpop.sim.*;
import playground.johannes.synpop.sim.data.CachedPerson;
import playground.johannes.synpop.sim.data.Converters;
import playground.johannes.synpop.sim.data.DoubleConverter;
import playground.johannes.synpop.source.mid2008.MiDKeys;

import java.io.IOException;
import java.util.*;

/**
 * @author johannes
 *
 */
public class Simulator {

	private static final Logger logger = Logger.getLogger(Simulator.class);

	private static final String MODULE_NAME = "synPopSim";
	/**
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		Config config = new Config();
		ConfigUtils.loadConfig(config, args[0]);

		logger.info("Loading persons...");
		Set<PlainPerson> refPersons = (Set<PlainPerson>) PopulationIO.loadFromXML(config.findParam(MODULE_NAME,
				"popInputFile"), new PlainFactory());
		logger.info(String.format("Loaded %s persons.", refPersons.size()));

		Random random = new XORShiftRandom(Long.parseLong(config.getParam("global", "randomSeed")));

		TaskRunner.run(new ReplaceActTypes(), refPersons);
		new GuessMissingActTypes(random).apply(refPersons);
		TaskRunner.run(new Route2GeoDistance(new Route2GeoDistFunction()), refPersons);

		logger.info("Cloning persons...");
		Set<PlainPerson> simPersons = (Set<PlainPerson>) PersonUtils.weightedCopy(refPersons, new PlainFactory(), 100000, random);
		logger.info(String.format("Generated %s persons.", simPersons.size()));

		logger.info("Loading data...");
		DataPool dataPool = new DataPool();
		dataPool.register(new FacilityDataLoader(config.getParam(MODULE_NAME, "facilities"), random), FacilityDataLoader.KEY);
//		dataPool.register(new LandUseDataLoader(config.getModule(MODULE_NAME)), LandUseDataLoader.KEY);
		dataPool.register(new ZoneDataLoader(config.getModule(MODULE_NAME)), ZoneDataLoader.KEY);
		logger.info("Done.");

//		logger.info("Validation data...");
//		FacilityZoneValidator.validate(dataPool, ActivityTypes.HOME, 3);
//		FacilityZoneValidator.validate(dataPool, ActivityTypes.HOME, 1);
//		logger.info("Done.");

		logger.info("Setting up sampler...");
		/*
		 * Distribute population according to zone values.
		 */
		new SetHomeFacilities(dataPool, "modena", random).apply(simPersons);
		/*
		 * Assign random activity facilities.
		 */
		TaskRunner.run(new SetActivityFacilities((FacilityData) dataPool.get(FacilityDataLoader.KEY)), simPersons);
		TaskRunner.run(new LegAttributeRemover(CommonKeys.LEG_GEO_DISTANCE), simPersons);
		TaskRunner.run(new CalculateGeoDistance((FacilityData) dataPool.get(FacilityDataLoader.KEY)), simPersons);

		final String output = config.getParam(MODULE_NAME, "output");

		final AnalyzerTaskComposite task = new AnalyzerTaskComposite();
		task.addTask(new LegGeoDistanceTask(CommonValues.LEG_MODE_CAR));
		task.addTask(new GeoDistLau2ClassTask());

		ProxyAnalyzer.analyze(refPersons, task, String.format("%s/ref/", output));

		final HamiltonianComposite hamiltonians = new HamiltonianComposite();

		MutatorComposite<? extends Attributable> mutators = new MutatorComposite<>(random);

        UnivariatFrequency distance = buildDistanceHamiltonian(refPersons, simPersons);
        hamiltonians.addComponent(distance, 1e6);

		BivariatMean distanceLau2Class = buildDistanceLau2Hamiltonian(refPersons, simPersons);
		hamiltonians.addComponent(distanceLau2Class, 2.0);

        FacilityMutatorBuilder fBuilder = new FacilityMutatorBuilder(dataPool, random);
        fBuilder.addToBlacklist(ActivityTypes.HOME);

        AttributeChangeListenerComposite listeners = new AttributeChangeListenerComposite();
		AttributeChangeListenerComposite distListeners = new AttributeChangeListenerComposite();
		distListeners.addComponent(distance);
		distListeners.addComponent(distanceLau2Class);
        listeners.addComponent(new GeoDistanceUpdater(distListeners));
        fBuilder.setListener(listeners);
        mutators.addMutator(fBuilder.build());

		MarkovEngine sampler = new MarkovEngine(simPersons, hamiltonians, mutators, random);

		MarkovEngineListenerComposite listener = new MarkovEngineListenerComposite();

		listener.addComponent(new MarkovEngineListener() {

			AnalyzerListener l = new AnalyzerListener(task, String.format("%s/sim/", output), 10000000);

			@Override
			public void afterStep(Collection<CachedPerson> population, Collection<? extends Attributable> mutations, boolean accepted) {
				l.afterStep(population, null, accepted);
			}
		});
		listener.addComponent(new MarkovEngineListener() {
			HamiltonianLogger l = new HamiltonianLogger(hamiltonians, 100000);
			@Override
			public void afterStep(Collection<CachedPerson> population, Collection<? extends Attributable> mutations, boolean accepted) {
				l.afterStep(population, null, accepted);
			}
		});

		sampler.setListener(listener);

		sampler.run(100000001);
	}

	private static double[] personValues(Set<? extends Person> persons, String attrKey) {
		TDoubleArrayList values = new TDoubleArrayList(persons.size());
		for(Person person : persons) {
			String strVal = person.getAttribute(attrKey);
			if(strVal != null) {
				values.add(Double.parseDouble(strVal));
			}
		}

		return values.toNativeArray();
	}

	private static UnivariatFrequency buildDistanceHamiltonian(Set<PlainPerson> refPersons, Set<PlainPerson>
			simPersons) {
		Set<Attributable> refLegs = getLegs(refPersons);
		Set<Attributable> simLegs = getLegs(simPersons);

		List<Double> values = new LegDoubleCollector(CommonKeys.LEG_GEO_DISTANCE).collect(refPersons);
		double[] nativeValues = CollectionUtils.toNativeArray(values);
		Discretizer disc = FixedSampleSizeDiscretizer.create(nativeValues, 50, 100);

		UnivariatFrequency f = new UnivariatFrequency(refLegs, simLegs, CommonKeys.LEG_GEO_DISTANCE, disc);

		return f;
	}

	private static Set<Attributable> getLegs(Set<? extends Person> persons) {
		Set<Attributable> legs = new HashSet<>();
		for(Person p : persons) {
			Episode e = p.getEpisodes().get(0);
			legs.addAll(e.getLegs());
		}

		return legs;
	}

	private static BivariatMean buildDistanceLau2Hamiltonian(Set<PlainPerson> refPersons, Set<PlainPerson> simPersons) {
		copyLau2ClassAttribute(refPersons);
		copyLau2ClassAttribute(simPersons);

		Set<Attributable> refLegs = getLegs(refPersons);
		Set<Attributable> simLegs = getLegs(simPersons);

		Converters.register(MiDKeys.PERSON_LAU2_CLASS, DoubleConverter.getInstance());
		BivariatMean bm = new BivariatMean(refLegs, simLegs, MiDKeys.PERSON_LAU2_CLASS, CommonKeys.LEG_GEO_DISTANCE,
				new LinearDiscretizer(1.0));

		return bm;
	}

	private static void copyLau2ClassAttribute(Set<PlainPerson> persons) {
		for(Person p : persons) {
			String lau2Class = p.getAttribute(MiDKeys.PERSON_LAU2_CLASS);
			for(Episode e : p.getEpisodes()) {
				for(Segment leg : e.getLegs()) {
					leg.setAttribute(MiDKeys.PERSON_LAU2_CLASS, lau2Class);
				}
			}
		}
	}
	public static class Route2GeoDistFunction implements UnivariateRealFunction {

		@Override
		public double value(double x) throws FunctionEvaluationException {
			double routDist = x/1000.0;
			double factor = 0.77 - Math.exp(-0.17 * Math.max(20, routDist) - 1.48);
			return routDist * factor * 1000;
		}
	}

}
