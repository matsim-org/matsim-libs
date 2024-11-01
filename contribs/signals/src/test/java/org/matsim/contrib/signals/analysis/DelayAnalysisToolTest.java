package org.matsim.contrib.signals.analysis;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.contrib.signals.analysis.DelayAnalysisTool;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.RoutingConfigGroup;
import org.matsim.core.controler.PrepareForSimUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.algorithms.EventWriterXML;
import org.matsim.core.events.handler.BasicEventHandler;
import org.matsim.core.mobsim.qsim.QSimBuilder;
import org.matsim.core.population.routes.LinkNetworkRouteFactory;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestUtils;

/**
 *
 * @author tschlenther
 * this class tests the functionality of TtTotalDelay in playground.dgrether.koehlerstrehlersignal.analysis
 * which calculates the total delay of all agents in a network
 *
 * having only one agent in a network, the total delay should be 0 (testGetTotalDelayOnePerson)
 * calculation of the delay of all persons in a network should be made in awareness of matsim's step logic
 *
 * the network generated in this tests basically consists of 4 links of 1000m with a free speed of 201 m/s
 * output is optionally written to outputDirectory of this testClass
 * -> set field writeOutput to do so
 *
 * the number of persons to be set in the network for the test can be modified
 * they will get an insertion delay due to the capacity of the first link
 */


public class DelayAnalysisToolTest {

	@RegisterExtension private MatsimTestUtils utils = new MatsimTestUtils();

	private final Id<Link> LINK_ID1 = Id.create("Link1", Link.class);
	private final Id<Link> LINK_ID2 = Id.create("Link2", Link.class);
	private final Id<Link> LINK_ID3 = Id.create("Link3", Link.class);
	private final Id<Link> LINK_ID4 = Id.create("Link4", Link.class);

	//optionally to be modified
	private static final boolean WRITE_OUTPUT = false;
	private static final int NUMBER_OF_PERSONS = 5;

	@Test
	void testGetTotalDelayOnePerson(){
		Scenario scenario = prepareTest(1);

		EventsManager events = EventsUtils.createEventsManager();
		DelayAnalysisTool handler = new DelayAnalysisTool(scenario.getNetwork(), events);

		final List<Event> eventslist = new ArrayList<Event>();
		events.addHandler(new BasicEventHandler(){
			@Override
			public void reset(int iteration) {
				eventslist.clear();
			}
			@Override
			public void handleEvent(Event event) {
				eventslist.add(event);
			}
		});

		PrepareForSimUtils.createDefaultPrepareForSim(scenario).run();
		new QSimBuilder(scenario.getConfig()).useDefaults().build(scenario, events).run();

		Assertions.assertEquals(0.0, handler.getTotalDelay(), MatsimTestUtils.EPSILON, "Total Delay of one agent is not correct");
		if(WRITE_OUTPUT){
			generateOutput(scenario, eventslist);
		}
	}

	@Test
	void testGetTotalDelaySeveralPerson(){
		Scenario scenario = prepareTest(NUMBER_OF_PERSONS);

		EventsManager events = EventsUtils.createEventsManager();
		DelayAnalysisTool handler = new DelayAnalysisTool(scenario.getNetwork(), events);

		final List<Event> eventslist = new ArrayList<Event>();
		events.addHandler(new BasicEventHandler(){
			@Override
			public void reset(int iteration) {
				eventslist.clear();
			}
			@Override
			public void handleEvent(Event event) {
				eventslist.add(event);
			}
		});

		PrepareForSimUtils.createDefaultPrepareForSim(scenario).run();
		new QSimBuilder(scenario.getConfig()).useDefaults().build(scenario, events).run();

		if(WRITE_OUTPUT){
			generateOutput(scenario, eventslist);
		}

		//expectedDelay = inserting delay as a result of capacity of first link being 3600 vh/h
		int expectedDelay = 0;
		for(int i=0; i<NUMBER_OF_PERSONS; i++){
			expectedDelay +=  i;
		}
		Assertions.assertEquals(expectedDelay, handler.getTotalDelay(), MatsimTestUtils.EPSILON, "Total Delay for " + NUMBER_OF_PERSONS + " persons is not correct.");
	}

	private void generateOutput(Scenario scenario, final List<Event> eventslist) {
			EventWriterXML eventWriter = new EventWriterXML(utils.getOutputDirectory() + "events.xml");
			for (Event e : eventslist) {
				eventWriter.handleEvent(e);
			}
			eventWriter.closeFile();
			NetworkWriter nw = new NetworkWriter(scenario.getNetwork());
			nw.write(utils.getOutputDirectory() + "network");
	}

	private Scenario prepareTest(int numberOfPersons) {
		Config config = ConfigUtils.createConfig();
		config.routing().setNetworkRouteConsistencyCheck(RoutingConfigGroup.NetworkRouteConsistencyCheck.disable);
		Scenario scenario = ScenarioUtils.createScenario(config);
		createNetwork(scenario);
		createPopulation(scenario, numberOfPersons);
		return scenario;
	}

	void createNetwork(Scenario scenario){
		Network network = scenario.getNetwork();
		NetworkFactory factory = network.getFactory();

		Node node1 = factory.createNode(Id.createNodeId("1"), new Coord((double) 0, (double) 0));
		Node node2 = factory.createNode(Id.createNodeId("2"), new Coord((double) 0, (double) 1000));
		Node node3 = factory.createNode(Id.createNodeId("3"), new Coord((double) 0, (double) 2000));
		Node node4 = factory.createNode(Id.createNodeId("4"), new Coord((double) 0, (double) 3000));
		Node node5 = factory.createNode(Id.createNodeId("5"), new Coord((double) 0, (double) 4000));

		network.addNode(node1);
		network.addNode(node2);
		network.addNode(node3);
		network.addNode(node4);
		network.addNode(node5);

		Link link1 = factory.createLink((LINK_ID1), node1, node2);
		link1.setCapacity(3600);
		link1.setLength(1000);
		link1.setFreespeed(201);
		network.addLink(link1);

		Link link2 = factory.createLink((LINK_ID2), node2, node3);
		link2.setCapacity(3600);
		link2.setLength(1000);
		link2.setFreespeed(201);
		network.addLink(link2);

		Link link3 = factory.createLink((LINK_ID3), node3 , node4);
		link3.setCapacity(3600);
		link3.setLength(1000);
		link3.setFreespeed(201);
		network.addLink(link3);

		Link link4 = factory.createLink((LINK_ID4), node4, node5);
		link4.setCapacity(3600);
		link4.setLength(1000);
		link4.setFreespeed(201);
		network.addLink(link4);
	}

	private void createPopulation(Scenario scenario, int numberOfPersons) {
		Population population = scenario.getPopulation();
        PopulationFactory popFactory = (PopulationFactory) scenario.getPopulation().getFactory();
		LinkNetworkRouteFactory routeFactory = new LinkNetworkRouteFactory();

		for (int i= 1; i <= numberOfPersons; i++){
			Activity workAct = popFactory.createActivityFromLinkId("work", LINK_ID4);

			Leg leg = popFactory.createLeg("car");
			List<Id<Link>> linkIds = new ArrayList<Id<Link>>();
			linkIds.add(LINK_ID2);
			linkIds.add(LINK_ID3);

			NetworkRoute route = (NetworkRoute) routeFactory.createRoute(LINK_ID1, LINK_ID4);
			route.setLinkIds(LINK_ID1, linkIds, LINK_ID4);
			leg.setRoute(route);

			Person person = popFactory.createPerson(Id.createPersonId(i));
			Plan plan = popFactory.createPlan();
			Activity homeActLink1_1 = popFactory.createActivityFromLinkId("home", LINK_ID1);
			homeActLink1_1.setEndTime(100);
			plan.addActivity(homeActLink1_1);
			plan.addLeg(leg);
			plan.addActivity(workAct);
			person.addPlan(plan);
			population.addPerson(person);
		}
	}
}
