package org.matsim.contrib.pseudosimulation.trafficinfo;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.pseudosimulation.MobSimSwitcher;
import org.matsim.contrib.pseudosimulation.PSimConfigGroup;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.TravelTimeCalculatorConfigGroup;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vehicles.Vehicle;

/**
 * A pseudo-simulation iteration derives its link travel times from this structure. If its own
 * synthetic link events were also fed back into it, the surrogate would be measuring itself: on a
 * 1:24 run the PSim iterations of one cycle contribute roughly as many link events as the queue
 * simulation does, so about half of what claims to be measured congestion would be PSim's own
 * output.
 */
class PSimTravelTimeCalculatorIntakeTest {

	private static final Id<Link> LINK = Id.createLinkId("link");
	private static final Id<Vehicle> VEHICLE = Id.createVehicleId("car");

	private static final class FixedSwitcher extends MobSimSwitcher {
		private boolean qsim = true;

		FixedSwitcher(Scenario scenario) {
			super(new PSimConfigGroup(), scenario);
		}

		@Override
		public boolean isQSimIteration() {
			return qsim;
		}
	}

	@Test
	void ignoresLinkEventsEmittedDuringAPSimIteration() {
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Network network = scenario.getNetwork();
		Node from = NetworkUtils.createAndAddNode(network, Id.createNodeId("from"), new Coord(0, 0));
		Node to = NetworkUtils.createAndAddNode(network, Id.createNodeId("to"), new Coord(1000, 0));
		Link link = NetworkUtils.createAndAddLink(network, LINK, from, to, 1000.0, 10.0, 1800.0, 1.0);

		FixedSwitcher switcher = new FixedSwitcher(scenario);
		PSimTravelTimeCalculator calculator = new PSimTravelTimeCalculator(
				new TravelTimeCalculatorConfigGroup(), network, switcher);

		calculator.handleEvent(new LinkEnterEvent(3600.0, VEHICLE, LINK));
		calculator.handleEvent(new LinkLeaveEvent(4200.0, VEHICLE, LINK));
		double measured = calculator.get().getLinkTravelTime(link, 3600.0, null, null);

		switcher.qsim = false;
		calculator.handleEvent(new LinkEnterEvent(3600.0, VEHICLE, LINK));
		calculator.handleEvent(new LinkLeaveEvent(3601.0, VEHICLE, LINK));

		assertEquals(measured, calculator.get().getLinkTravelTime(link, 3600.0, null, null),
				"a PSim iteration reads these travel times; it must not also write to them");
	}
}
