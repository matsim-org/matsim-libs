package org.matsim.contrib.drt.routing;

import com.google.inject.Inject;
import org.matsim.api.core.v01.Message;
import org.matsim.api.core.v01.network.Network;
import org.matsim.dsim.scoring.ExperiencedRouteBuilder;
import org.matsim.dsim.scoring.ExperiencedRouteBuilderProvider;

public class DrtRouteBuilderProvider implements ExperiencedRouteBuilderProvider {

	private final Network network;

	@Inject
	public DrtRouteBuilderProvider(Network network) {
		this.network = network;
	}

	@Override
	public ExperiencedRouteBuilder get() {
		return new ExperiencedDrtRouteBuilder(network);
	}

	@Override
	public ExperiencedRouteBuilder get(Message fromMessage) {
		if (fromMessage instanceof ExperiencedDrtRouteBuilder.Data data)
			return new ExperiencedDrtRouteBuilder(network, data);

		throw new IllegalArgumentException("Cannot create DrtRouteBuilder from message of type " + fromMessage.getClass());
	}
}
