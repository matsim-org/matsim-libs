package org.matsim.withinday.trafficmonitoring;

import javax.inject.Singleton;

import org.matsim.core.config.groups.ControlerConfigGroup;
import org.matsim.core.controler.AbstractModule;

public class WithinDayTravelTimeModule extends AbstractModule {

	public WithinDayTravelTimeModule() {
	}

	@Override
	public void install() {
		if (getConfig().controler().getRoutingAlgorithmType() != ControlerConfigGroup.RoutingAlgorithmType.Dijkstra) {
			throw new RuntimeException(
					"for me, in KNAccidentScenario, this works with Dijkstra (default until spring 2019), and does not work with AStarLandmarks "
							+ "(default afterwards).  I have not tried the other routing options, nor have I systematically debugged. KN, feb'19");
		}

		bind(WithinDayTravelTime.class).in(Singleton.class);
		addEventHandlerBinding().to(WithinDayTravelTime.class);
		bindNetworkTravelTime().to(WithinDayTravelTime.class);
		// yyyyyy also needs to be bound as mobsim listener.  There is maybe a reason
		// why this is not added here, but could someone please explain?  thx.  kai, dec'17
		// Trying it out:
		addMobsimListenerBinding().to(WithinDayTravelTime.class);
	}
}
