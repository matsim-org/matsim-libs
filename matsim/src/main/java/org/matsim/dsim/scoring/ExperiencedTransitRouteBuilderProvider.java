package org.matsim.dsim.scoring;

import com.google.inject.Inject;
import org.matsim.api.core.v01.Message;
import org.matsim.api.core.v01.network.Network;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

public class ExperiencedTransitRouteBuilderProvider implements ExperiencedRouteBuilderProvider {

	private final Network network;
	private final TransitSchedule transitSchedule;

	@Inject
	public ExperiencedTransitRouteBuilderProvider(Network network, TransitSchedule transitSchedule) {
		this.network = network;
		this.transitSchedule = transitSchedule;
	}

	@Override
	public ExperiencedRouteBuilder get() {
		return new ExperiencedTransitRouteBuilder(network, transitSchedule);
	}

	@Override
	public ExperiencedRouteBuilder get(Message fromMessage) {
		//noinspection DeconstructionCanBeUsed
		if (fromMessage instanceof ExperiencedTransitRouteBuilder.Msg msg) {
			return new ExperiencedTransitRouteBuilder(network, transitSchedule, msg.parts());
		}

		throw new IllegalArgumentException("Cannot create route builder from message of type " + fromMessage.getClass());
	}
}
