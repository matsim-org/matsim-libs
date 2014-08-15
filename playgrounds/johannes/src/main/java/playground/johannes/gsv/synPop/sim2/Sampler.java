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

package playground.johannes.gsv.synPop.sim2;

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
import playground.johannes.socialnetworks.utils.XORShiftRandom;

/**
 * @author johannes
 *
 */
public class Sampler {
	
	private static final Logger logger = Logger.getLogger(Sampler.class);

	private MutatorFactory mutatorFactory;
	
	private SamplerListener listerners;
	
	private final Random random;
	
	private List<ProxyPerson> population;
	
	private Hamiltonian hamiltonian;
	
	private final int numThreads = 1;
	
	private List<ProxyPerson>[] segments;
	
	private ExecutorService executor;
	
	private SampleThread[] threads;
	
	private long microSteps;
	
	public Sampler(Random random) {
		this.random = random;
//		this.mutators = new ArrayList<Mutator>();
//		this.listerners = new ArrayList<SamplerListener>();
	}
	
	public void setHamiltonian(Hamiltonian hamiltonian) {
		this.hamiltonian = hamiltonian;
	}
	
	public int getNumThreads() {
		return numThreads;
	}
//	public void addMutator(Mutator mutator) {
//		mutators.add(mutator);
//	}
//	
//	public void addListener(SamplerListener listener) {
//		listerners.add(listener);
//	}
	
	/*
	 * This doesn't work correctly in parallel environments since the population
	 * may change during creation of the hash set.
	 */
	public Collection<ProxyPerson> getPopulationCopy() {
		return new HashSet<ProxyPerson>(population);
	}
	
	public void run(Collection<ProxyPerson> population, long burnin) {
		this.population = new ArrayList<ProxyPerson>(population);
		
		executor = Executors.newFixedThreadPool(numThreads);
		
		/*
		 * split collection in approx even segments
		 */
		int n = Math.min(population.size(), numThreads);
		segments = CollectionUtils.split(population, n);
		/*
		 * create threads
		 */
		threads = new SampleThread[numThreads];
		for(int i = 0; i < numThreads; i++) {
			Mutator thisMutator = mutatorFactory.newInstance();
			Random thisRandom = new XORShiftRandom(random.nextLong());
			threads[i] = new SampleThread(segments[i], hamiltonian, thisMutator, microSteps, thisRandom);
		}
		
		
		Timer timer = new Timer();
		Task task = new Task();
//		this.addListener(task);
		timer.schedule(task, 2000, 2000);
		
		for(long i = 0; i < burnin; i += (numThreads * microSteps)) {
			step();
		}
		
		timer.cancel();
	}
	
	public void step() {
		Future<?>[] futures = new Future[numThreads];
		/*
		 * submit tasks
		 */
		for(int i = 0; i < numThreads; i++) {
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
		
		
		listerners.afterStep(population, null);
		
		
	}
	
	public void pause() {
		for(SampleThread thread : threads) {
			thread.suspend();
		}
		
		for(SampleThread thread : threads) {
			while(!thread.isSuspended()); // TODO: is this the right way?
		}
	}
	
	public void resume() {
		for(SampleThread thread : threads) {
			thread.resume();
		}
	}
	
	private class SampleThread implements Runnable {

		private List<ProxyPerson> population;
		
		private Hamiltonian hamiltonian;
		
		private Mutator mutator;
		
		private final Random random;
		
		private long acceptedSteps;
		
		private final long iterations;
		
		private boolean suspendFlag;
		
		private boolean suspended;
		
		public SampleThread(List<ProxyPerson> population, Hamiltonian hamiltonian, Mutator mutator, long iterations, Random random) {
			this.population = population;
			this.hamiltonian = hamiltonian;
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
			int idx = random.nextInt(population.size());
			ProxyPerson person = population.get(idx);
			/*
			 * select mutator
			 */
			
			double H_before = hamiltonian.evaluate(person);
			
			if (mutator.modify(person)) {
				/*
				 * evaluate
				 */
				double H_after = hamiltonian.evaluate(person);

				double p = 1 / (1 + Math.exp(H_after - H_before));

				if (p >= random.nextDouble()) {
					acceptedSteps++;
				} else {
					mutator.revert(person);
				}
			}
			
			listerners.afterStep(population, person);
			
			synchronized(this) {
				while(suspendFlag) {
					try {
						suspended = true;
						wait();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		}
		
		public long getAcceptedSteps() {
			return acceptedSteps;
		}
		
		public void suspend() {
			suspendFlag = true;
		}
		
		public synchronized void resume() {
			suspendFlag = false;
			notify();
		}
		
		public boolean isSuspended() {
			return suspended;
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
		public void afterStep(Collection<ProxyPerson> population, ProxyPerson original) {
			iter++;
			iters2++;
//			if(accepted)
//				accepts++;
		}
		
	}
}
