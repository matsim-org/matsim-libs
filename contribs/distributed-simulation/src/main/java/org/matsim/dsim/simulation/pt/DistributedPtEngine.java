package org.matsim.dsim.simulation.pt;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.mobsim.dsim.DistributedDepartureHandler;
import org.matsim.core.mobsim.dsim.DistributedMobsimEngine;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.qsim.InternalInterface;
import org.matsim.core.mobsim.qsim.pt.TransitQSimEngine;
import org.matsim.dsim.simulation.net.SimNetwork;
import org.matsim.pt.transitSchedule.api.TransitRoute;

import java.util.stream.Stream;

public class DistributedPtEngine implements DistributedMobsimEngine, DistributedDepartureHandler {

	private final TransitQSimEngine transitQSimEngine;

	DistributedPtEngine(Scenario scenario, TransitQSimEngine transitQSimEngine, SimNetwork simNetwork) {
		this.transitQSimEngine = transitQSimEngine;

		// find out which links are pt links.
		var ptLinks = scenario.getTransitSchedule().getTransitLines().values().stream()
			.flatMap(line -> line.getRoutes().values().stream())
			.map(TransitRoute::getRoute)
			.flatMap(netRoute -> Stream.concat(Stream.of(netRoute.getStartLinkId(), netRoute.getEndLinkId()), netRoute.getLinkIds().stream()))
			.distinct()
			.map(id -> simNetwork.getLinks().get(id))
			.map(PtLink::new)
			.toList();
		// override existing normal links with pt link.
		for (var ptLink : ptLinks) {
			simNetwork.getLinks().put(ptLink.getId(), ptLink);
		}
	}

	@Override
	public boolean handleDeparture(double now, MobsimAgent agent, Id<Link> linkId) {
		return transitQSimEngine.handleDeparture(now, agent, linkId);
	}

	@Override
	public void setInternalInterface(InternalInterface internalInterface) {
		transitQSimEngine.setInternalInterface(internalInterface);
	}

	@Override
	public void doSimStep(double time) {
		transitQSimEngine.doSimStep(time);
	}

	@Override
	public void afterSim() {
		transitQSimEngine.afterSim();
	}
}
