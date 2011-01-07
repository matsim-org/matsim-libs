/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package playground.mrieser.core.mobsim.features.fastQueueNetworkFeature;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class BusyWaitCyclicBarrier {

	private final int parties;
	private final AtomicLong generation = new AtomicLong(Long.MIN_VALUE);
	private final AtomicInteger counter = new AtomicInteger(0);

	public BusyWaitCyclicBarrier(final int parties) {
		this.parties = parties;
		this.counter.set(parties);
	}

	public int await() throws InterruptedException, BrokenBarrierException {
		long gen = this.generation.get();
		int i = this.counter.decrementAndGet();
		if (i == 0) {
			this.counter.set(this.parties);
			this.generation.incrementAndGet();
		}
//		System.out.println("Waiting: i = " + i + " gen = " + gen);
		while (gen == this.generation.get()) {
			 ; // busy waiting
		}
		return i;
	}

}
