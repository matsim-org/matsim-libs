package org.matsim.dsim.scoring;

import com.google.inject.Inject;
import org.matsim.api.core.v01.Message;
import org.matsim.api.core.v01.network.Network;

class BackpackNetworkRouteProvider implements BackpackRouteProvider {

	private final Network network;

	@Inject
	BackpackNetworkRouteProvider(Network network) {
		this.network = network;
	}

	@Override
	public BackpackRoute get() {
		return new BackpackNetworkRoute(network);
	}

	@Override
	public BackpackRoute get(Message fromMessage) {
		if (fromMessage instanceof BackpackNetworkRoute.Data data)
			return new BackpackNetworkRoute(network, data);

		throw new IllegalArgumentException("Cannot create route builder from message of type " + fromMessage.getClass());
	}
}
