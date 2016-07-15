package org.matsim.core.router;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.matsim.core.network.Network;

public class SingleModeNetworksCache {

	private Map<String, Network> singleModeNetworksCache = new ConcurrentHashMap<>();

	public Map<String, Network> getSingleModeNetworksCache() {
		return singleModeNetworksCache;
	}
}
