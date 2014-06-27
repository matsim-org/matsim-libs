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

import java.util.ArrayList;
import java.util.Collection;
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

	private final List<Mutator> mutators;
	
	private final List<SamplerListener> listerners;
	
	private final Random random;
	
	private List<ProxyPerson> population;
	
	private Hamiltonian hamiltonian;
	
	private int logInterval = 100000;
	
	public Sampler(Random random) {
		this.random = random;
		this.mutators = new ArrayList<Mutator>();
		this.listerners = new ArrayList<SamplerListener>();
	}
	
	public void setHamiltonian(Hamiltonian hamiltonian) {
		this.hamiltonian = hamiltonian;
	}
	
	public void addMutator(Mutator mutator) {
		mutators.add(mutator);
	}
	
	public void addListenter(SamplerListener listener) {
		listerners.add(listener);
	}
	
	public void setLogInterval(int logInterval) {
		this.logInterval = logInterval;
	}
	
	public Collection<ProxyPerson> getPopulation() {
		return population;
	}
	
	public void run(Collection<ProxyPerson> population, long burnin) {
		this.population = new ArrayList<ProxyPerson>(population);
		
		int accepts = 0;
		
		long time = System.currentTimeMillis();
		for(long i = 0; i < burnin; i++) {
			if(step())
				accepts++;
			
			if(i % 10000 == 0) {
				logger.info(String.format("[%s] Accepted %s of %s steps.", i, accepts, 10000));
				accepts = 0;
			}
			
			if(i % logInterval == 0) {
				long t = System.currentTimeMillis() - time;
				double h = hamiltonian.evaluate(this.population);
				logger.info(String.format("Total hamiltonian score: %s. Time: %s", h, t));
				time = System.currentTimeMillis();
			}
		}
	}
	
	public boolean step() {
		/*
		 * create new mutated person
		 */
		int idx = random.nextInt(population.size());
		ProxyPerson template = population.get(idx);
		ProxyPerson mutation = template.clone();
		/*
		 * select mutator
		 */
		boolean result;
		Mutator mutator = mutators.get(random.nextInt(mutators.size()));
		if (mutator.mutate(template, mutation)) {
			/*
			 * evaluate
			 */
			double H = hamiltonian.evaluate(template, mutation);

			double p = 1 / (1 + Math.exp(-H));

			if (p >= random.nextDouble()) {
				population.set(idx, mutation);
				
				result = true;
			} else {
				result = false;
			}
		} else {
			result = false;
		}
		
		for(SamplerListener listener : listerners)
			listener.afterStep(template, mutation, result);
		
		return result;
	}
}
