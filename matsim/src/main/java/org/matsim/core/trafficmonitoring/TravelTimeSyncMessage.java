package org.matsim.core.trafficmonitoring;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.matsim.api.core.v01.Message;

/**
 * Data class so sent travel time information.
 */
public class TravelTimeSyncMessage implements Message {

	final Int2ObjectMap<long[]> travelTimes = new Int2ObjectOpenHashMap<>();
}
