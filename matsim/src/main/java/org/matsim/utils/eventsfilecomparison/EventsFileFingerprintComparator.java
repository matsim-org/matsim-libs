package org.matsim.utils.eventsfilecomparison;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;

import jakarta.annotation.Nullable;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Arrays;

/**
 * Utility class for comparing events and fingerprints.
 */
public final class EventsFileFingerprintComparator {

	private static final Logger log = LogManager.getLogger(EventsFileComparator.class);

	private EventsFileFingerprintComparator() {
	}


	/**
	 * Create and compare event fingerprints and return the handler holding resulting information.
	 */
	public static FingerprintEventHandler createFingerprintHandler(final String eventsfile, @Nullable String compareFingerprint) {

		EventFingerprint fp = null;
		Exception err = null;
		if (compareFingerprint != null) {
			try {
				fp = EventFingerprint.read(compareFingerprint);
			} catch (Exception e) {
				log.warn("Could not read compare fingerprint from file: {}", compareFingerprint, e);
				fp = new EventFingerprint();
				err = e;
			}
		}

		FingerprintEventHandler handler = new FingerprintEventHandler(fp);

		EventsManager manager = EventsUtils.createEventsManager();

		manager.addHandler(handler);

		EventsUtils.readEvents(manager, eventsfile);

		manager.finishProcessing();
		handler.finishProcessing();

		// File error overwrite any other error
		if (err != null) {
			handler.setComparisonResult(ComparisonResult.FILE_ERROR);
			handler.setComparisonMessage(err.getMessage());
		}

		return handler;
	}

	public static ComparisonResult compareFingerprints(final String fp1, final String fp2) {

        EventFingerprint fingerprint1;
		EventFingerprint fingerprint2;
        try {
            fingerprint1 = EventFingerprint.read(fp1);
			fingerprint2 = EventFingerprint.read(fp2);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        String logMessage = "";
		//Check if time array size is the same
		if (fingerprint1.timeArray.size() != fingerprint2.timeArray.size()) {
			logMessage = "Different number of timesteps";
			log.warn(logMessage);
			return ComparisonResult.DIFFERENT_NUMBER_OF_TIMESTEPS;
		}

		//Check if both time arrays have the same timesteps
		if (!Arrays.equals(fingerprint1.timeArray.toFloatArray(), fingerprint2.timeArray.toFloatArray())) {
			logMessage = "Different timesteps";
			log.warn(logMessage);
			return ComparisonResult.DIFFERENT_TIMESTEPS;
		}


		//Check which event type counts are different among 2 fingerprints
		boolean countDiffers = false;
		for (Object2IntMap.Entry<String> entry1 : fingerprint1.eventTypeCounter.object2IntEntrySet()) {
			String key = entry1.getKey();
			int count1 = entry1.getIntValue();
			int count2 = fingerprint2.eventTypeCounter.getInt(key);
			if (count1 != count2) {
				countDiffers = true;

				if (!logMessage.isEmpty())
					logMessage += "\n";

				logMessage += "Count for event type '%s' differs: %d != %d".formatted(key, count1, count2);
			}
		}
		if (countDiffers) {
			log.warn(logMessage);
			return ComparisonResult.WRONG_EVENT_COUNT;
		}


		//Check if total hash is the same
		byte[] hash1 = fingerprint1.hash;
		byte[] hash2 = fingerprint2.hash;
		if (!Arrays.equals(hash1, hash2)) {

			logMessage = String.format("Difference occurred hash codes hash of first file is %s, hash of second is %s", Arrays.toString(hash1), Arrays.toString(hash2));

			log.warn(logMessage);
			return ComparisonResult.DIFFERENT_EVENT_ATTRIBUTES;
		}

		return ComparisonResult.FILES_ARE_EQUAL;

	}
}
