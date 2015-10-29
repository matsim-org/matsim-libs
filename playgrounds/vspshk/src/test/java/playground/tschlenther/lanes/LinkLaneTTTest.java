package playground.tschlenther.lanes;



import java.util.ArrayList;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
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
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.algorithms.EventWriterXML;
import org.matsim.core.events.handler.BasicEventHandler;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.QSimUtils;
import org.matsim.core.population.PopulationFactoryImpl;
import org.matsim.core.population.routes.LinkNetworkRouteFactory;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.lanes.data.v20.Lane;
import org.matsim.lanes.data.v20.Lanes;
import org.matsim.lanes.data.v20.LaneDefinitionsFactory20;
import org.matsim.lanes.data.v20.LaneDefinitionsWriter20;
import org.matsim.lanes.data.v20.LanesToLinkAssignment20;
import org.matsim.testcases.MatsimTestUtils;

/**
 * This class tests the travel time of one agent on different configuration of a
 * 200m link. Currently, the following phenomenon occurs for certain values of
 * free speed: the travel time on a 200m link is reduced if lanes are enabled
 * 
 * See methods createNetwork and modifyNetwork for details on link
 * configurations.
 * 
 * You can add cases of network configuration in modifyNetwork(). Therefore you
 * need to adapt the field NUMBER_OF_CASES as well as the methods getTTofCase()
 * and printResults() in LinkLaneTTTestEventHandler. Please note that only
 * Link2 is to be modified. Otherwise the event handler won't recognize changes.
 * 
 * If you want to have a look at the lane definition, network or event file,
 * enable the field writeOutput.
 * 
 * @author Tilmann Schlenther
 */

public class LinkLaneTTTest {
	
	private static Id<Link> LINK_ID1 = Id.create("Link1", Link.class);
	private static Id<Link> LINK_ID2 = Id.create("Link2", Link.class);
	private static Id<Link> LINK_ID3 = Id.create("Link3", Link.class);
	
	// needs to be enabled if you want to have output files like events, network
	// and lanes
	private static boolean WRITE_OUTPUT = false;
	
	// needs to be modified if you add cases
	private static final int NUMBER_OF_CASES = 8;									

	
	@Rule public MatsimTestUtils utils = new MatsimTestUtils();
	
	@Test
	public void testLinkTT(){
		EventsManager events = EventsUtils.createEventsManager();
		Config config = createConfig();
		
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
		
		LinkLaneTTTestEventHandler handler = new LinkLaneTTTestEventHandler();
		events.addHandler(handler);
		
		for(int i = 1; i<= NUMBER_OF_CASES ; i++){
			
			/*
			 * since modifyNetwork acts in the assumption of Case1-network and
			 * the plan needs to be modified dependent on the case these three
			 * lines need to be here and not above
			 */
			Scenario scenario = ScenarioUtils.createScenario(config);
			createAndModifyNetwork(scenario, i);
			createPopulation(scenario, i);				 			
			
			QSim qsim = QSimUtils.createDefaultQSim(scenario, events);
			qsim.run();
			
			// write network and events
			if (WRITE_OUTPUT) {
				NetworkWriter networkWriter = new NetworkWriter(
						scenario.getNetwork());
				networkWriter.write(utils.getOutputDirectory() + "CASE" + i
						+ "_network");
				
				EventWriterXML eventWriter = new EventWriterXML(
						utils.getOutputDirectory() + "CASE" + i + "_events");
				for (Event e : eventslist) {
					eventWriter.handleEvent(e);
				}
				eventWriter.closeFile();
			}		
			events.resetHandlers(0);
		}
		
		handler.printResults();
	}
	
	
	/**
	 * Creates the basic Case1 network as shown below and modifies it 
	 * dependent on the case number.
	 *  
	 * Only the second link is to be modified.
	 * In Case1 it has a freespeed of 75m/s and a length of 200m		
															
	   Link1   Link2   Link3												
	(1)=====(2)=====(3)=====(4)
	[..200m..][.200m.][.200m.]	
	
	 * @param caseNr the case number 								
	 */
	private void createAndModifyNetwork(Scenario scenario, int caseNr){
		
		Network network = scenario.getNetwork();
		NetworkFactory factory = network.getFactory();

		Node node1 = factory.createNode(Id.createNodeId("1"), new Coord((double) 0, (double) 0));
		Node node2 = factory.createNode(Id.createNodeId("2"), new Coord((double) 0, (double) 200));
		Node node3 = factory.createNode(Id.createNodeId("3"), new Coord((double) 0, (double) 400));
		Node node4 = factory.createNode(Id.createNodeId("4"), new Coord((double) 0, (double) 600));
		
		network.addNode(node1);
		network.addNode(node2);
		network.addNode(node3);
		network.addNode(node4);
		
		Link link1 = factory.createLink(LINK_ID1, node1, node2);		
		link1.setCapacity(3600);
		link1.setLength(200);
		link1.setFreespeed(200);
		network.addLink(link1);
		
		// create a normal 200m link with freespeed 75 m/s
		Link link2 = factory.createLink(LINK_ID2, node2, node3);		
		link2.setCapacity(3600);
		link2.setLength(200);
		link2.setFreespeed(75);
		network.addLink(link2);		
	
		Link link3 = factory.createLink(LINK_ID3, node3 , node4);		
		link3.setCapacity(3600);
		link3.setLength(200);
		link3.setFreespeed(200);
		network.addLink(link3);
		
		if(!(caseNr == 1)){
			modifyNetwork(scenario, caseNr);
		}
		
	}
	
	/**
	 * Method to modify the CASE1 network. 
	 * Note: only Link2 is to be modified.
	 * 
	 * If you add cases, adapt the field NUMBER_OF_CASES
	 * 
	 * @param caseNr indicates the network version to be created 
	 * 
	 */
	private void modifyNetwork(Scenario scenario, int caseNr){
		Network network = scenario.getNetwork();
		Link link2 = network.getLinks().get(LINK_ID2);
		NetworkFactory factory = network.getFactory();
		
		switch (caseNr){
		case 2:
			//set speed to 76 m/s
			link2.setFreespeed(76);
			break;
		case 3:
		case 4:
			/*
			 * split Link2 into four lanes of 50m 
																	
			   Link1  		Link2		 Link3	
			   
			   		   [50m][50m][50m][50m]							
			  (1)=====(2)===+===+===+===(3)=====(4)
			  [..200m..][......200m......][.200m.]	
			 */
			Lanes lanes = scenario.getLanes();
			LaneDefinitionsFactory20 lfactory = lanes.getFactory();
			Id<Lane> olId = Id.create("2.1", Lane.class);
			Id<Lane> secondLaneId = Id.create("2.2", Lane.class);
			Id<Lane> thirdLaneId = Id.create("2.3", Lane.class);
			Id<Lane> fourthLaneId = Id.create("2.4", Lane.class);
			LanesToLinkAssignment20 l2l = lfactory.createLanesToLinkAssignment(LINK_ID2);
			
			//create 4 lanes following each other
			Lane lane = lfactory.createLane(olId);
			lane.setNumberOfRepresentedLanes(1);
			lane.setStartsAtMeterFromLinkEnd(200.0);
			lane.addToLaneId(secondLaneId);
			l2l.addLane(lane);
			
			lane = lfactory.createLane(secondLaneId);
			lane.setNumberOfRepresentedLanes(1);
			lane.setStartsAtMeterFromLinkEnd(150.0);
			lane.setAlignment(0);
			lane.addToLaneId(thirdLaneId);
			l2l.addLane(lane);
			
			lane = lfactory.createLane(thirdLaneId);
			lane.setNumberOfRepresentedLanes(1);
			lane.setStartsAtMeterFromLinkEnd(100.0);
			lane.setAlignment(0);
			lane.addToLaneId(fourthLaneId);
			l2l.addLane(lane);
			
			lane = lfactory.createLane(fourthLaneId);
			lane.setNumberOfRepresentedLanes(1);
			lane.setStartsAtMeterFromLinkEnd(50.0);
			lane.setAlignment(0);
			lane.addToLinkId(LINK_ID3);
			l2l.addLane(lane);
			
			lanes.addLanesToLinkAssignment(l2l);			
			
			if(caseNr==4){
				link2.setFreespeed(76);
			}
			
			if(WRITE_OUTPUT){
				LaneDefinitionsWriter20 writer = new LaneDefinitionsWriter20(lanes);
				writer.write(utils.getOutputDirectory() + "4lanes.xml");
			}
			
			break;
		case 5:
		case 6:
			/*
			 * split Link2 into one original lane of 50m and two parallel lanes of 150m 
			 * leading to Link3
																			
			   Link1     Link2    Link3   
			   
			   		   [50m][150m.]
			  (1)=====(2)==+=====(3)=====(4)
						   
			  [..200m..][..200m..][.200m.]	
			 */
			lanes = scenario.getLanes();
			lfactory = lanes.getFactory();
			Id<Lane> ol = Id.create("2.1", Lane.class);
			Id<Lane> followingLane = Id.create("2.2", Lane.class);
			l2l = lfactory.createLanesToLinkAssignment(LINK_ID2);
			
			//create original lane of 50m
			lane = lfactory.createLane(ol);
			lane.setNumberOfRepresentedLanes(1);
			lane.setStartsAtMeterFromLinkEnd(200.0);
			lane.addToLaneId(followingLane);
			l2l.addLane(lane);
			
			//create following lane of 150m
			lane = lfactory.createLane(followingLane);
			lane.setNumberOfRepresentedLanes(1);
			lane.setStartsAtMeterFromLinkEnd(150.0);
			lane.setAlignment(0);
			lane.addToLinkId(LINK_ID3);
			l2l.addLane(lane);
						
			lanes.addLanesToLinkAssignment(l2l);			
			
			if(caseNr==6){
				link2.setFreespeed(76);
			}
			
			if(WRITE_OUTPUT){
				LaneDefinitionsWriter20 writer = new LaneDefinitionsWriter20(lanes);
				writer.write(utils.getOutputDirectory() + "topBottomlanes.xml");
			}
			
			break;			
		case 7:
		case 8:
			/*
			 * split Link2 in one 50m link and one 150m link 
																	
			     Link1 Link2.1 Link 2  Link3	
			   
			   		   [.50m..][.150m.]							
			  (1)=====(2)===(2.1)=====(4)====(5)
			  [..200m..][...200m......][.200m.]	
			 */
			Node inbetweenNode = factory.createNode(Id.createNodeId("2.1"),
					new Coord((double) 0, (double) 250));
			Link inbetweenLink = factory.createLink(Id.createLinkId("Link2.1"),
					scenario.getNetwork().getNodes().get(Id.createNodeId("2")),
					inbetweenNode);
			inbetweenLink.setLength(50);
			inbetweenLink.setFreespeed(75);
			
			link2.setFromNode(inbetweenNode);
			link2.setLength(150);
			
			if(caseNr==8){
				inbetweenLink.setFreespeed(76);
				link2.setFreespeed(76);
			}
			
			network.addNode(inbetweenNode);			
			network.addLink(inbetweenLink);
			
			break;
		}	
	}
	
	private Config createConfig(){
		Config config = ConfigUtils.createConfig();
		QSimConfigGroup qSimConfigGroup = config.qsim();
		qSimConfigGroup.setFlowCapFactor(1.0);
		qSimConfigGroup.setStorageCapFactor(1.0);
		qSimConfigGroup.setInsertingWaitingVehiclesBeforeDrivingVehicles(true);
		qSimConfigGroup.setRemoveStuckVehicles(true);
		qSimConfigGroup.setStuckTime(3600.0);
		config.qsim().setUseLanes(true);
		return config;
	}
	
	private void createPopulation(Scenario scenario, int caseNr) {
		
		Population population = scenario.getPopulation();
        PopulationFactoryImpl popFactory = (PopulationFactoryImpl) scenario.getPopulation().getFactory();
		LinkNetworkRouteFactory routeFactory = new LinkNetworkRouteFactory();
		
		Activity workAct = popFactory.createActivityFromLinkId("work", LINK_ID3);
		
		Leg leg = popFactory.createLeg("car");
		List<Id<Link>> linkIds = new ArrayList<Id<Link>>();
		if(caseNr == 7 || caseNr ==8){
			linkIds.add(Id.createLinkId("Link2.1"));
		}

		linkIds.add(LINK_ID2);
		
		NetworkRoute route = (NetworkRoute) routeFactory.createRoute(LINK_ID1, LINK_ID3);
		route.setLinkIds(LINK_ID1, linkIds, LINK_ID3);
		leg.setRoute(route);
		
		Person person = popFactory.createPerson(Id.createPersonId("P"));
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
