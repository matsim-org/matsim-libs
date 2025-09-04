package org.matsim.dsim.simulation.net;

import lombok.Getter;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.gbl.MatsimRandom;

import java.util.*;

public class SimNode {

	@Getter
	private final Id<Node> id;
	private final Random rng;
	@Getter
	private final List<SimLink> inLinks = new ArrayList<>();
	@Getter
	private final Map<Id<Link>, SimLink> outLinks = new HashMap<>();

	public SimNode(Id<Node> id) {
		this.id = id;
		this.rng = MatsimRandom.getLocalInstance(id.hashCode());
	}

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

	/**
	 * The node maintains an rng. The rng needs to reside inside the SimNode, so that the sequence of random numbers used in intersection updates
	 * is similar regardless of the applied partitioning.
	 *
	 * @return next random double in the random number sequence.
	 */
	double nextDouble() {
		return rng.nextDouble();
	}

	void addInLink(SimLink link) {
		inLinks.add(link);
	}

	void addOutLink(SimLink link) {
		outLinks.put(link.getId(), link);
	}
}
