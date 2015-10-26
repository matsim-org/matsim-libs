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

package playground.johannes.gsv.synPop.sim3;

import org.apache.log4j.Logger;
import org.matsim.contrib.common.collections.CollectionUtils;
import org.matsim.contrib.common.util.XORShiftRandom;
import playground.johannes.synpop.data.Person;

import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * @author johannes
 *
 */
public class Sampler {
	
//	private final static Object H_KEY = new Object();
	private static final Logger logger = Logger.getLogger(Sampler.class);
	
	private final Collection<? extends Person> population;
	
	private final Hamiltonian hamiltonian;
	
	private final MutatorFactory mutatorFactory;
	
	private final Random random;
	
	private SamplerListener listener;
	
	private PopulationSegmenter segmenter = new DefaultSegmenter();
	
	public Sampler(Collection<? extends Person> population, Hamiltonian hamiltonian, MutatorFactory factory, Random
			random) {
		this.population = population;
		this.hamiltonian = hamiltonian;
		this.mutatorFactory = factory;
		this.random = random;
		
		listener = new DefaultListener();
	}
	
	
	public void setSamplerListener(SamplerListener listener) {
		this.listener = listener;
	}
	
	public void setSegmenter(PopulationSegmenter segmenter) {
		this.segmenter = segmenter;
	}
	
	public void run(long iters, int numThreads) {
		/*
		 * split collection in approx even segments
		 */
		List<Person>[] segments = segmenter.split((Set<Person>)population, numThreads);
		/*
		 * create threads
		 */
		Thread[] threads = new Thread[numThreads];
		for(int i = 0; i < numThreads; i++) {
			
			Mutator thisMutator = mutatorFactory.newInstance();
			Random thisRandom = new XORShiftRandom(random.nextLong());
			threads[i] = new Thread(new SampleThread(segments[i], thisMutator, iters, thisRandom));
			logger.info(String.format("%s: %s persons.", threads[i].getName(), segments[i].size()));
			threads[i].start();
		}
		/*
		 * wait for threads
		 */
		for(int i = 0; i < numThreads; i++) {
			try {
				threads[i].join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	private class SampleThread implements Runnable {

		private final List<Person> population;
		
		private final Mutator mutator;
		
		private final Random random;
		
		private final long iterations;
		
		public SampleThread(List<Person> population, Mutator mutator, long iterations, Random random) {
			this.population = population;
			this.mutator = mutator;
			this.iterations = iterations;
			this.random = random;
		}
		
		@Override
		public void run() {
			for(long i = 0; i < iterations; i++) {
				step();
			}
		}
	
		public void step() {
			/*
			 * select person
			 */
			List<Person> mutations = mutator.select(population);
			/*
			 * evaluate
			 */
			double H_before = 0;
			for(int i = 0; i < mutations.size(); i++) {
				H_before += hamiltonian.evaluate(mutations.get(i));
			}

			boolean accepted = false;
			if (mutator.modify(mutations)) {
//				listener.afterModify(person1);
				/*
				 * evaluate
				 */
				double H_after = 0;
				for(int i = 0; i < mutations.size(); i++) {
				 H_after += hamiltonian.evaluate(mutations.get(i));
				}

				double p = 1 / (1 + Math.exp(H_after - H_before));

				if (p >= random.nextDouble()) {
					accepted = true;
				} else {
					mutator.revert(mutations);

				}
			}
			
			listener.afterStep(Sampler.this.population, mutations, accepted);
		}

	}
	
	private static class DefaultListener implements SamplerListener {

				@Override
		public void afterStep(Collection<? extends Person> population, Collection<? extends Person> mutations, boolean accepted) {
			
		}
		
	}
	
	private static class DefaultSegmenter implements PopulationSegmenter {

		@Override
		public List<Person>[] split(Collection<Person> persons, int segments) {
			int n = Math.min(persons.size(), segments);
			return CollectionUtils.split(persons, n);
		}
		
	}
}
