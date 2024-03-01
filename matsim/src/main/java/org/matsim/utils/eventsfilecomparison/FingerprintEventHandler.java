package org.matsim.utils.eventsfilecomparison;

import it.unimi.dsi.fastutil.floats.FloatListIterator;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import org.matsim.api.core.v01.events.Event;
import org.matsim.core.events.handler.BasicEventHandler;
import org.matsim.counts.algorithms.graphs.helper.Comp;

import java.util.*;

public final class FingerprintEventHandler implements BasicEventHandler {


	/**
	 * Generated finger print.
	 */
	public final EventFingerprint eventFingerprint = new EventFingerprint();

	/**
	 * Existing fingerprint for comparison against event file. Can be null, then no comparison is performed.
	 */
	public final EventFingerprint compareFingerprint;

	FloatListIterator iterator = null;


	/**
	 * Result of the comparison.
	 */
	public ComparisonResult comparisonResult;
	public String comparisonMessage;

	public FingerprintEventHandler() {
		this.compareFingerprint = null;
	}

	public FingerprintEventHandler(EventFingerprint compareFingerprint) {
		this.compareFingerprint = compareFingerprint;
		this.comparisonResult = null;
	}

	public EventFingerprint getEventFingerprint() {
		return eventFingerprint;
	}

	@Override
	public void handleEvent(Event event) {
		eventFingerprint.addTimeStamp(event.getTime());

		String lexicographicSortedString = toLexicographicSortedString(event);



		if (compareFingerprint != null) {
			if(iterator== null){
				this.iterator = compareFingerprint.timeArray.iterator();
			}
			if (this.comparisonResult == null) {
				if(iterator.hasNext()){
					Float entry = iterator.nextFloat();
					if((float) event.getTime()!=entry){
						this.comparisonResult = ComparisonResult.DIFFERENT_TIMESTEPS;
						this.comparisonMessage = "Difference occurred in this event "+lexicographicSortedString;
					}
				}
			}
		}

		eventFingerprint.addEventType(event.getEventType());

		eventFingerprint.addHashCode(lexicographicSortedString);
	}

	/**
	 * <p>
	 * Finish processing of the events file and return comparison result (if compare fingerprint was present).
	 * If the result is not equal it will generate a {@link #comparisonMessage}.
	 */
	ComparisonResult finishProcessing() {

		byte[] hash = eventFingerprint.computeHash();

		/** Difference was found in {@link EventFingerprint#timeArray}
		 *
		 */
		if (comparisonResult != null) {
			return comparisonResult;
		}

		if (compareFingerprint == null)
			return null;

		//Handling EventTypeCounter differences
		for (Object2IntMap.Entry<String> entry1 : compareFingerprint.eventTypeCounter.object2IntEntrySet()) {
			String key = entry1.getKey();
			int count1 = entry1.getIntValue();
			int count2 = eventFingerprint.eventTypeCounter.getInt(key);
			if (count1 != count2) {
				comparisonResult = (comparisonResult == null ? ComparisonResult.WRONG_EVENT_COUNT : comparisonResult);
				comparisonMessage = (comparisonMessage == null ? "" : comparisonMessage) + ("\r\nCount for key '" + key + "' differs: " + count1 + " != " + count2);
			}
		}

		/** Difference was found in {@link EventFingerprint#eventTypeCounter}
		 *
		 */
		if (comparisonResult != null) {
			return comparisonResult;
		}


		if (!Arrays.equals(hash, compareFingerprint.hash)) {
			comparisonResult = ComparisonResult.DIFFERENT_EVENT_ATTRIBUTES;
			comparisonMessage = "Difference occured in this hash of 2 files";

			/** Difference was found in {@link EventFingerprint#hash}
			 *
			 */
			return comparisonResult;
		}


		if (comparisonResult == null) {
			comparisonResult = ComparisonResult.FILES_ARE_EQUAL;
			comparisonMessage = "Seems to be equal :)";
		}

		//log.warn(comparisonMessage);

		// TODO: compare and generate messages
		return comparisonResult;

	}

	private String toLexicographicSortedString(Event event) {
		List<String> strings = new ArrayList<String>();
		for (Map.Entry<String, String> e : event.getAttributes().entrySet()) {
			StringBuilder tmp = new StringBuilder();
			final String key = e.getKey();

			// don't look at certain attributes
			switch (key) {
				case Event.ATTRIBUTE_X:
				case Event.ATTRIBUTE_Y:
				case Event.ATTRIBUTE_TIME:
					continue;
			}

			tmp.append(key);
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

		eventStr.append(" | ");
		return eventStr.toString();
	}
}
