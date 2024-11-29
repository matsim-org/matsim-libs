package org.matsim.dsim.simulation.net;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
public class SimNode {

	@Getter
	private final Id<Node> id;
	@Getter
	private final List<SimLink> inLinks = new ArrayList<>();
	@Getter
	private final Map<Id<Link>, SimLink> outLinks = new HashMap<>();

	// We have had problems with the order of vehicles being switched, when the number of processes changes. One source of error is the
	// sequence of random numbers being different for varying number of processes. For example: An ActiveNodes object in a single
	// threaded setup takes care of 100 nodes, but with 2 threads two ActiveNodes take care of ±50 nodes each. This alters the sequence
	// of random numbers and therefore the sequence of links being served during the intersection update. This attempt puts a random
	// seed into the node, and increments the seed everytime a random number is generated for that node. This way, the seed for each
	// random number is the same, regardless of the number of processes.
	private int randomSeed;

	double calculateAvailableCapacity() {
		return inLinks.stream()
			.filter(SimLink::isOffering)
			.mapToDouble(SimLink::getMaxFlowCapacity)
			.sum();
	}

	boolean[] createExhaustedLinks() {
		var result = new boolean[inLinks.size()];
		for (var i = 0; i < inLinks.size(); i++) {
			if (!inLinks.get(i).isOffering()) {
				result[i] = true;
			}
		}
		return result;
	}

	boolean isActiveInNextTimestep() {
		return inLinks.stream().anyMatch(SimLink::isOffering);
	}

	int nextRandomSeed() {
		this.randomSeed++;
		return randomSeed;
	}

	void addInLink(SimLink link) {
		inLinks.add(link);
	}

	void addOutLink(SimLink link) {
		outLinks.put(link.getId(), link);
	}
}
