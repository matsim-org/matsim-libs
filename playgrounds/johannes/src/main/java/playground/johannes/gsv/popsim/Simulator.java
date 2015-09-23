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
import org.apache.log4j.Logger;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import playground.johannes.gsv.synPop.analysis.AnalyzerTaskComposite;
import playground.johannes.gsv.synPop.analysis.LegGeoDistanceTask;
import playground.johannes.gsv.synPop.analysis.ProxyAnalyzer;
import playground.johannes.gsv.synPop.data.DataPool;
import playground.johannes.gsv.synPop.data.FacilityData;
import playground.johannes.gsv.synPop.data.FacilityDataLoader;
import playground.johannes.gsv.synPop.data.LandUseDataLoader;
import playground.johannes.gsv.synPop.sim3.ReplaceActTypes;
import playground.johannes.sna.math.LinearDiscretizer;
import playground.johannes.socialnetworks.utils.XORShiftRandom;
import playground.johannes.synpop.data.*;
import playground.johannes.synpop.data.io.PopulationIO;
import playground.johannes.synpop.processing.*;
import playground.johannes.synpop.sim.*;
import playground.johannes.synpop.sim.data.CachedPerson;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

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
				"popInputFile"), new
				PlainFactory());
		logger.info(String.format("Loaded %s persons.", refPersons.size()));

		Random random = new XORShiftRandom(Long.parseLong(config.getParam("global", "randomSeed")));

		TaskRunner.run(new ReplaceActTypes(), refPersons);
		new GuessMissingActTypes(random).apply(refPersons);
		TaskRunner.run(new EpisodeTask() {
			@Override
			public void apply(Episode episode) {
				for (Segment leg : episode.getLegs()) {
					leg.setAttribute(CommonKeys.LEG_GEO_DISTANCE, leg.getAttribute(CommonKeys.LEG_ROUTE_DISTANCE));
				}
			}
		}, refPersons);

		logger.info("Cloning persons...");
		Set<PlainPerson> simPersons = (Set<PlainPerson>) PersonUtils.weightedCopy(refPersons, new PlainFactory(), 100000, random);
		logger.info(String.format("Generated %s persons.", simPersons.size()));

		logger.info("Loading data...");
		DataPool dataPool = new DataPool();
		dataPool.register(new FacilityDataLoader(config.getParam(MODULE_NAME, "facilities"), random), FacilityDataLoader.KEY);
		dataPool.register(new LandUseDataLoader(config.getModule(MODULE_NAME)), LandUseDataLoader.KEY);
		logger.info("Done.");

		logger.info("Validation data...");
		//FacilityZoneValidator.validate(dataPool, ActivityTypes.HOME, 3);
		//FacilityZoneValidator.validate(dataPool, ActivityTypes.HOME, 1);
		logger.info("Done.");

		logger.info("Setting up sampler...");
		/*
		 * Distribute population according to zone values.
		 */
		new SetHomeFacilities(dataPool, random).apply(simPersons);
		/*
		 * Assign random activity facilities.
		 */
		TaskRunner.run(new SetActivityFacilities((FacilityData) dataPool.get(FacilityDataLoader.KEY)), simPersons);
		TaskRunner.run(new LegAttributeRemover(CommonKeys.LEG_GEO_DISTANCE), simPersons);
		TaskRunner.run(new CalculateGeoDistance((FacilityData) dataPool.get(FacilityDataLoader.KEY)), simPersons);

		final String output = config.getParam(MODULE_NAME, "output");

		final AnalyzerTaskComposite task = new AnalyzerTaskComposite();
		task.addTask(new LegGeoDistanceTask(CommonValues.LEG_MODE_CAR));

		ProxyAnalyzer.analyze(refPersons, task, String.format("%s/ref/", output));

		final HamiltonianComposite hamiltonians = new HamiltonianComposite();

		MutatorComposite<? extends Attributable> mutators = new MutatorComposite<>(random);

        UnivariatFrequency distance = buildDistanceHamiltonian(refPersons, simPersons);
        hamiltonians.addComponent(distance, 1e6);


        FacilityMutatorBuilder fBuilder = new FacilityMutatorBuilder(dataPool, random);
        fBuilder.addToBlacklist(ActivityTypes.HOME);

        AttributeChangeListenerComposite listeners = new AttributeChangeListenerComposite();
        listeners.addComponent(new GeoDistanceUpdater(distance));
//        listeners.addComponent(distance);
        fBuilder.setListener(listeners);
        mutators.addMutator(fBuilder.build());

		MarkovEngine sampler = new MarkovEngine(simPersons, hamiltonians, mutators, random);

		MarkovEngineListenerComposite listener = new MarkovEngineListenerComposite();

		listener.addComponent(new MarkovEngineListener() {

			AnalyzerListener l = new AnalyzerListener(task, String.format("%s/sim/", output), 1000000);

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

		sampler.run(10000001);
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



		UnivariatFrequency f = new UnivariatFrequency(refLegs, simLegs, CommonKeys.LEG_GEO_DISTANCE, new
				LinearDiscretizer(10000));

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

}
