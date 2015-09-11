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

//		for(PlainPerson person : persons) {
//			String age = person.getAttribute(CommonKeys.PERSON_AGE);
//			if(age != null) person.setUserData(DistanceVector.AGE_KEY, Double.parseDouble(age));
//			String income = person.getAttribute(CommonKeys.HH_INCOME);
//			if(income != null) person.setUserData(DistanceVector.INCOME_KEY, Double.parseDouble(income));
//
//		}

		final AnalyzerTaskComposite task = new AnalyzerTaskComposite();
		task.addTask(new AgeIncomeCorrelation());
		task.addTask(new DependendLegVariableAnalyzerTask(CommonKeys.LEG_START_TIME, CommonKeys.LEG_ROUTE_DISTANCE));
		task.addTask(new MunicipalityDistanceTask());

		ProxyAnalyzer.analyze(refPersons, task, "/home/johannes/gsv/germany-scenario/sim/output/ref/");

		Random random = new XORShiftRandom();

//		persons = PersonCloner.weightedClones(persons, 100000, random);

		final HamiltonianComposite h = new HamiltonianComposite();
//		h.addComponent(new DistanceVector(persons, random), 100);
//		Hamiltonian h = new DistanceVector(persons);

		Set<PlainPerson> simPersons = new HashSet<>(100000);

		for(int i = 0; i < 100000; i++) {
			PlainPerson p = new PlainPerson(String.valueOf(i));

			p.setAttribute(CommonKeys.HH_INCOME, String.valueOf(random.nextInt(8000)));
//			p.setUserData(DistanceVector.INCOME_KEY, new Double(p.getAttribute(CommonKeys.HH_INCOME)));

			p.setAttribute(CommonKeys.PERSON_AGE, String.valueOf(random.nextInt(100)));
//			p.setUserData(DistanceVector.AGE_KEY, new Double(p.getAttribute(CommonKeys.PERSON_AGE)));

			simPersons.add(p);
		}

		MutatorComposite<? extends Attributable> factory = new MutatorComposite<>(random);
//		factory.addFactory(new IncomeMutatorFactory(random));
//		HistogramSync1D histSyncAge = new HistogramSync1D((Set<PlainPerson>)refPersons, (Set<PlainPerson>)simPersons, CommonKeys.PERSON_AGE,
//				DistanceVector
//				.AGE_KEY, null);
//		HistogramSync1D histSyncIncome = new HistogramSync1D(refPersons, simPersons, CommonKeys.HH_INCOME, DistanceVector.INCOME_KEY, null);
//
//		HistogramSync2D histSyncAgeIncomeMean = new HistogramSync2D(refPersons, simPersons, DistanceVector.AGE_KEY, DistanceVector.INCOME_KEY, new LinearDiscretizer(1.0));

//		HistoSyncComposite comp = new HistoSyncComposite();
//		comp.addComponent(histSyncAge);
//		comp.addComponent(histSyncIncome);
//		comp.addComponent(histSyncAgeIncomeMean);

//		factory.addFactory(new AgeMutatorFactory(random, comp));
//		factory.addFactory(new IncomeMutatorFactory(random, comp));

//		h.addComponent(histSyncAge, 50000);
//		h.addComponent(histSyncIncome, 70000);
//		h.addComponent(histSyncAgeIncomeMean, 0.05);

		UnivariatFrequency ageHamiltonian = new UnivariatFrequency(refPersons, simPersons, CommonKeys.PERSON_AGE,
				new LinearDiscretizer(1.0));
		h.addComponent(ageHamiltonian, 1000000);// * cachedPersons.size());

//		UnivariatFrequency income = new UnivariatFrequency(refPersons, cachedPersons, CommonKeys.HH_INCOME, new
//				InterpolatingDiscretizer(personValues(refPersons, CommonKeys.HH_INCOME)));
		UnivariatFrequency income = new UnivariatFrequency(refPersons, simPersons, CommonKeys.HH_INCOME, new
				LinearDiscretizer(500));
		h.addComponent(income, 10000000);

		BivariatMean ageIncome = new BivariatMean(refPersons, simPersons, CommonKeys.PERSON_AGE, CommonKeys
				.HH_INCOME, new LinearDiscretizer(1.0));
		h.addComponent(ageIncome, 5);

		AttributeChangeListenerComposite c1 = new AttributeChangeListenerComposite();
		c1.addComponent(ageHamiltonian);
		c1.addComponent(ageIncome);
		factory.addMutator(new AgeMutatorBuilder(c1, random).build());

		AttributeChangeListenerComposite c2 = new AttributeChangeListenerComposite();
		c2.addComponent(income);
		c2.addComponent(ageIncome);
		factory.addMutator(new IncomeMutatorFactory(c2, random).build());

		MarkovEngine sampler = new MarkovEngine(simPersons, h, factory, random);

		MarkovEngineListenerComposite listener = new MarkovEngineListenerComposite();

//		Map<Object, String> map = new HashMap<>();
//		map.put(DistanceVector.AGE_KEY, CommonKeys.PERSON_AGE);
//		map.put(DistanceVector.INCOME_KEY, CommonKeys.HH_INCOME);

//		listener.addComponent(new SynchronizeUserData(map, 100000));
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
