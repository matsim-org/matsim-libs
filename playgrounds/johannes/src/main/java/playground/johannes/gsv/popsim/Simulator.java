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
import playground.johannes.gsv.synPop.analysis.AnalyzerTaskComposite;
import playground.johannes.gsv.synPop.analysis.DependendLegVariableAnalyzerTask;
import playground.johannes.gsv.synPop.analysis.ProxyAnalyzer;
import playground.johannes.sna.math.LinearDiscretizer;
import playground.johannes.socialnetworks.utils.XORShiftRandom;
import playground.johannes.synpop.data.*;
import playground.johannes.synpop.data.io.XMLHandler;
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

	/**
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		XMLHandler parser = new XMLHandler(new PlainFactory());
		parser.setValidating(false);
		parser.parse("/home/johannes/gsv/germany-scenario/mid2008/pop/pop.xml");

		Set<PlainPerson> refPersons = (Set<PlainPerson>)parser.getPersons();


		final AnalyzerTaskComposite task = new AnalyzerTaskComposite();
		task.addTask(new AgeIncomeCorrelation());
		task.addTask(new DependendLegVariableAnalyzerTask(CommonKeys.LEG_START_TIME, CommonKeys.LEG_ROUTE_DISTANCE));
		task.addTask(new MunicipalityDistanceTask());

		ProxyAnalyzer.analyze(refPersons, task, "/home/johannes/gsv/germany-scenario/sim/output/ref/");

		Random random = new XORShiftRandom();


		final HamiltonianComposite h = new HamiltonianComposite();

		Set<PlainPerson> simPersons = new HashSet<>(100000);

		for(int i = 0; i < 100000; i++) {
			PlainPerson p = new PlainPerson(String.valueOf(i));
			p.setAttribute(CommonKeys.HH_INCOME, String.valueOf(random.nextInt(8000)));
			p.setAttribute(CommonKeys.PERSON_AGE, String.valueOf(random.nextInt(100)));
			simPersons.add(p);
		}

		MutatorComposite<? extends Attributable> factory = new MutatorComposite<>(random);

		UnivariatFrequency ageHamiltonian = new UnivariatFrequency(refPersons, simPersons, CommonKeys.PERSON_AGE,
				new LinearDiscretizer(1.0));
		h.addComponent(ageHamiltonian, 1000000);// * cachedPersons.size());

		UnivariatFrequency income = new UnivariatFrequency(refPersons, simPersons, CommonKeys.HH_INCOME, new
				LinearDiscretizer(500));
		h.addComponent(income, 10000000);

		BivariatMean ageIncome = new BivariatMean(refPersons, simPersons, CommonKeys.PERSON_AGE, CommonKeys
				.HH_INCOME, new LinearDiscretizer(1.0));
		h.addComponent(ageIncome, 5);

        UnivariatFrequency distance = new UnivariatFrequency(refPersons, simPersons, CommonKeys.LEG_GEO_DISTANCE, null);
        h.addComponent(distance);

		AttributeChangeListenerComposite c1 = new AttributeChangeListenerComposite();
		c1.addComponent(ageHamiltonian);
		c1.addComponent(ageIncome);
        factory.addMutator(new AgeMutatorBuilder(c1, random).build());

        AttributeChangeListenerComposite c2 = new AttributeChangeListenerComposite();
        c2.addComponent(ageHamiltonian);
        c2.addComponent(income);
        factory.addMutator(new IncomeMutatorBuilder(c2, random).build());

        FacilityMutatorBuilder fBuilder = new FacilityMutatorBuilder(null, random);
        fBuilder.addToBlacklist("home");

        AttributeChangeListenerComposite c3 = new AttributeChangeListenerComposite();
        c3.addComponent(new GeoDistanceUpdater());
        c3.addComponent(distance);
        fBuilder.setListener(c3);
        factory.addMutator(fBuilder.build());

		MarkovEngine sampler = new MarkovEngine(simPersons, h, factory, random);

		MarkovEngineListenerComposite listener = new MarkovEngineListenerComposite();

		listener.addComponent(new MarkovEngineListener() {

			AnalyzerListener l = new AnalyzerListener(task, "/home/johannes/gsv/germany-scenario/sim/output/", 100000);

			@Override
			public void afterStep(Collection<CachedPerson> population, Collection<? extends Attributable> mutations, boolean accepted) {
				l.afterStep(population, null, accepted);
			}
		});
		listener.addComponent(new MarkovEngineListener() {
			HamiltonianLogger l = new HamiltonianLogger(h, 100000);
			@Override
			public void afterStep(Collection<CachedPerson> population, Collection<? extends Attributable> mutations, boolean accepted) {
				l.afterStep(population, null, accepted);
			}
		});

		sampler.setListener(listener);

		sampler.run(4000001);
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

}
