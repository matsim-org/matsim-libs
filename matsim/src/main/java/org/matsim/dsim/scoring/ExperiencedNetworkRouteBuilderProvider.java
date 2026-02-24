package org.matsim.dsim.scoring;

import com.google.inject.Inject;
import org.matsim.api.core.v01.Message;
import org.matsim.api.core.v01.network.Network;

public class ExperiencedNetworkRouteBuilderProvider implements ExperiencedRouteBuilderProvider {

	private final Network network;

	@Inject
	public ExperiencedNetworkRouteBuilderProvider(Network network) {
		this.network = network;
	}

	@Override
	public ExperiencedRouteBuilder get() {
		return new ExperiencedNetworkRouteBuilder(network);
	}

	@Override
	public ExperiencedRouteBuilder get(Message fromMessage) {
		if (fromMessage instanceof ExperiencedNetworkRouteBuilder.Data data)
			return new ExperiencedNetworkRouteBuilder(network, data);

		throw new IllegalArgumentException("Cannot create route builder from message of type " + fromMessage.getClass());
	}
}
