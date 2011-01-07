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

import org.junit.Test;

public class BusyWaitCyclicBarrierTest {

	@Test
	public void testAwait() {
		Thread[] t = new Thread[1];
		final BusyWaitCyclicBarrier b1 = new BusyWaitCyclicBarrier(t.length + 1);
		final BusyWaitCyclicBarrier b2 = new BusyWaitCyclicBarrier(t.length + 1);
		for (int i = 0; i < t.length; i++) {
			final int j = i;
			t[i] = new Thread(new Runnable() {
				@Override
				public void run() {
					while (!Thread.interrupted()) {
						try {
							b1.await();
							System.out.print(j);
							b2.await();
						} catch (InterruptedException e) {
							e.printStackTrace();
						} catch (BrokenBarrierException e) {
							e.printStackTrace();
						}
					}
				}
			});
		}

		for (int i = 0; i < t.length; i++) {
			t[i].start();
		}
		for (int i = 0; i < 100; i++) {
			try {
				b1.await();
				b2.await();
			} catch (InterruptedException e) {
				e.printStackTrace();
			} catch (BrokenBarrierException e) {
				e.printStackTrace();
			}
			System.out.println();
		}
		for (int i = 0; i < t.length; i++) {
			t[i].interrupt();
		}
	}
}
