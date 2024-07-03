package org.matsim.core.network.algorithms;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.api.internal.NetworkRunnable;
import org.matsim.core.network.NetworkUtils;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * This class changes the allowed modes of links in a network and cleans the network afterward.
 * I.e. for each mode, it removes by running the {@link NetworkCleaner} that mode for links, that are not reachable from all other links
 * or from which all other links are not reachable.
 */
public class NetworkModeRestriction implements NetworkRunnable {
	private static final Logger log = LogManager.getLogger(NetworkModeRestriction.class);

	private final Map<Id<Link>, Set<String>> modesToRemoveByLinkId;

	public NetworkModeRestriction(Map<Id<Link>, Set<String>> modesToRemoveByLinkId) {
		this.modesToRemoveByLinkId = modesToRemoveByLinkId;
	}

	@Override
	public void run(Network network) {
		Map<String, Long> modeCountBefore = countModes(network);

		applyModeChanges(network);
		cleanNetworkPerMode(network);
		removeLinksWithNoModes(network);
		removeNodesWithNoLinks(network);

		Map<String, Long> modeCountAfter = countModes(network);
		logModeCountDifference(modeCountBefore, modeCountAfter);
	}

	private void applyModeChanges(Network network) {
		for (Map.Entry<Id<Link>, Set<String>> entry : modesToRemoveByLinkId.entrySet()) {
			Link link = network.getLinks().get(entry.getKey());
			for (String modeToRemove : entry.getValue()) {
				NetworkUtils.removeAllowedMode(link, modeToRemove);
			}
		}
	}

	private void cleanNetworkPerMode(Network network) {
		Set<String> modes = network.getLinks().values().stream().flatMap(l -> l.getAllowedModes().stream()).collect(Collectors.toSet());
		for (String mode : modes) {
			MultimodalNetworkCleaner multimodalNetworkCleaner = new MultimodalNetworkCleaner(network);
			multimodalNetworkCleaner.run(Set.of(mode));
		}
	}

	private void removeLinksWithNoModes(Network network) {
		network.getLinks().values().stream()
			   .filter(l -> l.getAllowedModes().isEmpty())
			   .map(Link::getId)
			   .forEach(network::removeLink);
	}

	private void removeNodesWithNoLinks(Network network) {
		network.getNodes().values().stream()
			   .filter(n -> n.getInLinks().isEmpty() && n.getOutLinks().isEmpty())
			   .map(Node::getId)
			   .forEach(network::removeNode);
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
