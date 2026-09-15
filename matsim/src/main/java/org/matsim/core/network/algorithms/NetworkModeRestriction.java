package org.matsim.core.network.algorithms;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.internal.NetworkRunnable;
import org.matsim.core.network.NetworkUtils;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * This class changes the allowed modes of links in a network and cleans the
 * network afterward.
 * I.e. for each mode, it removes by running
 * {@link NetworkUtils#cleanNetwork(Network, Set)} that mode for links, that are
 * not reachable from all other links or from which all other links are not
 * reachable.
 */
public class NetworkModeRestriction implements NetworkRunnable {
	private static final Logger log = LogManager.getLogger(NetworkModeRestriction.class);

	private final Function<Id<Link>, Set<String>> modesToRemoveByLinkId;

	private Set<String> removedModes;

	public NetworkModeRestriction(Function<Id<Link>, Set<String>> modesToRemoveByLinkId) {
		this.modesToRemoveByLinkId = modesToRemoveByLinkId;
	}

	public NetworkModeRestriction(Map<Id<Link>, Set<String>> modesToRemoveByLinkId) {
		this.modesToRemoveByLinkId = l -> modesToRemoveByLinkId.getOrDefault(l, Set.of());
	}

	@Override
	public void run(Network network) {
		Map<String, Long> modeCountBefore = countModes(network);

		applyModeChanges(network);

		log.info("Cleaning network for mdoes: {}", this.removedModes);
		NetworkUtils.cleanNetwork(network, this.removedModes);

		Map<String, Long> modeCountAfter = countModes(network);
		logModeCountDifference(modeCountBefore, modeCountAfter);
	}

	private void applyModeChanges(Network network) {
		this.removedModes = new HashSet<>();
		for (Map.Entry<Id<Link>, ? extends Link> link : network.getLinks().entrySet()) {
			this.modesToRemoveByLinkId.apply(link.getKey()).forEach(m -> {
				NetworkUtils.removeAllowedMode(link.getValue(), m);
				removedModes.add(m);
			});
		}
	}

	private void logModeCountDifference(Map<String, Long> modeCountBefore, Map<String, Long> modeCountAfter) {
		modeCountBefore.forEach((mode, countBefore) -> {
			long countAfter = modeCountAfter.getOrDefault(mode, 0L);
			log.info("Removed mode {} from {} links.", mode, countBefore - countAfter);
		});
	}

	private static Map<String, Long> countModes(Network network) {
		return network.getLinks().values().stream()
					  .flatMap(l -> l.getAllowedModes().stream())
					  .collect(Collectors.groupingBy(m -> m, Collectors.counting()));
	}
}
