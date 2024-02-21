package org.matsim.utils.eventsfilecomparison;

import it.unimi.dsi.fastutil.Hash;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.events.Event;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.handler.BasicEventHandler;

import java.io.*;
import java.util.*;
import java.util.concurrent.CyclicBarrier;

public class EventsFileFingerprintComparator {

	private static final Logger log = LogManager.getLogger(EventsFileComparator.class);

	public enum Result { FILES_ARE_EQUAL, DIFFERENT_NUMBER_OF_TIMESTEPS, DIFFERENT_TIMESTEPS, MISSING_EVENT, WRONG_EVENT_COUNT }

	public static Result compare(final String fingerprint, final String eventsfile) {
		FingerprintEventHandler handler = new FingerprintEventHandler();
		EventsManager manager = EventsUtils.createEventsManager();

		manager.addHandler(handler);

		EventsUtils.readEvents(manager,eventsfile);

		FingerprintEventHandler.EventFingerprint fingerprintFromEvents = handler.eventFingerprint;

		FingerprintEventHandler.EventFingerprint correctFingerprint = FingerprintEventHandler.readEventFingerprintFromFile(fingerprint);


		if (fingerprintFromEvents == fingerprintFromEvents) {
			return Result.FILES_ARE_EQUAL;
		}
		if (fingerprintFromEvents == null) {
			return Result.DIFFERENT_TIMESTEPS;
		}

		// Compare timeArray
		if (!Objects.equals(fingerprintFromEvents.timeArray, fingerprintFromEvents.timeArray)) {
			return Result.DIFFERENT_TIMESTEPS;
		}

		// Compare eventTypeCounter
		if (!Objects.equals(fingerprintFromEvents.eventTypeCounter, fingerprintFromEvents.eventTypeCounter)) {
			return Result.WRONG_EVENT_COUNT;
		}

		// Compare stringHash
		if (!Objects.equals(fingerprintFromEvents.stringHash, fingerprintFromEvents.stringHash)) {
			return Result.MISSING_EVENT;
		}

		// All fields are equal
		return Result.FILES_ARE_EQUAL;
	}


}

