/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

package playground.johannes.gsv.synPop.sim;

import java.util.List;
import java.util.Random;

import org.apache.log4j.Logger;

import playground.johannes.gsv.synPop.ProxyPerson;

/**
 * @author johannes
 *
 */
public class Sampler {
	
	private static final Logger logger = Logger.getLogger(Sampler.class);

	private List<Mutator> mutators;
	
	private Random random;
	
	private List<ProxyPerson> persons;
	
	private Hamiltonian hamiltonian;
	
	public void run(long burnin, List<ProxyPerson> persons, Hamiltonian hamiltonian, List<Mutator> mutators, Random random) {
		this.persons = persons;
		this.hamiltonian = hamiltonian;
		this.mutators = mutators;
		this.random = random;
		
		int accepts = 0;
		int loginterval = 1000;
		long time = System.currentTimeMillis();
		for(long i = 0; i < burnin; i++) {
			if(step())
				accepts++;
			
			if(i % loginterval == 0) {
				double h = hamiltonian.evaluate(persons);
				long t = System.currentTimeMillis() - time;
				logger.info(String.format("[%s] Accepted %s of %s steps. Hamiltonian: %s, time; %s", i, accepts, loginterval, h, t));
				accepts = 0;
				time = System.currentTimeMillis();
			}
		}
	}
	
	public boolean step() {
		/*
		 * create new mutated person
		 */
		int idx = random.nextInt(persons.size());
		ProxyPerson template = persons.get(idx);
		ProxyPerson mutation = template.clone();
		/*
		 * select mutator
		 */
		Mutator mutator = mutators.get(random.nextInt(mutators.size()));
		mutator.mutate(template, mutation);
		/*
		 * evaluate
		 */
		double H = hamiltonian.evaluate(template, mutation);
		
		double p = 1 / (1 + Math.exp(-H));
		
		if(p >= random.nextDouble()) {
			persons.set(idx, mutation);
			return true;
		} else {
			return false;
		}
	}
}
