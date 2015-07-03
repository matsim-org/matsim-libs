package lanes;

import java.util.ArrayList;
import java.util.List;

import junit.framework.Assert;

import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
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
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.QSimUtils;
import org.matsim.core.population.PopulationFactoryImpl;
import org.matsim.core.population.routes.LinkNetworkRouteFactory;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.lanes.data.v20.Lane;
import org.matsim.lanes.data.v20.LaneDefinitions20;
import org.matsim.lanes.data.v20.LaneDefinitionsFactory20;
import org.matsim.lanes.data.v20.LaneDefinitionsWriter20;
import org.matsim.lanes.data.v20.LanesToLinkAssignment20;
import org.matsim.testcases.MatsimTestUtils;

/**
 * 
 * this class tests the TravelTime of one agent on different configuration of a 200m link. currently, the following phenomenon occurs:
 * the TravelTime on a 200m link is reduced if lanes are enabled
 * 
 * see method createNetwork for details on links configuration 
 * 
 * the network.xml and the Lanedefintions.xml files are written to the testOutputDirectory
 * 
 * @author Tilmann Schlenther
 *
 */
public class LinkLaneTTTest {
	
	
	private static String OUTPUT_DIR = "";
	private static Id<Link> linkId1 = Id.create("incoming", Link.class);
	private static Id<Link> linkId2 = Id.create("200noLanesSpeed75", Link.class);
	private static Id<Link> linkId3 = Id.create("50noLanesSpeed75", Link.class);
	private static Id<Link> linkId4 = Id.create("150noLanesSpeed75", Link.class);
	private static Id<Link> linkId5 = Id.create("200LanesSpeed75", Link.class);
	private static Id<Link> linkId6 = Id.create("200noLanesSpeed76", Link.class);
	private static Id<Link> linkId7 = Id.create("50noLanesSpeed76", Link.class);
	private static Id<Link> linkId8 = Id.create("150noLanesSpeed76", Link.class);
	private static Id<Link> linkId9 = Id.create("200LanesSpeed76", Link.class);
	private static Id<Link> linkId10 = Id.create("outgoing", Link.class);
	
	@Rule public MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	public void testLinkTT(){
		LinkLaneTTTest.OUTPUT_DIR = utils.getOutputDirectory();
		EventsManager events = EventsUtils.createEventsManager();
		Config config = createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);
		createNetwork(scenario);
		createPopulation(scenario, 1);
				
		NetworkWriter writer = new NetworkWriter(scenario.getNetwork());
		writer.write(OUTPUT_DIR +"network.xml");
		
		LinkLaneTTTestEventHandler handler = new LinkLaneTTTestEventHandler();
		events.addHandler(handler);
				
		QSim qsim = QSimUtils.createDefaultQSim(scenario, events);
		qsim.run();
				
		handler.printResults();
		
		Assert.assertEquals("unexpected TravelTime: configuration 1 (freespeed 75)",
				3.0, handler.getTTV1Sp75());
		Assert.assertEquals("unexpected TravelTime: configuration 1 (freespeed 76)",
				3.0, handler.getTTV1Sp76());

		Assert.assertEquals("unexpected TravelTime: configuration 2 (freespeed 75)",
				4.0, handler.getTTV2Sp75());
		Assert.assertEquals("unexpected TravelTime: configuration 2 (freespeed 76)",
				3.0, handler.getTTV2Sp76());

		Assert.assertEquals("unexpected TravelTime: configuration 3 (freespeed 75)",
				3.0, handler.getTTV3Sp75());
		Assert.assertEquals("unexpected TravelTime: configuration 3 (freespeed 76) "
				+ "=> bug might be fixed if value is 3.0", 2.0, handler.getTTV3Sp76());

		
	}
	
	/**
	 * The network to be created has 10 links in total. One incoming and one 
	 * outgoing both with the length 200m and freespeed 200 m/s.
	 * 
	 * the following versions of a 200m link are put twice inbetween (once with 
	 * freespeed 75 m/s, once with freespeed 76 m/s):
	 *
	 * normal 200m link 											(1)
	 * split in a 50m and a 150m link 								(2)
	 * 200m link with 50m original lane leading to one 150m lanes	(3)
	 * 
	 * 	
	 * (*) stands for a Node, + for end of a lane
	 *
	 *
	 *  (1)						(2)							(3)
	 *														   	   
	 *  (*)=====(*)				(*)===(*)====(*)			(*) ===== + =======(*)
	 *  [..200m...]				[.50m.][.150m..]					  
	 *													     [..50m..][..150m..]
	 */
	static void createNetwork(Scenario scenario){
		
		Network network = scenario.getNetwork();
		NetworkFactory factory = network.getFactory();
		
		Node node1 = factory.createNode(Id.createNodeId("1"), scenario.createCoord(0, 0));
		Node node2 = factory.createNode(Id.createNodeId("2"), scenario.createCoord(0, 200));
		Node node3 = factory.createNode(Id.createNodeId("3"), scenario.createCoord(0, 400));
		Node node4 = factory.createNode(Id.createNodeId("4"), scenario.createCoord(0, 450));
		Node node5 = factory.createNode(Id.createNodeId("5"), scenario.createCoord(0, 600));
		Node node6 = factory.createNode(Id.createNodeId("6"), scenario.createCoord(0, 800));
		Node node7 = factory.createNode(Id.createNodeId("7"), scenario.createCoord(0, 850));
		Node node8 = factory.createNode(Id.createNodeId("8"), scenario.createCoord(0, 1000));
		Node node9 = factory.createNode(Id.createNodeId("9"), scenario.createCoord(0, 1200));
		Node node10 = factory.createNode(Id.createNodeId("10"), scenario.createCoord(0, 1400));
		Node node11 = factory.createNode(Id.createNodeId("11"), scenario.createCoord(0, 1600));
		
		network.addNode(node1);
		network.addNode(node2);
		network.addNode(node3);
		network.addNode(node4);
		network.addNode(node5);
		network.addNode(node6);
		network.addNode(node7);
		network.addNode(node8);
		network.addNode(node9);
		network.addNode(node10);
		network.addNode(node11);

		Link link1 = factory.createLink((linkId1), node1, node2);		//incoming
		link1.setCapacity(3600);
		link1.setLength(200);
		link1.setFreespeed(200);
		network.addLink(link1);
		
		Link link2 = factory.createLink((linkId2), node2, node3);		//normal 200m link with freespeed=75
		link2.setCapacity(3600);
		link2.setLength(200);
		link2.setFreespeed(75);
		network.addLink(link2);		
	
		Link link3 = factory.createLink((linkId3), node3 , node4);		//50m link with freespeed=75
		link3.setCapacity(3600);
		link3.setLength(50);
		link3.setFreespeed(75);
		network.addLink(link3);
		
		Link link4 = factory.createLink((linkId4), node4 , node5);		//150m link with freespeed=75
		link4.setCapacity(3600);
		link4.setLength(150);
		link4.setFreespeed(75);
		network.addLink(link4);
		
		Link link5 = factory.createLink((linkId5), node5, node6);		//200m link with lanes; freespeed=75
		link5.setCapacity(3600);
		link5.setLength(200);
		link5.setFreespeed(75);
		network.addLink(link5);
		
		Link link6 = factory.createLink((linkId6), node6, node7);		//normal 200m link with freespeed=76
		link6.setCapacity(3600);
		link6.setLength(200);
		link6.setFreespeed(76);
		network.addLink(link6);		
	
		Link link7 = factory.createLink((linkId7), node7 , node8);		//50m link with freespeed=76
		link7.setCapacity(3600);
		link7.setLength(50);
		link7.setFreespeed(76);
		network.addLink(link7);
		
		Link link8 = factory.createLink((linkId8), node8 , node9);		//150m link with freespeed=76
		link8.setCapacity(3600);
		link8.setLength(150);
		link8.setFreespeed(76);
		network.addLink(link8);
		
		Link link9 = factory.createLink((linkId9), node9 , node10);		//200m link with lanes; freespeed=76
		link9.setCapacity(3600);
		link9.setLength(200);
		link9.setFreespeed(76);
		network.addLink(link9);
		
		Link link10 = factory.createLink((linkId10), node10 , node11);	//outgoing
		link10.setCapacity(3600);
		link10.setLength(200);
		link10.setFreespeed(200);
		network.addLink(link10);
		
		//create Lanes
		LaneDefinitions20 lanes =scenario.getLanes();
		LaneDefinitionsFactory20 lb = lanes.getFactory();
		Id<Lane> ol = Id.create("speed75.ol", Lane.class);
		Id<Lane> secondLane75Id = Id.create("speed75.1", Lane.class);
		Id<Lane> ol76 = Id.create("speed76.ol", Lane.class);
		Id<Lane> secondLane76Id = Id.create("speed76.1", Lane.class);
		LanesToLinkAssignment20 l2l75 = lb.createLanesToLinkAssignment(linkId5);
		LanesToLinkAssignment20 l2l76 = lb.createLanesToLinkAssignment(linkId9);
		
		//create original lanes
		Lane olLane75 = lb.createLane(ol);
		olLane75.setNumberOfRepresentedLanes(1);
		olLane75.setStartsAtMeterFromLinkEnd(200.0);
		olLane75.addToLaneId(secondLane75Id);
		l2l75.addLane(olLane75);
		
		Lane olLane76 = lb.createLane(ol76);
		olLane76.setNumberOfRepresentedLanes(1);
		olLane76.setStartsAtMeterFromLinkEnd(200.0);
		olLane76.addToLaneId(secondLane76Id);
		l2l76.addLane(olLane76);
		
	
		//create following lanes	
		Lane secondLane75 = lb.createLane(secondLane75Id);
		secondLane75.setNumberOfRepresentedLanes(1);
		secondLane75.setStartsAtMeterFromLinkEnd(150.0);
		secondLane75.setAlignment(1);
		secondLane75.addToLinkId(linkId6);
		l2l75.addLane(secondLane75);
				
		Lane secondLane76 = lb.createLane(secondLane76Id);
		secondLane76.setNumberOfRepresentedLanes(1);
		secondLane76.setStartsAtMeterFromLinkEnd(150.0);
		secondLane76.setAlignment(1);
		secondLane76.addToLinkId(linkId10);
		l2l76.addLane(secondLane76);
		
		lanes.addLanesToLinkAssignment(l2l75);
		lanes.addLanesToLinkAssignment(l2l76);
		
		LaneDefinitionsWriter20 writer = new LaneDefinitionsWriter20(lanes);
		writer.write(OUTPUT_DIR + "lanes20.xml");
		
		
	}
	
	static Config createConfig(){
		Config config = ConfigUtils.createConfig();
		QSimConfigGroup qSimConfigGroup = config.qsim();
		qSimConfigGroup.setFlowCapFactor(1.0);
		qSimConfigGroup.setStorageCapFactor(1.0);
		qSimConfigGroup.setStuckTime(3600.0);
		config.scenario().setUseLanes(true);
		return config;
	}
	
	private static void createPopulation(Scenario scenario, int NumberOfPersons) {
		
		Population population = scenario.getPopulation();
        PopulationFactoryImpl popFactory = (PopulationFactoryImpl) scenario.getPopulation().getFactory();
		LinkNetworkRouteFactory routeFactory = new LinkNetworkRouteFactory();

		for(int i=NumberOfPersons; i>= 1; i--){
		
		Activity workAct = popFactory.createActivityFromLinkId("work", linkId10);
		
		Leg leg = popFactory.createLeg("car");
		List<Id<Link>> linkIds = new ArrayList<Id<Link>>();
		linkIds.add(linkId2);
		linkIds.add(linkId3);
		linkIds.add(linkId4);
		linkIds.add(linkId5);
		linkIds.add(linkId6);
		linkIds.add(linkId7);
		linkIds.add(linkId8);
		linkIds.add(linkId9);
		
		NetworkRoute route = (NetworkRoute) routeFactory.createRoute(linkId1, linkId10);
		route.setLinkIds(linkId1, linkIds, linkId10);
		leg.setRoute(route);
		
		Person person = popFactory.createPerson(Id.createPersonId(i));
		Plan plan = popFactory.createPlan();
		Activity homeActLink1_1 = popFactory.createActivityFromLinkId("home", linkId1);
		homeActLink1_1.setEndTime(100);
		plan.addActivity(homeActLink1_1);
		plan.addLeg(leg);
		plan.addActivity(workAct);
		person.addPlan(plan);
		population.addPerson(person);
		
		}
	}
	

}
