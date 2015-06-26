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

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import playground.johannes.gsv.synPop.CommonKeys;
import playground.johannes.gsv.synPop.ProxyPerson;
import playground.johannes.gsv.synPop.analysis.AnalyzerTaskComposite;
import playground.johannes.gsv.synPop.analysis.ProxyAnalyzer;
import playground.johannes.gsv.synPop.io.XMLParser;
import playground.johannes.gsv.synPop.sim3.HamiltonianComposite;
import playground.johannes.gsv.synPop.sim3.HamiltonianLogger;
import playground.johannes.gsv.synPop.sim3.MutatorCompositeFactory;
import playground.johannes.gsv.synPop.sim3.Sampler;
import playground.johannes.gsv.synPop.sim3.SamplerListenerComposite;
import playground.johannes.socialnetworks.utils.XORShiftRandom;

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
		XMLParser parser = new XMLParser();
		parser.setValidating(false);
		parser.parse("/home/johannes/gsv/germany-scenario/mid2008/pop/pop.xml");
		
		Set<ProxyPerson> persons = parser.getPersons();
		
		AnalyzerTaskComposite task = new AnalyzerTaskComposite();
		task.addTask(new AgeIncomeCorrelation());
		
		ProxyAnalyzer.analyze(persons, task, "/home/johannes/gsv/germany-scenario/sim/output/ref/");
		
		Random random = new XORShiftRandom();
		
//		persons = PersonCloner.weightedClones(persons, 100000, random);

		HamiltonianComposite h = new HamiltonianComposite();
		h.addComponent(new DistanceVector(persons, random), 100);
//		Hamiltonian h = new DistanceVector(persons);
		
		Set<ProxyPerson> simPersons = new HashSet<>(100000);
		for(int i = 0; i < 10000; i++) {
			ProxyPerson p = new ProxyPerson(String.valueOf(i));
			
			p.setAttribute(CommonKeys.HH_INCOME, String.valueOf(random.nextInt(10000)));
			p.setUserData(DistanceVector.INCOME_KEY, new Double(p.getAttribute(CommonKeys.HH_INCOME)));
			
			p.setAttribute(CommonKeys.PERSON_AGE, String.valueOf(random.nextInt(100)));
			p.setUserData(DistanceVector.AGE_KEY, new Double(p.getAttribute(CommonKeys.PERSON_AGE)));
			
			simPersons.add(p);
		}
		
		MutatorCompositeFactory factory = new MutatorCompositeFactory(random);
		factory.addFactory(new IncomeMutatorFactory(random));
		factory.addFactory(new AgeMutatorFactory(random));
		
		Sampler sampler = new Sampler(simPersons, h, factory, random);
		
		SamplerListenerComposite listener = new SamplerListenerComposite();
		
		Map<Object, String> map = new HashMap<>();
		map.put(DistanceVector.AGE_KEY, CommonKeys.PERSON_AGE);
		map.put(DistanceVector.INCOME_KEY, CommonKeys.HH_INCOME);
		
		
		
		
		listener.addComponent(new SynchronizeUserData(map, 100000));
		listener.addComponent(new AnalyzerListener(task, "/home/johannes/gsv/germany-scenario/sim/output/", 100000));
		listener.addComponent(new HamiltonianLogger(h, 100000));
		
		sampler.setSamplerListener(listener);
		
		sampler.run(10000000, 1);
	}

}
