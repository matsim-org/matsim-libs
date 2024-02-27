package org.matsim.utils.eventsfilecomparison;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;

import java.util.*;

public class EventsFileFingerprintComparator {

	private static final Logger log = LogManager.getLogger(EventsFileComparator.class);


	public static ComparisonResult compare(final String fingerprint, final String eventsfile) {

		EventFingerprint correctFingerprint = EventFingerprint.read(fingerprint);

		FingerprintEventHandler handler = new FingerprintEventHandler(correctFingerprint);
		EventsManager manager = EventsUtils.createEventsManager();

		manager.addHandler(handler);

		EventsUtils.readEvents(manager, eventsfile);

		manager.finishProcessing();

		ComparisonResult result = handler.finishProcessing();

		log.warn(handler.comparisonMessage);

		// All fields are equal
		return result;
	}

	public static ComparisonResult compareFingerprints(final String fp1, final String fp2) {


		EventFingerprint fingerprint1 = EventFingerprint.read(fp1);
		EventFingerprint fingerprint2 = EventFingerprint.read(fp2);

		String log_message = "";
		//Check if time array size is the same
		if(fingerprint1.timeArray.size()!=fingerprint2.timeArray.size()){
			log_message = "Different number of timesteps";
			log.warn(log_message);
			return ComparisonResult.DIFFERENT_NUMBER_OF_TIMESTEPS;
		}

		//Check if both time arrays have the same timesteps
		if(!Arrays.equals(fingerprint1.timeArray.toFloatArray(),fingerprint2.timeArray.toFloatArray())){
			log_message = "Different timesteps";
			log.warn(log_message);
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
				log_message = log_message + ("\r\nCount for key '" + key + "' differs: " + count1 + " != " + count2);
			}
		}
		if(countDiffers){
			log.warn(log_message);
			return ComparisonResult.WRONG_EVENT_COUNT;
		}


		//Check if total hash is the same
		byte[] hash1 = fingerprint1.hash;
		byte[] hash2 = fingerprint2.hash;
		if(!Arrays.equals(hash1, hash2)){

			log_message = String.format("Difference occurred hash codes hash of first file is %s, hash of second is %s", Arrays.toString(hash1), Arrays.toString(hash2));

			log.warn(log_message);
			return ComparisonResult.MISSING_EVENT;
		}

		log_message = "Files are equal";
		log.warn(log_message);

		// TODO: compare and generate messages
		return ComparisonResult.FILES_ARE_EQUAL;

	}
}
