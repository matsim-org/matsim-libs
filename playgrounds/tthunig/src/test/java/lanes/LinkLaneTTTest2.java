package lanes;



import java.util.ArrayList;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
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
import org.matsim.lanes.data.v20.LaneDefinitions20;
import org.matsim.lanes.data.v20.LaneDefinitions20Impl;
import org.matsim.lanes.data.v20.LaneDefinitionsFactory20;
import org.matsim.lanes.data.v20.LaneDefinitionsWriter20;
import org.matsim.lanes.data.v20.LanesToLinkAssignment20;
import org.matsim.testcases.MatsimTestUtils;

/**
 * 
 * @author Tilmann Schlenther
 * this class tests the TravelTime of one agent on different configuration of a 200m link. currently, the following phenomenon occurs
 * for certain values of freespeed:
 * the TravelTime on a 200m link is reduced if lanes are enabled
 * 
 * see methods createNetwork and modifyNetwork for details on links configuration 
 *
 * you can add cases of network configuration in modifyNetwork(). Therefore you need to adapt the field NUMBER_OF_CASES.
 * please note that only Link2 is to be modified. otherwise the eventhandler won't recognize changes.
 * 
 * if you want to have a look at the LaneDefintion or network files or the events, set the field writeOutput true.
 * 
 */

public class LinkLaneTTTest2 {
	
	private static String OUTPUT_DIR = "";
	private static Id<Link> linkId1 = Id.create("Link1", Link.class);
	private static Id<Link> linkId2 = Id.create("Link2", Link.class);
	private static Id<Link> linkId3 = Id.create("Link3", Link.class);
	
	@Rule public MatsimTestUtils utils = new MatsimTestUtils();
	private final int NUMBER_OF_CASES = 8;									//needs to be modified if you add cases

	private static boolean writeOutput = true;
	
	@Test
	public void testLinkTT(){
		OUTPUT_DIR = utils.getOutputDirectory();
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
		
		LinkLaneTTTestEventHandler2 handler = new LinkLaneTTTestEventHandler2();
		events.addHandler(handler);
		
		for(int i = 1; i<= NUMBER_OF_CASES ; i++){
			Scenario scenario = ScenarioUtils.createScenario(config); 		//since modifyNetwork acts in the assumption of Case1-network 
			createAndModifyNetwork(scenario, i);							//and the plan needs to be modified dependent on the case	
			createPopulation(scenario, i);						 			//these three lines need to be here and not above
			QSim qsim = QSimUtils.createDefaultQSim(scenario, events);
			qsim.run();
			
			//write events to OUTPUT_DIR
			if(writeOutput){
			NetworkWriter networkWriter = new NetworkWriter(scenario.getNetwork()) ;
			networkWriter.write(OUTPUT_DIR + "CASE" + i + "_network");
			EventWriterXML eventWriter = new EventWriterXML(OUTPUT_DIR+"CASE"+i+"_events");
			for(Event e: eventslist){
				eventWriter.handleEvent(e);
			}
			eventWriter.closeFile();
			}		
			System.out.println("-----------------------------------------------");
			events.resetHandlers(0);
		}
		
		handler.printResults();

	}
	
	
	/**create the basic Case1 Network as shown below and modify it dependent on @param caseNr. 
	 * Only the second Link2 is to be modified.
	 * Link2 has originally (in Case1) a freespeed of 75m/s and a length of 200m
				
															
	   Link1   Link2   Link3												
	(1)=====(2)=====(3)=====(4)
	[..200m..][.200m.][.200m.]	
	 * @param caseNr 
													

	**/
	static void createAndModifyNetwork(Scenario scenario, int caseNr){
		
		Network network = scenario.getNetwork();
		NetworkFactory factory = network.getFactory();
		
		Node node1 = factory.createNode(Id.createNodeId("1"), scenario.createCoord(0, 0));
		Node node2 = factory.createNode(Id.createNodeId("2"), scenario.createCoord(0, 200));
		Node node3 = factory.createNode(Id.createNodeId("3"), scenario.createCoord(0, 400));
		Node node4 = factory.createNode(Id.createNodeId("4"), scenario.createCoord(0, 600));
		
		network.addNode(node1);
		network.addNode(node2);
		network.addNode(node3);
		network.addNode(node4);
		
		Link link1 = factory.createLink((linkId1), node1, node2);		
		link1.setCapacity(3600);
		link1.setLength(200);
		link1.setFreespeed(200);
		network.addLink(link1);
		
		Link link2 = factory.createLink((linkId2), node2, node3);		//normal 200m link with freespeed=75
		link2.setCapacity(3600);
		link2.setLength(200);
		link2.setFreespeed(75);
		network.addLink(link2);		
	
		Link link3 = factory.createLink((linkId3), node3 , node4);		
		link3.setCapacity(3600);
		link3.setLength(200);
		link3.setFreespeed(200);
		network.addLink(link3);
		
		if(!(caseNr == 1)){
			modifyNetwork(scenario, caseNr);
		}
		
	}
	
	/**method to modify the CASE1 network 
	 * @param caseNr: indicates the network version to be created 
	 * NOTE: only Link2 is to be modified!
	 * if you add cases, adapt the field NUMBER_OF_CASES
	 **/
	
	static void modifyNetwork(Scenario scenario, int caseNr){
		Network network = scenario.getNetwork();
		Link link2 = network.getLinks().get(linkId2);
		NetworkFactory factory = network.getFactory();
		
		
		//set speed to 76 m/s
		if(caseNr == 2){
			link2.setFreespeed(76);
		}

		
		/**
		 * split Link2 in four lanes of 50m 
																
		   Link1  		Link2		 Link3	
		   
		   		 [50m][50m][50m][50m]							
		(1)=====(2)===+===+===+===(3)=====(4)
		[..200m..][......200m......][.200m.]	
		**/
		else if(caseNr == 3 || caseNr == 4){
			LaneDefinitions20 lanes = scenario.getLanes();
			LaneDefinitionsFactory20 lfactory = lanes.getFactory();
			Id<Lane> olId = Id.create("2.1", Lane.class);
			Id<Lane> secondLaneId = Id.create("2.2", Lane.class);
			Id<Lane> thirdLaneId = Id.create("2.3", Lane.class);
			Id<Lane> fourthLaneId = Id.create("2.4", Lane.class);
			LanesToLinkAssignment20 l2l = lfactory.createLanesToLinkAssignment(linkId2);
			
			//create 4 lanes following each other
			Lane olLane = lfactory.createLane(olId);
			olLane.setNumberOfRepresentedLanes(1);
			olLane.setStartsAtMeterFromLinkEnd(200.0);
			olLane.addToLaneId(secondLaneId);
			l2l.addLane(olLane);
			
			Lane secondLane = lfactory.createLane(secondLaneId);
			secondLane.setNumberOfRepresentedLanes(1);
			secondLane.setStartsAtMeterFromLinkEnd(150.0);
			secondLane.setAlignment(0);
			secondLane.addToLaneId(thirdLaneId);
			l2l.addLane(secondLane);
			
			Lane thirdLane = lfactory.createLane(thirdLaneId);
			thirdLane.setNumberOfRepresentedLanes(1);
			thirdLane.setStartsAtMeterFromLinkEnd(100.0);
			thirdLane.setAlignment(0);
			thirdLane.addToLaneId(fourthLaneId);
			l2l.addLane(thirdLane);
			
			Lane fourthLane = lfactory.createLane(fourthLaneId);
			fourthLane.setNumberOfRepresentedLanes(1);
			fourthLane.setStartsAtMeterFromLinkEnd(50.0);
			fourthLane.setAlignment(0);
			fourthLane.addToLinkId(linkId3);
			l2l.addLane(fourthLane);
			
			lanes.addLanesToLinkAssignment(l2l);			
			
			if(caseNr==4){
				link2.setFreespeed(76);
			}
			if(writeOutput){
				LaneDefinitionsWriter20 writer = new LaneDefinitionsWriter20(lanes);
				writer.write(OUTPUT_DIR + "4lanes.xml");
			}
		}
		

		/**
		 * split Link2 in one originalLane of 50m and two parallel lanes of 150m leading to Link3
																		
		   Link1     Link2    Link3   
		   
		   			  [150m]
		   			  =====									
		(1)=====(2)==+     (3)=====(4)
					  =====
		[..200m..][..200m..][.200m.]	
		**/
		else if(caseNr==5 || caseNr ==6){
			LaneDefinitions20 lanes = scenario.getLanes();
			LaneDefinitionsFactory20 lfactory = lanes.getFactory();
			Id<Lane> ol = Id.create("2.ol", Lane.class);
			Id<Lane> topLane = Id.create("2.top", Lane.class);
			Id<Lane> bottomLane = Id.create("2.btm", Lane.class);
			LanesToLinkAssignment20 l2l = lfactory.createLanesToLinkAssignment(linkId2);
			
			//create original lane leading to two parallel Lanes
			Lane olLane = lfactory.createLane(ol);
			olLane.setNumberOfRepresentedLanes(1);
			olLane.setStartsAtMeterFromLinkEnd(200.0);
			olLane.addToLaneId(topLane);
			olLane.addToLaneId(bottomLane);
			l2l.addLane(olLane);
			
			//create two parallel lanes
			Lane tLane = lfactory.createLane(topLane);
			tLane.setNumberOfRepresentedLanes(1);
			tLane.setStartsAtMeterFromLinkEnd(150.0);
			tLane.setAlignment(1);
			tLane.addToLinkId(linkId3);
			l2l.addLane(tLane);
			
			Lane bLane = lfactory.createLane(bottomLane);
			bLane.setNumberOfRepresentedLanes(1);
			bLane.setStartsAtMeterFromLinkEnd(150.0);
			bLane.setAlignment(-1);
			bLane.addToLinkId(linkId3);
			l2l.addLane(bLane);
			
			lanes.addLanesToLinkAssignment(l2l);			
			
			if(caseNr==6){
				link2.setFreespeed(76);
			}
			if(writeOutput){
				LaneDefinitionsWriter20 writer = new LaneDefinitionsWriter20(lanes);
				writer.write(OUTPUT_DIR + "topBottomlanes.xml");
			}
		}
		
		
		/**
		 * split Link2 in one 50m Link and one 150m Link 
																
		   Link1  		Link2	 Link3	
		   
		   		 [.50m.][.150m.]							
		(1)=====(2)===(3)=====(4)====(5)
		[..200m..][..200m......][.200m.]	
		**/
		else if(caseNr==7 || caseNr ==8){
			Node inbetweenNode = factory.createNode(Id.createNodeId("2.1"), scenario.createCoord(0, 250));
			Link inbetweenLink = factory.createLink(Id.createLinkId("Link2.1"), scenario.getNetwork().getNodes().get(Id.createNodeId("2")), inbetweenNode);
			inbetweenLink.setLength(50);
			link2.setFromNode(inbetweenNode);
			if(caseNr == 7){
				inbetweenLink.setFreespeed(75);
			}
			else if(caseNr==8){
				inbetweenLink.setFreespeed(76);
				link2.setFreespeed(76);
			}
			network.addNode(inbetweenNode);			
			network.addLink(inbetweenLink);
		}		
	}
	
	static Config createConfig(){
		Config config = ConfigUtils.createConfig();
		QSimConfigGroup qSimConfigGroup = config.qsim();
		qSimConfigGroup.setFlowCapFactor(1.0);
		qSimConfigGroup.setStorageCapFactor(1.0);
		qSimConfigGroup.setInsertingWaitingVehiclesBeforeDrivingVehicles(true);
		qSimConfigGroup.setRemoveStuckVehicles(true);
		qSimConfigGroup.setStuckTime(3600.0);
		config.scenario().setUseLanes(true);
		return config;
	}
	
	private static void createPopulation(Scenario scenario, int caseNr) {
		
		Population population = scenario.getPopulation();
        PopulationFactoryImpl popFactory = (PopulationFactoryImpl) scenario.getPopulation().getFactory();
		LinkNetworkRouteFactory routeFactory = new LinkNetworkRouteFactory();
		
		Activity workAct = popFactory.createActivityFromLinkId("work", linkId3);
		
		Leg leg = popFactory.createLeg("car");
		List<Id<Link>> linkIds = new ArrayList<Id<Link>>();
		if(caseNr == 7 || caseNr ==8){
			linkIds.add(Id.createLinkId("Link2.1"));
		}

		linkIds.add(linkId2);
		
		NetworkRoute route = (NetworkRoute) routeFactory.createRoute(linkId1, linkId3);
		route.setLinkIds(linkId1, linkIds, linkId3);
		leg.setRoute(route);
		
		Person person = popFactory.createPerson(Id.createPersonId("P"));
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
