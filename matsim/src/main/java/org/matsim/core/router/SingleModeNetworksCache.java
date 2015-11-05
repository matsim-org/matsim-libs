package org.matsim.core.router;

import org.matsim.api.core.v01.network.Network;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SingleModeNetworksCache {

	private Map<String, Network> singleModeNetworksCache = new ConcurrentHashMap<>();

	public Map<String, Network> getSingleModeNetworksCache() {
		return singleModeNetworksCache;
	}
}
