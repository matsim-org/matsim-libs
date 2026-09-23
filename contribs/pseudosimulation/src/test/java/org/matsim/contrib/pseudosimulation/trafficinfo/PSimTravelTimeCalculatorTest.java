package org.matsim.contrib.pseudosimulation.trafficinfo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.pseudosimulation.MobSimSwitcher;
import org.matsim.contrib.pseudosimulation.PSimConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.TravelTimeCalculatorConfigGroup;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vehicles.Vehicle;

/**
 * A pseudo-simulation iteration is only meaningful if it reads the link travel times measured by
 * the preceding queue simulation. This pins the guard that keeps those measurements alive, which
 * only takes effect if this class is registered as the event handler rather than its delegate.
 */
class PSimTravelTimeCalculatorTest {

	private static final Id<Link> LINK = Id.createLinkId("congested");
	private static final Id<Vehicle> VEHICLE = Id.createVehicleId("car");

	/** Lets the test choose the mobsim of the iteration without driving a whole controler. */
	private static final class FixedSwitcher extends MobSimSwitcher {
		private boolean qsim = true;

		FixedSwitcher(PSimConfigGroup group, Scenario scenario) {
			super(group, scenario);
		}

		@Override
		public boolean isQSimIteration() {
			return qsim;
		}
	}

	private record Fixture(PSimTravelTimeCalculator calculator, FixedSwitcher switcher, Link link) {
	}

	private static Fixture fixture() {
		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);
		Network network = scenario.getNetwork();
		Node from = NetworkUtils.createAndAddNode(network, Id.createNodeId("from"),
				new org.matsim.api.core.v01.Coord(0, 0));
		Node to = NetworkUtils.createAndAddNode(network, Id.createNodeId("to"),
				new org.matsim.api.core.v01.Coord(1000, 0));
		Link link = NetworkUtils.createAndAddLink(network, LINK, from, to, 1000.0, 10.0, 1800.0, 1.0);

		FixedSwitcher switcher = new FixedSwitcher(new PSimConfigGroup(), scenario);
		PSimTravelTimeCalculator calculator = new PSimTravelTimeCalculator(
				new TravelTimeCalculatorConfigGroup(), network, switcher);
		return new Fixture(calculator, switcher, link);
	}

	/** Drives a vehicle over the link slowly enough to be distinguishable from free speed. */
	private static void observeCongestedTraversal(PSimTravelTimeCalculator calculator) {
		calculator.handleEvent(new LinkEnterEvent(3600.0, VEHICLE, LINK));
		calculator.handleEvent(new LinkLeaveEvent(4200.0, VEHICLE, LINK));
	}

	@Test
	void keepsMeasuredTravelTimesThroughAPSimIteration() {
		Fixture fixture = fixture();
		observeCongestedTraversal(fixture.calculator());
		double measured = fixture.calculator().get()
				.getLinkTravelTime(fixture.link(), 3600.0, null, null);
		assertTrue(measured > 100.0,
				"the observed traversal should be far slower than free speed, but was " + measured);

		fixture.switcher().qsim = false;
		fixture.calculator().reset(1);

		assertEquals(measured, fixture.calculator().get()
						.getLinkTravelTime(fixture.link(), 3600.0, null, null),
				"a PSim iteration must keep the travel times of the preceding QSim iteration");
	}

	@Test
	void discardsMeasuredTravelTimesWhenTheNextIterationIsAQSimIteration() {
		Fixture fixture = fixture();
		observeCongestedTraversal(fixture.calculator());
		double measured = fixture.calculator().get()
				.getLinkTravelTime(fixture.link(), 3600.0, null, null);

		fixture.switcher().qsim = true;
		fixture.calculator().reset(1);

		assertNotEquals(measured, fixture.calculator().get()
						.getLinkTravelTime(fixture.link(), 3600.0, null, null),
				"a QSim iteration measures travel times afresh");
	}
}
