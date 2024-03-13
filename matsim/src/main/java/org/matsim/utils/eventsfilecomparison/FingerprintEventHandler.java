package org.matsim.utils.eventsfilecomparison;

import it.unimi.dsi.fastutil.floats.FloatListIterator;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import org.matsim.api.core.v01.events.Event;
import org.matsim.core.events.handler.BasicEventHandler;

import java.util.*;

/**
 * Handler for creating and comparing {@link EventFingerprint}.
 */
public final class FingerprintEventHandler implements BasicEventHandler {

	/**
	 * Generated finger print.
	 */
	private final EventFingerprint eventFingerprint = new EventFingerprint();

	/**
	 * Precision for timestamp comparison.
	 */
	private static final float EPS = 1e-8f;

	/**
	 * Accumulate all event strings for same timestamps.
	 */
	private final List<String> hashAccumulationList = new ArrayList<>();

	/**
	 * Existing fingerprint for comparison against event file. Can be null, then no comparison is performed.
	 */
	private final EventFingerprint compareFingerprint;

	private FloatListIterator iterator = null;

	/**
	 * Result of the comparison.
	 */
	private ComparisonResult comparisonResult;
	private String comparisonMessage;

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

	public ComparisonResult getComparisonResult() {
		return comparisonResult;
	}

	void setComparisonResult(ComparisonResult comparisonResult) {
		this.comparisonResult = comparisonResult;
	}

	public String getComparisonMessage() {
		return comparisonMessage;
	}

	void setComparisonMessage(String comparisonMessage) {
		this.comparisonMessage = comparisonMessage;
	}

	@Override
	public void handleEvent(Event event) {


		String lexicographicSortedString = toLexicographicSortedString(event);

		if (compareFingerprint != null) {
			if (iterator == null) {
				this.iterator = compareFingerprint.timeArray.iterator();
			}

			if (this.comparisonResult == null) {
				if (iterator.hasNext()) {
					float entry = iterator.nextFloat();
					//Comparing floats with precision
					if (Math.abs((float) event.getTime() - entry) >= EPS) {
						this.comparisonResult = ComparisonResult.DIFFERENT_TIMESTEPS;
						this.comparisonMessage = "Difference occurred in this event time=" + event.getTime() + lexicographicSortedString;
					}
				} else {
					this.comparisonResult = ComparisonResult.DIFFERENT_TIMESTEPS;
					this.comparisonMessage = "Additional event time=" + event.getTime() + lexicographicSortedString;
				}
			}
		}

		eventFingerprint.addEventType(event.getEventType());


		//First timestep, nothing to accumulate
		if (eventFingerprint.timeArray.isEmpty()) {
			hashAccumulationList.add(lexicographicSortedString);
		} else {
			float lastTime = eventFingerprint.timeArray.getFloat(eventFingerprint.timeArray.size() - 1);
			//If new time is the same as previous, add to accumulation list, event hash calculation is not ready
			if (lastTime == event.getTime()) {
				hashAccumulationList.add(lexicographicSortedString);
			}
			//if new time differs from previous, all hash can be calculated
			else {
				accumulateHash();
				hashAccumulationList.add(lexicographicSortedString);
			}
		}

		eventFingerprint.addTimeStamp(event.getTime());

		//eventFingerprint.addHashCode(lexicographicSortedString);
	}

	private void accumulateHash() {

		Collections.sort(hashAccumulationList);

		for (String str : hashAccumulationList) {
			eventFingerprint.addHashCode(str);
		}

		hashAccumulationList.clear();
	}

	/**
	 * <p>
	 * Finish processing of the events file and return comparison result (if compare fingerprint was present).
	 * If the result is not equal it will generate a {@link #comparisonMessage}.
	 */
	void finishProcessing() {

		if (!hashAccumulationList.isEmpty()) {
			accumulateHash();
		}

		byte[] hash = eventFingerprint.computeHash();

		//hash = eventFingerprint.computeHash();

		if (compareFingerprint == null)
			return;

		//Handling EventTypeCounter differences
		for (Object2IntMap.Entry<String> entry1 : compareFingerprint.eventTypeCounter.object2IntEntrySet()) {
			String key = entry1.getKey();
			int count1 = entry1.getIntValue();
			int count2 = eventFingerprint.eventTypeCounter.getInt(key);
			if (count1 != count2) {
				comparisonMessage = comparisonMessage == null ? "" : comparisonMessage;

				comparisonResult = (comparisonResult == null ? ComparisonResult.WRONG_EVENT_COUNT : comparisonResult);

				if (!comparisonMessage.isEmpty())
					comparisonMessage += "\n";

				comparisonMessage += "Count for event type '%s' differs: %d (in fingerprint) != %d (in events)".formatted(key, count1, count2);
			}
		}

		//  Difference was found in {@link EventFingerprint#eventTypeCounter}
		if (comparisonResult != null) {
			return;
		}

		// only check hash if there was no difference up until here
		if (!Arrays.equals(hash, compareFingerprint.hash)) {
			comparisonResult = ComparisonResult.DIFFERENT_EVENT_ATTRIBUTES;
			comparisonMessage = "Difference occurred in this hash of 2 files";
			return;
		}

		comparisonResult = ComparisonResult.FILES_ARE_EQUAL;
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
