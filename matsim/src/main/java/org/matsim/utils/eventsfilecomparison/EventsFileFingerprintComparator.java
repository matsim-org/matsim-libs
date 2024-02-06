package org.matsim.utils.eventsfilecomparison;

import it.unimi.dsi.fastutil.Hash;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.events.Event;
import org.matsim.core.events.handler.BasicEventHandler;

import java.io.*;
import java.util.*;
import java.util.concurrent.CyclicBarrier;

public class EventsFileFingerprintComparator {

	private static final Logger log = LogManager.getLogger(EventsFileComparator.class);

	public enum Result { FILES_ARE_EQUAL, DIFFERENT_NUMBER_OF_TIMESTEPS, DIFFERENT_TIMESTEPS, MISSING_EVENT, WRONG_EVENT_COUNT }

	public static EventsFileFingerprintComparator.Result compare(final String filename1, final String filename2) {
		return null;
	}


}

