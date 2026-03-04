package org.matsim.dsim.scoring;

import com.google.inject.Inject;
import org.matsim.api.core.v01.Message;
import org.matsim.api.core.v01.network.Network;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

class BackpackTransitRouteProvider implements BackpackRouteProvider {

	private final Network network;
	private final TransitSchedule transitSchedule;

	@Inject
	public BackpackTransitRouteProvider(Network network, TransitSchedule transitSchedule) {
		this.network = network;
		this.transitSchedule = transitSchedule;
	}

	@Override
	public BackpackRoute get() {
		return new BackpackTransitRoute(network, transitSchedule);
	}

	@Override
	public BackpackRoute get(Message fromMessage) {
		//noinspection DeconstructionCanBeUsed
		if (fromMessage instanceof BackpackTransitRoute.Msg msg) {
			return new BackpackTransitRoute(network, transitSchedule, msg.parts());
		}

		throw new IllegalArgumentException("Cannot create route builder from message of type " + fromMessage.getClass());
	}
}
