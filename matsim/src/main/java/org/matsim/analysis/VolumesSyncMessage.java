package org.matsim.analysis;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.matsim.api.core.v01.Message;

import java.util.HashMap;
import java.util.Map;

/**
 * Link volumes of one compute node, sent to the other nodes after the simulation. Links are indexed by their id index.
 */
public class VolumesSyncMessage implements Message {

	final Int2ObjectMap<int[]> volumes = new Int2ObjectOpenHashMap<>();

	final Map<String, Int2ObjectMap<int[]>> modeVolumes = new HashMap<>();

}
