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
import playground.johannes.synpop.data.Person;
import playground.johannes.synpop.data.PlainPerson;

import java.util.Collection;
import java.util.concurrent.Phaser;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author johannes
 *
 */
public class BlockingSamplerListener implements SamplerListener {

	private static final Logger logger = Logger.getLogger(BlockingSamplerListener.class);
	
	private final AtomicLong iters = new AtomicLong();
	
	private final long interval;
	
	private volatile long next; // not sure if this needs to be volatile
	
	private final SamplerListener delegate;
	
	private final ThePhaser phaser;
	
	public BlockingSamplerListener(SamplerListener delegate, long interval, int numThreads) {
		this.delegate = delegate;
		this.interval = interval;
		this.next = 0;
		
		phaser = new ThePhaser();
		for(int i = 0; i < numThreads; i++) phaser.register();
	}
	
	@Override
	public void afterStep(Collection<? extends Person> population, Collection<? extends Person> mutations, boolean accept) {
		if(iters.get() >= next) {
			phaser.setState(population, mutations, accept);
			logger.debug(String.format("Thread %s: Arrived at barrier.", Thread.currentThread().getName()));
			phaser.arriveAndAwaitAdvance();
			logger.debug(String.format("Thread %s: Advanced.", Thread.currentThread().getName()));
		} else {
			delegate.afterStep(population, mutations, accept);
		}
		iters.incrementAndGet();
	}
	
	private class ThePhaser extends Phaser {

		private Collection<? extends Person> tmpPopulation;
		
		private Collection<? extends Person> tmpMutations;
		
		private boolean tmpAccept;

		private synchronized void setState(Collection<? extends Person> population, Collection<? extends Person>
				mutations,
										   boolean	accept) {
			this.tmpPopulation = population;
			this.tmpMutations = mutations;
			this.tmpAccept = accept;
		}
		
		@Override
		protected boolean onAdvance(int phase, int registeredParties) {
			logger.debug(String.format("Thread %s: on advance.", Thread.currentThread().getName()));
			for(int i = 0; i < registeredParties; i++)
				delegate.afterStep(tmpPopulation, tmpMutations, tmpAccept);
			
			next += interval;
			return false;
		}
		
	}
}
