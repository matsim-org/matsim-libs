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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.core.gbl.Gbl;

import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CyclicBarrier;

/**
 * This class checks if two events files are semantic equivalent. The order of the events does not matter as long as
 * they are chronologically sorted.
 *
 * @author mrieser
 * @author laemmel
 */
public final class EventsFileComparator {
	private static final Logger log = LogManager.getLogger(EventsFileComparator.class);

	private boolean ignoringCoordinates = false;
	public EventsFileComparator setIgnoringCoordinates( boolean ignoringCoordinates ){
		this.ignoringCoordinates = ignoringCoordinates;
		return this;
	}

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
	 * @param filename1 name of the first event file
	 * @param filename2 name of the second event file
	 * @return <code>Result.FILES_ARE_EQUAL</code> if the events files are equal, or some error code (see {@link ComparisonResult}) if not.
	 */
	public static ComparisonResult compare(final String filename1, final String filename2) {
		return new EventsFileComparator().runComparison( filename1, filename2 );
	}

	public ComparisonResult runComparison(final String filename1, final String filename2 ) {
		// (need method name different from pre-existing static method.  kai, feb'20)

		EventsComparator comparator = new EventsComparator( );
		CyclicBarrier doComparison = new CyclicBarrier(2, comparator);
		Worker w1 = new Worker(filename1, doComparison, ignoringCoordinates );
		Worker w2 = new Worker(filename2, doComparison, ignoringCoordinates );
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

		ComparisonResult retCode = comparator.retCode;
		if (retCode == ComparisonResult.FILES_ARE_EQUAL) {
			log.info("Event files are semantically equivalent.");
		} else {
			log.warn("Event files differ.");
		}
		return retCode;
	}

	private static class EventsComparator implements Runnable {

		private Worker worker1 = null;
		private Worker worker2 = null;
		private volatile ComparisonResult retCode = null ;

		/*package*/ void setWorkers(final Worker w1, final Worker w2) {
			this.worker1 = w1;
			this.worker2 = w2;
		}

		@Override
		public void run() {
			if (this.worker1.getCurrentTime() != this.worker2.getCurrentTime()) {
				log.warn("Differnt time steps in event files!");
				setExitCode(ComparisonResult.DIFFERENT_TIMESTEPS);
				return;
			}

			if (this.worker1.isFinished() != this.worker2.isFinished()) {
				log.warn("Events files have different number of time steps!");
				setExitCode(ComparisonResult.DIFFERENT_NUMBER_OF_TIMESTEPS);
				return;
			}

			Map<String, Counter> map1 = this.worker1.getEventsMap();
			Map<String, Counter> map2 = this.worker2.getEventsMap();

			boolean problem = false ;

			int logCounter = 0;

			// check that map2 contains all keys of map1, with the same values
			for (Entry<String, Counter> entry : map1.entrySet()) {

				Counter counter = map2.get(entry.getKey());
				if (counter == null) {
					if (logCounter < 50) {
						logCounter++;
						log.warn("The event:");
						log.warn(entry.getKey());
						log.warn("is missing in events file:" + worker2.getEventsFile());
						setExitCode(ComparisonResult.MISSING_EVENT);
						problem = true;
						if (logCounter == 50) {
							log.warn(Gbl.FUTURE_SUPPRESSED);
						}
					}
				} else{
					if( counter.getCount() != entry.getValue().getCount() ){
						log.warn(
							  "Wrong event count for: " + entry.getKey() + "\n" + entry.getValue().getCount() + " times in file:" + worker1.getEventsFile()
								    + "\n" + counter.getCount() + " times in file:" + worker2.getEventsFile() );
						setExitCode( ComparisonResult.WRONG_EVENT_COUNT );
						problem = true;
					}
				}
			}

			logCounter = 0;
			// also check that map1 contains all keys of map2
			for (Entry<String, Counter> e : map2.entrySet()) {
				Counter counter = map1.get(e.getKey());
				if (counter == null) {
					if (logCounter < 50) {
						logCounter++;
						log.warn("The event:");
						log.warn(e.getKey());
						log.warn("is missing in events file:" + worker1.getEventsFile());
						setExitCode(ComparisonResult.MISSING_EVENT);
						problem = true;
						if (logCounter == 50) {
							log.warn(Gbl.FUTURE_SUPPRESSED);
						}
					}
				}
			}

			if ( problem ) {
				return ;
			}

			if (this.worker1.isFinished()) {
				setExitCode(ComparisonResult.FILES_ARE_EQUAL);
			}
		}

		private void setExitCode(final ComparisonResult errCode) {
			this.retCode= errCode;
			if (errCode != ComparisonResult.FILES_ARE_EQUAL) {
				this.worker1.interrupt();
				this.worker2.interrupt();
			}
		}
	}

	/**
	 * Don't use this enum. See deprecation message.
	 * @deprecated Use {@link ComparisonResult} instead. This enum is not used anymore and empty now.
	 */
	@Deprecated
	public enum Result {
	}

}
