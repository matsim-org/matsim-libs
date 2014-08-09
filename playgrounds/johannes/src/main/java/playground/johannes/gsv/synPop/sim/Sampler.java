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
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.log4j.Logger;

import playground.johannes.gsv.synPop.ProxyPerson;
import playground.johannes.socialnetworks.utils.CollectionUtils;

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
	
	public void addListener(SamplerListener listener) {
		listerners.add(listener);
	}
	
	/*
	 * This doesn't work correctly in parallel environments since the population
	 * may change during creation of the hash set.
	 */
	public Collection<ProxyPerson> getPopulationCopy() {
		return new HashSet<ProxyPerson>(population);
	}
	
	public void run(Collection<ProxyPerson> population, long burnin) {
		this.population = new ArrayList<ProxyPerson>(population);
		
		Timer timer = new Timer();
		Task task = new Task();
		this.addListener(task);
		timer.schedule(task, 2000, 2000);
		
		for(long i = 0; i < burnin; i++) {
			step();
		}
		
		timer.cancel();
	}
	
	public void run(Collection<ProxyPerson> population, long burnin, int numThreads) {
		this.population = new ArrayList<ProxyPerson>(population);
		
		Timer timer = new Timer();
		Task task = new Task();
		this.addListener(task);
		timer.schedule(task, 2000, 2000);
		
		ExecutorService executor = Executors.newFixedThreadPool(numThreads);
		
		SampleThread[] threads = new SampleThread[numThreads];
		for(int i = 0; i < numThreads; i++)
			threads[i] = new SampleThread();
		
		Future<?>[] futures = new Future[numThreads];
		/*
		 * split collection in approx even segments
		 */
		int n = Math.min(population.size(), threads.length);
		List<ProxyPerson>[] segments = CollectionUtils.split(population, n);
		/*
		 * submit tasks
		 */
		int startIdx = 0;
		for(int i = 0; i < segments.length; i++) {
//			threads[i].init(segments[i], network, linkTravelTimes, eventManager);
			threads[i].burnin = burnin;
			threads[i].startIdx = startIdx;
			threads[i].subListSize = segments[i].size();
			startIdx += segments[i].size();
			futures[i] = executor.submit(threads[i]);
		}
		/*
		 * wait for threads
		 */
		for(int i = 0; i < segments.length; i++) {
			try {
				futures[i].get();
			} catch (InterruptedException e) {
				e.printStackTrace();
			} catch (ExecutionException e) {
				e.printStackTrace();
			}
		}
		
		timer.cancel();
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
			listener.afterStep(population, template, mutation, result);
		
		return result;
	}
	
	private class SampleThread implements Runnable {

		private long burnin;
		
		private int startIdx;
		
		private int subListSize;
		
		@Override
		public void run() {
			for(long i = 0; i < burnin; i++) {
				step();
			}
		}
		
		private boolean step() {
			/*
			 * create new mutated person
			 */
			int idx = startIdx + random.nextInt(subListSize);
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
				listener.afterStep(population, template, mutation, result);
			
			return result;
		}
	}
	
	private class Task extends TimerTask implements SamplerListener {

		private long iter;
		
		private long accepts;
		
		private long iters2;
		
		@Override
		public void run() {
			logger.info(String.format(Locale.US, "[%s] Accepted %s of %s steps (%.2f %%).", iter, accepts, iters2, accepts/(double)iters2 * 100));
			accepts = 0;
			iters2 = 0;
		}

		@Override
		public void afterStep(Collection<ProxyPerson> population, ProxyPerson original, ProxyPerson mutation, boolean accepted) {
			iter++;
			iters2++;
			if(accepted)
				accepts++;
		}
		
	}
}
