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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.events.Event;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.events.SingleHandlerEventsManager;
import org.matsim.core.events.handler.BasicEventHandler;

class Worker extends Thread implements BasicEventHandler{

	private static final Logger log = LogManager.getLogger(Worker.class);

	private final EventsManager eventsManager;
	private final String eFile;
	private final CyclicBarrier doComparison;
	private final boolean ignoringCoordinates;

	private final Map<String,Counter> events = new HashMap<String,Counter>();

	private volatile double time = -1;
	private volatile boolean finished = false;
	private volatile int numEvents = 0;

	Worker( String eFile1, final CyclicBarrier doComparison, boolean ignoringCoordinates ) {
		this.eFile = eFile1;
		this.doComparison = doComparison;
		this.ignoringCoordinates = ignoringCoordinates;

		this.eventsManager = new SingleHandlerEventsManager(this);

	}

	/*package*/ String getEventsFile() {
		return this.eFile;
	}

	@Override
	public void run() {
		try {
			new MatsimEventsReader(this.eventsManager).readFile(this.eFile);
			this.finished = true;
			try {
				this.doComparison.await();

			} catch (InterruptedException e1) {
				throw new ComparatorInterruptedException(e1);
			} catch (BrokenBarrierException e1) {
				throw new ComparatorInterruptedException(e1);
			}
		} catch (ComparatorInterruptedException e1) {
//			log.info("events-comparator got interrupted", e1);
			log.info("events-comparator got interrupted");
		}
	}

	public boolean isFinished() {
		return this.finished;
	}

	public Map<String, Counter> getEventsMap() {
		return this.events;
	}

	public double getCurrentTime() {
		return this.time;
	}

	@Override
	public void reset(int iteration) {
	}

	@Override
	public void handleEvent(Event event) {
		if (this.time != event.getTime()) {
			try {
				doComparison.await();
			} catch (InterruptedException e1) {
				throw new ComparatorInterruptedException(e1);
			} catch (BrokenBarrierException e1) {
				throw new ComparatorInterruptedException(e1);
			}

			this.events.clear();
			this.time = event.getTime();
			this.numEvents = 0;
		}

		addEvent(event);
	}

	public int getNumEvents() {
		return this.numEvents;
	}

	private void addEvent(Event event) {
		this.numEvents++;
		String lexString = toLexicographicSortedString(event);
		Counter counter = this.events.get(lexString);
		if (counter == null) {
			counter = new Counter();
			this.events.put(lexString, counter);
		}
		counter.increment();
	}

	private String toLexicographicSortedString(Event event) {
		List<String> strings = new ArrayList<String>();
		for (Entry<String, String> e : event.getAttributes().entrySet()) {
			StringBuilder tmp = new StringBuilder();
			final String key = e.getKey();

			// don't look at coordinates if configured as such:
			if ( ignoringCoordinates ){
				switch( key ){
					case Event.ATTRIBUTE_X:
					case Event.ATTRIBUTE_Y:
					case Event.ATTRIBUTE_TIME:
						continue;
				}
			}

			tmp.append( key );
			tmp.append("=");
			tmp.append(e.getValue());
			strings.add(tmp.toString());
		}
		Collections.sort(strings);
		StringBuilder eventStr = new StringBuilder();
		for (String str : strings) {
			eventStr.append(" | ");
			eventStr.append(str);
		}

		eventStr.append(" | ") ;
		return eventStr.toString();
	}

	/**
	 * An exception that signals that the comparison-thread was interrupted but which
	 * does not need to be declared, thus extending from RuntimeException.
	 *
	 * @author mrieser
	 */
	private static class ComparatorInterruptedException extends RuntimeException {
		public ComparatorInterruptedException(final Exception e) {
			super(e);
		}
	}

}
