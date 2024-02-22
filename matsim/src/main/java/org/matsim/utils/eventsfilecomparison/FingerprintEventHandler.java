package org.matsim.utils.eventsfilecomparison;

import it.unimi.dsi.fastutil.Hash;
import org.matsim.api.core.v01.events.Event;
import org.matsim.core.events.handler.BasicEventHandler;

import java.io.*;
import java.security.MessageDigest;
import java.util.*;

public class FingerprintEventHandler implements BasicEventHandler {

	public boolean ignoringCoordinates = false;

	public static byte header = 1;
	public static byte version = 1;
	public final EventFingerprint eventFingerprint = new EventFingerprint();


	public static class EventFingerprint {
		List<Float> timeArray = new ArrayList<>();
		public Map<String, Integer> eventTypeCounter = new HashMap<>();
		Integer stringHash;

		public EventFingerprint(List<Float> timeArray, Map<String, Integer> eventTypeCounter, Integer stringHash) {
			this.timeArray = timeArray;
			this.eventTypeCounter = eventTypeCounter;
			this.stringHash = stringHash;
		}

		public EventFingerprint() {
			this.timeArray = timeArray;
			this.eventTypeCounter = eventTypeCounter;
			Hash stringHash;
		}

		public List<Float> getTimeArray() {
			return timeArray;
		}

		public Map<String, Integer> getEventTypeCounter() {
			return eventTypeCounter;
		}

		public int getStringHash() {
			return stringHash;
		}

		public void addHashCode(String stringToAdd) {
			if (stringToAdd == null) {
				return;
			}

			int hashToAdd = stringToAdd.hashCode();
			if (stringHash == null) {
				stringHash = hashToAdd;
			} else {
				stringHash += hashToAdd;
			}
		}

		public void printFingerprint(FingerprintEventHandler.EventFingerprint fingerprint) {
			System.out.println("Time Array:");
			var i = 0;
			for (Float value : fingerprint.getTimeArray()) {
				i++;
				if(i % 100000 == 0)
					System.out.println(value);
			}

			System.out.println("Event Type Counter:");
			for (Map.Entry<String, Integer> entry : fingerprint.getEventTypeCounter().entrySet()) {
				System.out.println(entry.getKey() + ": " + entry.getValue());
			}

			System.out.println("String Hash: " + fingerprint.getStringHash());
		}
	}

	public void addEventType(String str) {
		// Increment the count for the given string
		Map<String, Integer> eventTypeCounter = eventFingerprint.getEventTypeCounter();
		eventTypeCounter.put(str, eventTypeCounter.getOrDefault(str, 0) + 1);
	}

	public int getEventCount(String str) {
		// Get the count for the given string
		return eventFingerprint.getEventTypeCounter().getOrDefault(str, 0);
	}

	@Override
	public void handleEvent(Event event) {

		addEventType(event.getEventType());
		eventFingerprint.getTimeArray().add((float) event.getTime());
		String lexicographicSortedString = toLexicographicSortedString(event);
		eventFingerprint.addHashCode(lexicographicSortedString);


	}

	private String toLexicographicSortedString(Event event) {
		List<String> strings = new ArrayList<String>();
		for (Map.Entry<String, String> e : event.getAttributes().entrySet()) {
			StringBuilder tmp = new StringBuilder();
			final String key = e.getKey();

			// dont look at cerftain attributes
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


	public static void writeEventFingerprintToFile(String filePath, EventFingerprint eventFingerprint) {
		try (DataOutputStream dataOutputStream = new DataOutputStream(new FileOutputStream(filePath))) {
			// Write header and version
			dataOutputStream.writeByte(header);
			dataOutputStream.writeByte(version);

			// Write time array size and elements
			dataOutputStream.writeInt(eventFingerprint.getTimeArray().size());
			for (float time : eventFingerprint.getTimeArray()) {
				dataOutputStream.writeFloat(time);
			}

			// Write event type counter map size and elements
			dataOutputStream.writeInt(eventFingerprint.getEventTypeCounter().size());
			for (Map.Entry<String, Integer> entry : eventFingerprint.getEventTypeCounter().entrySet()) {
				dataOutputStream.writeUTF(entry.getKey());
				dataOutputStream.writeInt(entry.getValue());
			}

			// Write string hash
			dataOutputStream.writeInt(eventFingerprint.getStringHash());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static EventFingerprint readEventFingerprintFromFile(String fingerprintPath) {
		EventFingerprint eventFingerprint = null;

		try (DataInputStream dataInputStream = new DataInputStream(new FileInputStream(fingerprintPath))) {
			// Read header and version
			byte fileHeader = dataInputStream.readByte();
			byte fileVersion = dataInputStream.readByte();

			// Read time array
			int timeArraySize = dataInputStream.readInt();
			List<Float> timeArray = new ArrayList<>();
			for (int i = 0; i < timeArraySize; i++) {
				timeArray.add(dataInputStream.readFloat());
			}

			// Read event type counter map
			int eventTypeCounterSize = dataInputStream.readInt();
			Map<String, Integer> eventTypeCounter = new HashMap<>();
			for (int i = 0; i < eventTypeCounterSize; i++) {
				String eventType = dataInputStream.readUTF();
				int count = dataInputStream.readInt();
				eventTypeCounter.put(eventType, count);
			}

			// Read string hash
			int stringHash = dataInputStream.readInt();

			// Create EventFingerprint object
			eventFingerprint = new EventFingerprint(timeArray, eventTypeCounter, stringHash);
		} catch (IOException e) {
			e.printStackTrace();
		}

		return eventFingerprint;
	}
}
