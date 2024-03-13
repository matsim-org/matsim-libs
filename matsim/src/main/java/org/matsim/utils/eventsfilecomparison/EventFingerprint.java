package org.matsim.utils.eventsfilecomparison;

import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.floats.FloatList;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import org.matsim.core.utils.io.IOUtils;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Map;

/**
 * Class holding reduced information about an events file.
 * If two such fingerprint are different, one can conclude that the event files are semantically different.
 * <p>
 * The fingerprint is based on the following information:
 * - Array of all timestamps
 * - Counts of each event type
 * - Hash of string concatenation of all event strings
 *
 * Note: Events with the same timestamp are allowed to occur in any order.
 */
public final class EventFingerprint {

	/**
	 * Header for version 1, FP/1
	 */
	static final int HEADER_V1 = 0x46502f31;

	final FloatList timeArray;
	final Object2IntMap<String> eventTypeCounter;
	final byte[] hash;

	/**
	 * Builder for the hash.
	 */
	private final MessageDigest digest;

	private EventFingerprint(FloatList timeArray, Object2IntMap<String> eventTypeCounter, byte[] hash) {
		this.timeArray = timeArray;
		this.eventTypeCounter = eventTypeCounter;
		this.hash = hash;
		this.digest = null;
	}

	EventFingerprint() {
		this.timeArray = new FloatArrayList();
		this.eventTypeCounter = new Object2IntOpenHashMap<>();
		this.hash = new byte[20];

		try {
			digest = MessageDigest.getInstance("SHA-1");
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("Hashing not supported;");
		}
	}

	public static void write(String filePath, EventFingerprint eventFingerprint) {
		try (DataOutputStream dataOutputStream = new DataOutputStream(IOUtils.getOutputStream(IOUtils.getFileUrl(filePath), false))) {
			// Write header and version
			dataOutputStream.writeInt(EventFingerprint.HEADER_V1);

			// Write time array size and elements
			dataOutputStream.writeInt(eventFingerprint.timeArray.size());
			for (float time : eventFingerprint.timeArray) {
				dataOutputStream.writeFloat(time);
			}

			// Write event type counter map size and elements
			dataOutputStream.writeInt(eventFingerprint.eventTypeCounter.size());
			for (Map.Entry<String, Integer> entry : eventFingerprint.eventTypeCounter.entrySet()) {
				dataOutputStream.writeUTF(entry.getKey());
				dataOutputStream.writeInt(entry.getValue());
			}

			// Hash should always be computed at this point
			assert !Arrays.equals(eventFingerprint.hash, new byte[20]) : "Hash was not computed";

			// Write byte hash
			dataOutputStream.write(eventFingerprint.hash);

		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	public static EventFingerprint read(String fingerprintPath) throws IOException {
		EventFingerprint eventFingerprint;

		try (DataInputStream dataInputStream = new DataInputStream(IOUtils.getInputStream(IOUtils.getFileUrl(fingerprintPath)))) {
			// Read header and version
			int fileHeader = dataInputStream.readInt();

			if (fileHeader != EventFingerprint.HEADER_V1) {
				throw new IllegalArgumentException("Invalid fingerprint file header");
			}

			// Read time array
			int timeArraySize = dataInputStream.readInt();
			FloatList timeArray = new FloatArrayList();
			for (int i = 0; i < timeArraySize; i++) {
				timeArray.add(dataInputStream.readFloat());
			}

			// Read event type counter map
			int eventTypeCounterSize = dataInputStream.readInt();
			Object2IntMap<String> eventTypeCounter = new Object2IntOpenHashMap<>();
			for (int i = 0; i < eventTypeCounterSize; i++) {
				String eventType = dataInputStream.readUTF();
				int count = dataInputStream.readInt();
				eventTypeCounter.put(eventType, count);
			}

			// Read string hash
			byte[] hash = dataInputStream.readNBytes(20);

			// Create EventFingerprint object
			eventFingerprint = new EventFingerprint(timeArray, eventTypeCounter, hash);
		}

		return eventFingerprint;
	}

	void addTimeStamp(double timestamp) {
		timeArray.add((float) timestamp);
	}

	void addEventType(String str) {
		// Increment the count for the given string
		eventTypeCounter.mergeInt(str, 1, Integer::sum);
	}

	void addHashCode(String stringToAdd) {
		if (stringToAdd == null) {
			return;
		}

		digest.update(stringToAdd.getBytes(StandardCharsets.UTF_8));
	}

	byte[] computeHash() {
		if (this.digest == null)
			throw new IllegalStateException("Hash was from from input and can not be computed");

		byte[] digest = this.digest.digest();
		System.arraycopy(digest, 0, hash, 0, hash.length);
		return hash;
	}

	@Override
	public String toString() {
		return "EventFingerprint{" +
			"timeArray=" + timeArray.size() +
			", eventTypeCounter=" + eventTypeCounter +
			", hash=" + Arrays.toString(hash) +
			'}';
	}
}
