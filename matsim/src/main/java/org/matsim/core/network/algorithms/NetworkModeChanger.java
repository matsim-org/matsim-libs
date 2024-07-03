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
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * This class changes the allowed modes of links in a network and cleans the network afterward.
 * I.e. for each mode, it removes by running the {@link NetworkCleaner} that mode for links, that are not reachable from all other links
 * or from which all other links are not reachable.
 */
public class NetworkModeChanger implements NetworkRunnable {
	private static final Logger log = LogManager.getLogger(NetworkModeChanger.class);

	private final Map<Id<Link>, Set<String>> changes;

	public NetworkModeChanger(Map<Id<Link>, Set<String>> changes) {
		this.changes = changes;
	}

	@Override
	public void run(Network network) {
		applyModeChanges(network);
		cleanNetworkPerMode(network);
		removeLinksWithNoModes(network);
		removeNodesWithNoLinks(network);
	}

	private void applyModeChanges(Network network) {
		for (Map.Entry<Id<Link>, Set<String>> entry : changes.entrySet()) {
			Link link = network.getLinks().get(entry.getKey());
			link.setAllowedModes(entry.getValue());
		}
	}

	private void cleanNetworkPerMode(Network network) {
		Set<String> modes = network.getLinks().values().stream().flatMap(l -> l.getAllowedModes().stream()).collect(Collectors.toSet());
		for (String mode : modes) {
			cleanNetworkForMode(network, mode);
		}
	}

	private void cleanNetworkForMode(Network network, String mode) {
		TransportModeNetworkFilter filter = new TransportModeNetworkFilter(network);
		Network modalSubNetwork = NetworkUtils.createNetwork();
		filter.filter(modalSubNetwork, Set.of(mode));

		log.info("Cleaning network for mode: {}", mode);
		int sizeBeforeCleaning = modalSubNetwork.getLinks().size();
		new NetworkCleaner().run(modalSubNetwork);
		int sizeAfterCleaning = modalSubNetwork.getLinks().size();
		log.info("Removed mode {} from {} links.", mode, sizeBeforeCleaning - sizeAfterCleaning);

		//remove mode from links that are not in the modal subnetwork
		for (Link link : network.getLinks().values()) {
			if (Objects.isNull(modalSubNetwork.getLinks().get(link.getId()))) {
				NetworkUtils.removeAllowedMode(link, mode);
			}
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
}
