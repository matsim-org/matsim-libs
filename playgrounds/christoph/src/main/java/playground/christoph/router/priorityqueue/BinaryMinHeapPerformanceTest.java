/* *********************************************************************** *
 * project: org.matsim.*
 * BinaryMinHeapTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.christoph.router.priorityqueue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import org.apache.commons.collections.iterators.ArrayIterator;
import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.matsim.core.gbl.MatsimRandom;

/**
 * @author muelleki
 */
public class BinaryMinHeapPerformanceTest extends MinHeapTest {
	private class TestThread extends Thread {
		static final int ITERS = 500000;
		static final int OUTDEGREE = 3;
		static final int DECREASE = 1;
		static final int MAXENTRIES = ITERS * OUTDEGREE;
		private final int id;
		private final int threads;

		TestThread(final int id, final int threads, final int fanout) {
			this.id = id;
			this.threads = threads;

			heap = new BinaryMinHeap<DummyHeapEntry>(MAXENTRIES, fanout);

			es = new DummyHeapEntry[MAXENTRIES];
			for (int gen = 0; gen < MAXENTRIES; gen++)
				es[gen] = new DummyHeapEntry(gen);
		}

		public long doTestDijkstraPerformance(BinaryMinHeap<DummyHeapEntry> pq,
				Iterator<DummyHeapEntry> it) {
			Random R = MatsimRandom.getLocalInstance();

			double cc = 0.0;
			pq.add(it.next(), cc);

			long t = System.nanoTime();
			for (int i = 1;; i++) {
				double c = pq.peekCost();
				assertTrue("Nondecreasing order for costs", c >= cc);
				cc = c;
				pq.remove();

				if (i < ITERS) {
					for (int j = 0; j < OUTDEGREE; j++) {
						pq.add(it.next(), c + R.nextDouble());
					}
				} else if (pq.isEmpty())
					break;

				for (int j = 0; j < DECREASE; j++) {
					final int index = R.nextInt(pq.size());
					DummyHeapEntry e = pq.peek(index);
					double co = pq.peekCost(index);
					double cn = cc + (co - cc) * R.nextDouble();
					pq.decreaseKey(e, cn);
				}
			}

			long tt = System.nanoTime();
			return tt - t;
		}

		private DescriptiveStatistics collectDijkstraPerformance() {
			int RUNS = 20;

			for (int i = 0; i < RUNS; i++) {
				@SuppressWarnings("unchecked")
				double dt = (doTestDijkstraPerformance(heap,
						((Iterator<DummyHeapEntry>) new ArrayIterator(es))) / 1.0e6);
				S.addValue(dt);
				log.info(String.format("Fanout: %d, Thread: %d/%d, Iteration: %d, Time: %f",
						heap.getFanout(), id, threads, i, dt));
			}
			return S;
		}

		public DescriptiveStatistics getStats() {
			return S;
		}

		private DescriptiveStatistics S = new DescriptiveStatistics();
		private BinaryMinHeap<DummyHeapEntry> heap;
		private DummyHeapEntry[] es;

		@Override
		public void run() {
			S = collectDijkstraPerformance();
		}
	}

	public Iterable<DescriptiveStatistics> collectThreadedDijkstraPerformance(
			int threads, int fanout) {
		List<TestThread> lt = new ArrayList<TestThread>(threads);
		for (int i = 0; i < threads; i++)
			lt.add(new TestThread(i, threads, fanout));

		System.gc();

		for (TestThread t : lt)
			t.start();

		List<DescriptiveStatistics> ls = new ArrayList<DescriptiveStatistics>(
				threads);
		try {
			for (TestThread t : lt) {
				t.join();
				ls.add(t.getStats());
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
			return null;
		}

		return ls;
	}

	private void doTestDijkstraPerformanceParam(final int threads,
			final int fanout) {
		for (DescriptiveStatistics S : collectThreadedDijkstraPerformance(threads, fanout)) {
			log.info(String
					.format("Time: Min/Max: %f/%f, Mean: %f, StDev: %f, 95%% CI: (%f, %f)",
							S.getMin(), S.getMax(), S.getMean(),
							S.getStandardDeviation(),
							(S.getMean() - 1.96 * S.getStandardDeviation()),
							(S.getMean() + 1.96 * S.getStandardDeviation())));
			log.info(Arrays.toString(S.getSortedValues()));
		}
	}

	public void testDijkstraPerformance() {
		final int threads = 2;
		final int fanout = 6;
		doTestDijkstraPerformanceParam(threads, fanout);
	}

	public static void main(String args[]) {
		int threads = Integer.parseInt(args[0]);
		int fanout = Integer.parseInt(args[1]);
		new BinaryMinHeapPerformanceTest().doTestDijkstraPerformanceParam(threads, fanout);
	}
}
