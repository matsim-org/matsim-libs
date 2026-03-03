package org.matsim.contrib.drt.routing;

import com.google.inject.Inject;
import org.matsim.api.core.v01.Message;
import org.matsim.api.core.v01.network.Network;
import org.matsim.dsim.scoring.BackpackRoute;
import org.matsim.dsim.scoring.BackpackRouteProvider;

public class DrtRouteProvider implements BackpackRouteProvider {

	private final Network network;

	@Inject
	public DrtRouteProvider(Network network) {
		this.network = network;
	}

	@Override
	public BackpackRoute get() {
		return new BackpackDrtRoute(network);
	}

	@Override
	public BackpackRoute get(Message fromMessage) {
		if (fromMessage instanceof BackpackDrtRoute.Data data)
			return new BackpackDrtRoute(network, data);

		throw new IllegalArgumentException("Cannot create DrtRouteBuilder from message of type " + fromMessage.getClass());
	}
}
