
/* *********************************************************************** *
 * project: org.matsim.*
 * EventsUtils.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2019 by the members listed in the COPYING,        *
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

package org.matsim.core.events;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Injector;
import org.matsim.utils.eventsfilecomparison.*;

import java.io.File;

public final class EventsUtils {

	private static final Logger log = LogManager.getLogger(EventsUtils.class);


	/**
	 * Create a events manager instance that guarantees causality of processed events across all handlers.
	 */
	public static EventsManager createEventsManager() {
		return new EventsManagerImpl();
	}

	/**
	 * Creates a parallel events manager, with no guarantees for the order of processed events between multiple handlers.
	 */
	public static EventsManager createParallelEventsManager() {
		return new ParallelEventsManager(false);
	}

	public static EventsManager createEventsManager(Config config) {
		final EventsManager events = Injector.createInjector(config, new EventsManagerModule()).getInstance(EventsManager.class);
//		events.initProcessing();
		return events;
	}

	public static void readEvents(EventsManager events, String filename) {
		new MatsimEventsReader(events).readFile(filename);
	}


	/**
	 * The SimStepParallelEventsManagerImpl can handle events from multiple threads.
	 * The (Parallel)EventsMangerImpl cannot, therefore it has to be wrapped into a
	 * SynchronizedEventsManagerImpl.
	 */
	public static EventsManager getParallelFeedableInstance(EventsManager events) {
		if (events instanceof SimStepParallelEventsManagerImpl) {
			return events;
		} else if (events instanceof ParallelEventsManager) {
			return events;
		} else if (events instanceof SynchronizedEventsManagerImpl) {
			return events;
		} else {
			return new SynchronizedEventsManagerImpl(events);
		}
	}

	/**
	 * Create and write fingerprint file for events.
	 */
	public static FingerprintEventHandler createEventsFingerprint(String eventFile, String outputFingerprintFile) {

		FingerprintEventHandler handler = EventsFileFingerprintComparator.createFingerprintHandler(eventFile, null);

		EventFingerprint.write(outputFingerprintFile, handler.getEventFingerprint());

		return handler;
	}


	/**
	 * Compares existing event file against fingerprint file. This will also create new fingerprint file along the input events.
	 *
	 * @param eventFile local events file
	 * @param compareFingerprintFile path or uri to fingerprint file
	 *
	 * @return comparison results
	 */
	public static ComparisonResult createAndCompareEventsFingerprint(File eventFile, String compareFingerprintFile) {

		String path = eventFile.getPath().replaceFirst("\\.xml[.a-zA-z0-9]*$", "");

		FingerprintEventHandler handler = EventsFileFingerprintComparator.createFingerprintHandler(eventFile.toString(), compareFingerprintFile);
		EventFingerprint.write(path + ".fp.zst", handler.getEventFingerprint());

		if (handler.getComparisonMessage() != null)
			log.warn(handler.getComparisonMessage());

		return handler.getComparisonResult();
	}

	/**
	 * Compares existing event file against fingerprint file. This will also create new fingerprint file along the input events.
	 *
	 * @throws AssertionError if comparison fails
	 * @see #createAndCompareEventsFingerprint(File, String)
	 */
	public static void assertEqualEventsFingerprint(File eventFile, String compareFingerprintFile) {

		String path = eventFile.getPath().replaceFirst("\\.xml[.a-zA-z0-9]*$", "");

		FingerprintEventHandler handler = EventsFileFingerprintComparator.createFingerprintHandler(eventFile.toString(), compareFingerprintFile);
		EventFingerprint.write(path + ".fp.zst", handler.getEventFingerprint());


		if (handler.getComparisonResult() != ComparisonResult.FILES_ARE_EQUAL)
			throw new AssertionError(handler.getComparisonMessage());

	}

	public static ComparisonResult compareEventsFiles(String filename1, String filename2) {
		return EventsFileComparator.compare(filename1, filename2);
	}

}
