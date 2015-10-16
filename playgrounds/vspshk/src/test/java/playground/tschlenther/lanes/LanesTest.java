package playground.tschlenther.lanes;

import java.util.ArrayList;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
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
import org.matsim.lanes.data.v20.LanesToLinkAssignment20;
import org.matsim.testcases.MatsimTestUtils;

public class LanesTest {

	@Rule public MatsimTestUtils utils = new MatsimTestUtils();
	
	private static String OUTPUT_DIR = "";
	private static Id<Link> linkId1 = Id.create("L1", Link.class);
	private static Id<Link> linkId2 = Id.create("L2", Link.class);
	private static Id<Link> linkId3 = Id.create("L3", Link.class);
	private static Id<Link> linkId4 = Id.create("L4", Link.class);
	
	private static EventsManager events = EventsUtils.createEventsManager();
	
	@Test
	public void test() {
		OUTPUT_DIR = utils.getOutputDirectory();
		Config config = createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);
		createNetwork(scenario);
		createPopulation(scenario, 20);
		
//		NetworkWriter writer = new NetworkWriter(scenario.getNetwork());
//		writer.write(OUTPUT_DIR +"network.xml");
//		
//		final List<Event> eventslist = new ArrayList<Event>();
//
//		events.addHandler(new BasicEventHandler(){
//
//			@Override
//			public void reset(int iteration) {
//				// TODO Auto-generated method stub
//				
//			}
//
//			@Override
//			public void handleEvent(Event event) {
//				eventslist.add(event);			
//			}
//			
//		});
		
		LaneTestEventHandler handler = new LaneTestEventHandler();
		events.addHandler(handler);
		
		QSim qsim = QSimUtils.createDefaultQSim(scenario, events);
		qsim.run();
		
		handler.printAllTravelTimes();
		
//		EventWriterXML eventWriter = new EventWriterXML(System.out);
//		for(Event e: eventslist){
//			eventWriter.handleEvent(e);
//		}
//		eventWriter.closeFile();
		
	}

//	public static void main(String[] args){
//	
//	}
	
	
	static void createNetwork(Scenario scenario){

		Network network = scenario.getNetwork();
		NetworkFactory factory = network.getFactory();

		Node node1 = factory.createNode(Id.createNodeId("1"), new Coord((double) 0, (double) 0));
		Node node2 = factory.createNode(Id.createNodeId("2"), new Coord((double) 0, (double) 200));
		Node node3 = factory.createNode(Id.createNodeId("3"), new Coord((double) 0, (double) 400));
		Node node4 = factory.createNode(Id.createNodeId("4"), new Coord((double) 0, (double) 600));
		Node node5 = factory.createNode(Id.createNodeId("5"), new Coord((double) 0, (double) 800));
		
		network.addNode(node1);
		network.addNode(node2);
		network.addNode(node3);
		network.addNode(node4);
		network.addNode(node5);

		Link link1 = factory.createLink((linkId1), node1, node2);
		link1.setCapacity(5400);
		link1.setLength(200);
		link1.setFreespeed(200);			// TT = 1s
		network.addLink(link1);
		
		Link link2 = factory.createLink((linkId2), node2, node3);
		link2.setCapacity(1);
		link2.setLength(200);
		link2.setFreespeed(100);
		network.addLink(link2);		
	
		Link link3 = factory.createLink((linkId3), node3 , node4);
		link3.setCapacity(3600);
		link3.setLength(200);
		link3.setFreespeed(201);			//TT = 1s		
		network.addLink(link3);
		
		Link link4 = factory.createLink((linkId4), node4 , node5);
		link4.setCapacity(3600);
		link4.setLength(200);
		link4.setFreespeed(200);			//TT = 1s
		network.addLink(link4);
		
		//create Lanes
		LaneDefinitions20 lanes = scenario.getLanes();
		LaneDefinitionsFactory20 lb = lanes.getFactory();
		Id<Lane> ol = Id.create("2.ol", Lane.class);
		Id<Lane> topLane = Id.create("2.1", Lane.class);
//		Id<Lane> middleLane = Id.create("2.2", Lane.class);
		Id<Lane> bottomLane = Id.create("2.3", Lane.class);

		
		//first lane
		Lane olLane = lb.createLane(ol);
		olLane.setNumberOfRepresentedLanes(1);
		olLane.setStartsAtMeterFromLinkEnd(200.0);
		
		olLane.addToLaneId(topLane);
		olLane.addToLaneId(bottomLane);
		olLane.setCapacityVehiclesPerHour(5400);
		LanesToLinkAssignment20 l2l = lb.createLanesToLinkAssignment(linkId2);
		l2l.addLane(olLane);
		
		//split link in 2 lanes		
		Lane tLane = lb.createLane(topLane);
		tLane.setNumberOfRepresentedLanes(1);
		tLane.setStartsAtMeterFromLinkEnd(150.0);
		tLane.setAlignment(1);
		tLane.setCapacityVehiclesPerHour(1800);
		tLane.addToLinkId(link3.getId());

		
//		Lane mLane = lb.createLane(middleLane);
//		mLane.setNumberOfRepresentedLanes(1);
//		mLane.setStartsAtMeterFromLinkEnd(150.0);
//		mLane.setAlignment(0);
//		mLane.addToLinkId(link3.getId());
		
		Lane bLane = lb.createLane(bottomLane);
		bLane.setNumberOfRepresentedLanes(1);
		bLane.setStartsAtMeterFromLinkEnd(150.0);
		bLane.setAlignment(-1);
		bLane.setCapacityVehiclesPerHour(3600);
		bLane.addToLinkId(link3.getId());
		l2l.addLane(tLane);
		l2l.addLane(bLane);
//		l2l.addLane(mLane);

		lanes.addLanesToLinkAssignment(l2l);
		
//		LaneDefinitionsWriter20 writer = new LaneDefinitionsWriter20(lanes);
//		writer.write(OUTPUT_DIR +"lanes20.xml");
		
		
	}
	
	 Config createConfig(){
		Config config = utils.loadConfig(null);
		QSimConfigGroup qSimConfigGroup = config.qsim();
		qSimConfigGroup.setFlowCapFactor(1.0);
		qSimConfigGroup.setStorageCapFactor(1.0);
		qSimConfigGroup.setInsertingWaitingVehiclesBeforeDrivingVehicles(true);
		qSimConfigGroup.setRemoveStuckVehicles(true);
		qSimConfigGroup.setStuckTime(3600.0);
		qSimConfigGroup.setUseLanes(true);
		config.controler().setFirstIteration(0);
		config.controler().setLastIteration(0);
		return config;
	}
	
	private static void createPopulation(Scenario scenario, int NumberOfPersons) {
		
		Population population = scenario.getPopulation();
        PopulationFactoryImpl popFactory = (PopulationFactoryImpl) scenario.getPopulation().getFactory();
		LinkNetworkRouteFactory routeFactory = new LinkNetworkRouteFactory();

		for(int i=NumberOfPersons; i>= 1; i--){
		
		Activity workAct = popFactory.createActivityFromLinkId("work", Id.createLinkId("L"+4));
		
		Leg leg_1_5 = popFactory.createLeg("car");
		List<Id<Link>> linkIds_1_5 = new ArrayList<Id<Link>>();
		linkIds_1_5.add(linkId2);
		linkIds_1_5.add(linkId3);
		
		NetworkRoute route_1_5 = (NetworkRoute) routeFactory.createRoute(linkId1, linkId4);
		route_1_5.setLinkIds(linkId1, linkIds_1_5, linkId4);
		leg_1_5.setRoute(route_1_5);
		
		Person person = popFactory.createPerson(Id.createPersonId(i));
		Plan plan = popFactory.createPlan();
		Activity homeActLink1_1 = popFactory.createActivityFromLinkId("home", linkId1);
		homeActLink1_1.setEndTime(100);
		plan.addActivity(homeActLink1_1);
		plan.addLeg(leg_1_5);
		plan.addActivity(workAct);
		person.addPlan(plan);
		population.addPerson(person);
		
		}
	}
	

	
	
	
	
	
}