/* *********************************************************************** *
 * project: org.matsim.*
 * TryPerformanceEventQueues.java
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
package playground.thibautd.scripts;

import org.matsim.core.mobsim.jdeqsim.util.Timer;
import playground.ivt.utils.ArgParser;
import playground.thibautd.utils.ConcurrentListSPSC;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Similar to what Rashid did for his ConcurrentListSPSC, but including 
 * Blocking queues.
 * @author thibautd
 */
public class TryPerformanceEventQueues {
	// this experiment effectivly demonstrates, why reimplement
	// ConcurrentLinkedQueue
	// ------------
	// consumed Items:10000
	// time required for ConcurrentList (consumer): 27031
	// ----------
	// consumed Items:10000
	// time required for ConcurrentLinkedQueue (consumer): 60953
	// ----------
	// This experiment was done with adding 10000000 elements but only 10000
	// consumed
	// It shows, that ConcurrentList much better decouples the producer from the
	// consumer
	// especially, when the consumer is slower than the producer (which was
	// simulated by the sleep(1)

	static enum Test { spscList , concurrentList , blockingQueue };
	public static void main(String[] args) {

		final ArgParser p = new ArgParser( );
		p.setDefaultValue( "-t" , null );

		switch ( p.parseArgs( args ).getEnumValue( "-t" , Test.class ) ) {
			case spscList: {
			   // time required for ConcurrentList (producer): 3989
			   // consumed Items:10000
			   // time required for ConcurrentList (consumer): 15126
				ConcurrentListSPSC<Integer> cList = new ConcurrentListSPSC<Integer>();
				Thread t = new Thread(new ARunnable(cList));
				t.start();
				t = new Thread(new BRunnable(cList));
				t.start();
				break;
			}
			case concurrentList: {
				// time required for ConcurrentLinkedQueue (producer): 5208
				// consumed Items:10000
				// time required for ConcurrentLinkedQueue (consumer): 16392
				//
				ConcurrentLinkedQueue<Integer> cList = new ConcurrentLinkedQueue<Integer>();
				Thread t = new Thread(new QueueProducerRunnable(cList));
				t.start();
				t = new Thread(new QueueConsummerRunnable(cList));
				t.start();
				break;
			}
			case blockingQueue: {
				// time required for LinkedBlockingQueue (producer): 5314
				// consumed Items:10000
				// time required for LinkedBlockingQueue (consumer): 16428
				Queue<Integer> cList = new LinkedBlockingQueue<Integer>();
				Thread t = new Thread(new QueueProducerRunnable(cList));
				t.start();
				t = new Thread(new QueueConsummerRunnable(cList));
				t.start();
				break;
			}
			default:
				break;
		}
	}

	static class ARunnable implements Runnable {
		final ConcurrentListSPSC<Integer> cList;

		@Override
		public void run() {
			Timer timer = new Timer();
			timer.startTimer();
			for (int i = 0; i < 10000000; i++) {
				cList.add(i);
			}
			timer.endTimer();
			timer
					.printMeasuredTime("time required for ConcurrentList (producer): ");
		}

		public ARunnable(final ConcurrentListSPSC<Integer> cList) {
			this.cList = cList;
		}
	}

	static class BRunnable implements Runnable {
		final ConcurrentListSPSC<Integer> cList;

		@Override
		public void run() {
			Timer timer = new Timer();
			timer.startTimer();
			int count = 0;
			while (count < 10000) {
				if (cList.remove() != null) {
					count++;
				}
				try {
					Thread.sleep(1);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			System.out.println("consumed Items:" + count);
			timer.endTimer();
			timer
					.printMeasuredTime("time required for ConcurrentList (consumer): ");
		}

		public BRunnable(final ConcurrentListSPSC<Integer> cList) {
			this.cList = cList;
		}
	}

	static class QueueProducerRunnable implements Runnable {
		final Queue<Integer> cList;

		@Override
		public void run() {
			Timer timer = new Timer();
			timer.startTimer();
			for (int i = 0; i < 10000000; i++) {
				cList.add(i);
			}
			timer.endTimer();
			timer
					.printMeasuredTime("time required for "+cList.getClass().getSimpleName()+" (producer): ");
		}

		public QueueProducerRunnable(final Queue<Integer> cList) {
			this.cList = cList;
		}
	}

	static class QueueConsummerRunnable implements Runnable {
		final Queue<Integer> cList;

		@Override
		public void run() {
			Timer timer = new Timer();
			timer.startTimer();
			int count = 0;
			while (count < 10000) {
				if (cList.poll() != null) {
					count++;
					try {
						Thread.sleep(1);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
			System.out.println("consumed Items:" + count);
			timer.endTimer();
			timer
					.printMeasuredTime("time required for "+cList.getClass().getSimpleName()+" (consumer): ");
		}

		public QueueConsummerRunnable(final Queue<Integer> cList) {
			this.cList = cList;
		}
	}

}
