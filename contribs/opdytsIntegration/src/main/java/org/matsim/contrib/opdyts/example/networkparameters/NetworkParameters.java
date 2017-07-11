package org.matsim.contrib.opdyts.example.networkparameters;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;

import floetteroed.opdyts.DecisionVariable;

class NetworkParameters implements DecisionVariable {

	// -------------------- MEMBERS --------------------

	private final Map<Link, LinkParameters> link2params;

	// -------------------- CONSTRUCTION --------------------

	NetworkParameters(final Network network) {
		this.link2params = new LinkedHashMap<>();
		for (Link link : network.getLinks().values()) {
			this.link2params.put(link, new LinkParameters(link));
		}
	}

	NetworkParameters(final Map<Link, LinkParameters> link2params) {
		this.link2params = link2params;
	}

	// -------------------- IMPLEMENTATION --------------------

	Map<Link, LinkParameters> getLinkParameters() {
		return Collections.unmodifiableMap(this.link2params);
	}

	// --------------- IMPLEMENTATION OF DecisionVariable ---------------

	@Override
	public final void implementInSimulation() {
		for (Map.Entry<Link, LinkParameters> entry : this.link2params.entrySet()) {
			entry.getKey().setFreespeed(entry.getValue().getFreespeed());
			entry.getKey().setCapacity(entry.getValue().getFlowCapacity());
			entry.getKey().setNumberOfLanes(entry.getValue().getNofLanes());
		}
	}
	
	// -------------------- OVERRIDING OF Object --------------------

	@Override
	public String toString() {
		return this.link2params.toString();
	}
}
