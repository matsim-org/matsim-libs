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

package org.matsim.utils.eventsfilecomparison;

import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CyclicBarrier;

import org.apache.log4j.Logger;

/**
 * This class checks if two events files are semantic equivalent. The order of the events does not matter as long as
 * they are chronologically sorted.
 *
 * @author mrieser
 * @author laemmel
 */
public abstract class EventsFileComparator {

	private static final Logger log = Logger.getLogger(EventsFileComparator.class);

	public static final int CODE_FILES_ARE_EQUAL = 0;
	public static final int CODE_DIFFERENT_NUMBER_OF_TIMESTEPS = -1;
	public static final int CODE_DIFFERENT_TIMESTEPS = -2;
	public static final int CODE_MISSING_EVENT = -3;
	public static final int CODE_WRONG_EVENT_COUNT = -4;

	public static void main(String[] args) {
		if (args.length != 2) {
			System.out.println("Error: expected 2 events files as input arguments but found " + args.length);
			System.out.println("Syntax: EventsFileComparator eventsFile1 eventsFile2");
		} else {
			String filename1 = args[0];
			String filename2 = args[1];
			
			EventsFileComparator.compare(filename1, filename2);			
		}
	}
	
	/**
	 * Compares two Events files. This method is thread-safe.
	 *
	 * @param filename1
	 * @param filename2
	 * @return <code>0</code> if the events files are equal, or some error code (see constants) if not.
	 */
	public static int compare(final String filename1, final String filename2) {
		EventsComparator comparator = new EventsComparator();
		CyclicBarrier doComparison = new CyclicBarrier(2, comparator);
		Worker w1 = new Worker(filename1, doComparison);
		Worker w2 = new Worker(filename2, doComparison);
		comparator.setWorkers(w1, w2);
		w1.start();
		w2.start();

		try {
			w1.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		try {
			w2.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		int retCode = comparator.retCode;
		if (retCode == CODE_FILES_ARE_EQUAL) {
			log.info("Event files are semantic equivalent.");
		} else {
			log.warn("Event files differ.");
		}
		return retCode;
	}

	/*package*/ static class EventsComparator implements Runnable {

		private Worker worker1 = null;
		private Worker worker2 = null;
		/*package*/ volatile int retCode = -99;

		/*package*/ void setWorkers(final Worker w1, final Worker w2) {
			this.worker1 = w1;
			this.worker2 = w2;
		}

		@Override
		public void run() {
			if (this.worker1.getCurrentTime() != this.worker2.getCurrentTime()) {
				log.warn("Differnt time steps in event files! Aborting!");
				setExitCode(CODE_DIFFERENT_TIMESTEPS);
				return;
			}

			if (this.worker1.isFinished() != this.worker2.isFinished()) {
				log.warn("Events files have different number of time steps! Aborting!");
				setExitCode(CODE_DIFFERENT_NUMBER_OF_TIMESTEPS);
				return;
			}

			Map<String, Counter> map1 = this.worker1.getEventsMap();
			Map<String, Counter> map2 = this.worker2.getEventsMap();

			// check that map2 contains all keys of map1, with the same values
			for (Entry<String, Counter> e : map1.entrySet()) {
				Counter c = map2.get(e.getKey());
				if (c == null) {
					log.warn("Missing event:" ) ;
					log.warn( e.getKey() );
					log.warn("in events file:" + worker2.getEventsFile());
					setExitCode(CODE_MISSING_EVENT);
					return;
				}
				if (c.getCount() != e.getValue().getCount()) {
					log.warn("Wrong event count for: " + e.getKey() + "\n" + e.getValue().getCount() + " times in file:" + worker1.getEventsFile()
							+ "\n" + c.getCount() + " times in file:" + worker2.getEventsFile());
					setExitCode(CODE_WRONG_EVENT_COUNT);
					return;
				}
			}

			// also check that map1 contains all keys of map2
			for (Entry<String, Counter> e : map2.entrySet()) {
				Counter c = map1.get(e.getKey());
				if (c == null) {
					log.warn("Missing event:" + e.getKey() + "\nin events file:" + worker1.getEventsFile());
					setExitCode(CODE_MISSING_EVENT);
					return;
				}
			}

			if (this.worker1.isFinished()) {
				setExitCode(CODE_FILES_ARE_EQUAL);
			}
		}

		private void setExitCode(final int errCode) {
			this.retCode= errCode;
			if (errCode != CODE_FILES_ARE_EQUAL) {
				this.worker1.interrupt();
				this.worker2.interrupt();
			}
		}
	}

}
