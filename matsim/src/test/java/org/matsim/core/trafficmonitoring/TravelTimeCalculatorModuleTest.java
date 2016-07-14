package org.matsim.core.trafficmonitoring;

import com.google.inject.Key;
import com.google.inject.name.Names;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Injector;
import org.matsim.core.events.EventsManagerModule;
import org.matsim.core.scenario.ScenarioByInstanceModule;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestUtils;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class TravelTimeCalculatorModuleTest {

	@Rule
	public MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	public void testOneTravelTimeCalculatorForAll() {
		Config config = ConfigUtils.createConfig();
		config.travelTimeCalculator().setAnalyzedModes("car,bike");
		Scenario scenario = ScenarioUtils.createScenario(config);
		Node node0 = scenario.getNetwork().getFactory().createNode(Id.createNodeId(0), new Coord(0, 0));
		Node node1 = scenario.getNetwork().getFactory().createNode(Id.createNodeId(1), new Coord(1, 0));
		scenario.getNetwork().addNode(node0);
		scenario.getNetwork().addNode(node1);
		Id<Link> linkId = Id.createLinkId(0);
		Link link = scenario.getNetwork().getFactory().createLink(linkId, node0, node1);
		scenario.getNetwork().addLink(link);
		com.google.inject.Injector injector = Injector.createInjector(config, new TravelTimeCalculatorModule(), new EventsManagerModule(), new ScenarioByInstanceModule(scenario));
		TravelTimeCalculator testee = injector.getInstance(TravelTimeCalculator.class);
		EventsManager events = injector.getInstance(EventsManager.class);
		events.processEvent(new VehicleEntersTrafficEvent(0.0, Id.createPersonId(0), linkId, Id.createVehicleId(0), "car", 0.0));
		events.processEvent(new LinkEnterEvent(0.0, Id.createVehicleId(0), linkId));
		events.processEvent(new LinkLeaveEvent(2.0, Id.createVehicleId(0), linkId));
		events.processEvent(new VehicleLeavesTrafficEvent(2.0, Id.createPersonId(0), linkId, Id.createVehicleId(0), "car", 0.0));

		events.processEvent(new VehicleEntersTrafficEvent(0.0, Id.createPersonId(1), linkId, Id.createVehicleId(1), "bike", 0.0));
		events.processEvent(new LinkEnterEvent(0.0, Id.createVehicleId(1), linkId));
		events.processEvent(new LinkLeaveEvent(8.0, Id.createVehicleId(1), linkId));
		events.processEvent(new VehicleLeavesTrafficEvent(8.0, Id.createPersonId(1), linkId, Id.createVehicleId(1), "bike", 0.0));

		assertThat(testee.getLinkTravelTime(linkId, 0.0), is(5.0));
	}


	@Test
	public void testOneTravelTimeCalculatorPerMode() {
		Config config = ConfigUtils.createConfig();
		config.travelTimeCalculator().setAnalyzedModes("car,bike");
		config.travelTimeCalculator().setSeparateModes(true);
		Scenario scenario = ScenarioUtils.createScenario(config);
		Node node0 = scenario.getNetwork().getFactory().createNode(Id.createNodeId(0), new Coord(0, 0));
		Node node1 = scenario.getNetwork().getFactory().createNode(Id.createNodeId(1), new Coord(1, 0));
		scenario.getNetwork().addNode(node0);
		scenario.getNetwork().addNode(node1);
		Id<Link> linkId = Id.createLinkId(0);
		Link link = scenario.getNetwork().getFactory().createLink(linkId, node0, node1);
		scenario.getNetwork().addLink(link);
		com.google.inject.Injector injector = Injector.createInjector(config, new TravelTimeCalculatorModule(), new EventsManagerModule(), new ScenarioByInstanceModule(scenario));
		TravelTimeCalculator car = injector.getInstance(Key.get(TravelTimeCalculator.class, Names.named("car")));
		TravelTimeCalculator bike = injector.getInstance(Key.get(TravelTimeCalculator.class, Names.named("bike")));
		EventsManager events = injector.getInstance(EventsManager.class);
		events.processEvent(new VehicleEntersTrafficEvent(0.0, Id.createPersonId(0), linkId, Id.createVehicleId(0), "car", 0.0));
		events.processEvent(new LinkEnterEvent(0.0, Id.createVehicleId(0), linkId));
		events.processEvent(new LinkLeaveEvent(2.0, Id.createVehicleId(0), linkId));
		events.processEvent(new VehicleLeavesTrafficEvent(2.0, Id.createPersonId(0), linkId, Id.createVehicleId(0), "car", 0.0));

		events.processEvent(new VehicleEntersTrafficEvent(0.0, Id.createPersonId(1), linkId, Id.createVehicleId(1), "bike", 0.0));
		events.processEvent(new LinkEnterEvent(0.0, Id.createVehicleId(1), linkId));
		events.processEvent(new LinkLeaveEvent(8.0, Id.createVehicleId(1), linkId));
		events.processEvent(new VehicleLeavesTrafficEvent(8.0, Id.createPersonId(1), linkId, Id.createVehicleId(1), "bike", 0.0));

		assertThat(car.getLinkTravelTime(linkId, 0.0), is(2.0));
		assertThat(bike.getLinkTravelTime(linkId, 0.0), is(8.0));
	}

}
