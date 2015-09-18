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

import java.util.Collection;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author johannes
 * 
 */
public class SamplerLogger implements SamplerListener {

	private static final Logger logger = Logger.getLogger(SamplerLogger.class);

	private final Timer timer;

	private final Task task;

	public SamplerLogger() {
		timer = new Timer();
		task = new Task();
		timer.schedule(task, 2000, 2000);
	}

	@Override
	public void afterStep(Collection<? extends Person> population, Collection<? extends Person> person, boolean accpeted) {
		task.afterStep(population, person, accpeted);

	}

	public void stop() {
		timer.cancel();
	}

	private class Task extends TimerTask implements SamplerListener {

		private final AtomicLong iter = new AtomicLong();

		private final AtomicLong iters2 = new AtomicLong();

		private final AtomicLong accepts = new AtomicLong();

		private long lastIter;

		@Override
		public void run() {
			if (iter.get() > lastIter) {
				double p = accepts.get() / (double) iters2.get() * 100;
				logger.info(String.format(Locale.US, "[%s] Accepted %s of %s steps (%.2f %%).", iter, accepts, iters2, p));
				accepts.set(0);
				iters2.set(0);
				lastIter = iter.get();
			}
		}

		@Override
		public void afterStep(Collection<? extends Person> population, Collection<? extends Person> original, boolean accepted) {
			iter.incrementAndGet();
			iters2.incrementAndGet();
			if (accepted)
				accepts.incrementAndGet();
		}
	}
}
