/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */
package playground.agarwalamit.mixedTraffic.FDTestSetUp;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.VspExperimentalConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup.LinkDynamics;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vehicles.VehicleCapacity;
import org.matsim.vehicles.VehicleCapacityImpl;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;

import playground.agarwalamit.mixedTraffic.MixedTrafficVehiclesUtils;

/**
 * @author amit
 */
public class InputsForFDTestSetUp {
	private String outputFolder;

	public static final int SUBDIVISION_FACTOR=1; //all sides of the triangle will be divided into subdivisionFactor links
	public static final double LINK_LENGTH = 1000;//in m, length of one the triangle sides.
	public static final double NO_OF_LANES = 1;//in m, length of one the triangle sides.
	private  final int LINK_CAPACITY = 2700;//in PCU/h
	private  final double END_TIME = 24*3600;
	private final  double FREESPEED = 60.;						//in km/h, maximum authorized velocity on the track
	private final  double STUCK_TIME = 10;
	
	private Scenario scenario;
	private  Map<Id<VehicleType>, TravelModesFlowDynamicsUpdator> vehicle2TravelModesData;
	
	void run(){
		this.outputFolder= GenerateFundamentalDiagramData.RUN_DIR;
		setUpConfig();
		createTriangularNetwork();
		//Initializing modeData objects//TODO [AA]: should be initialized when instancing FundamentalDiagrams, no workaround still found
		//Need to be currently initialized at this point to initialize output and modified QSim
		fillTravelModeData();
	}

	private void setUpConfig(){
		GenerateFundamentalDiagramData.log.info("==========Creating config ============");
		Config config = ConfigUtils.createConfig();

		config.qsim().setMainModes(Arrays.asList(GenerateFundamentalDiagramData.TRAVELMODES));
		config.qsim().setStuckTime(STUCK_TIME);//allows to overcome maximal density regime
		config.qsim().setEndTime(END_TIME);//allows to set agents to abort after getting the wanted data.
		if(GenerateFundamentalDiagramData.PASSING_ALLOWED){
			config.qsim().setLinkDynamics(LinkDynamics.PassingQ.toString());
		}
		if(GenerateFundamentalDiagramData.WITH_HOLES){
			config.qsim().setTrafficDynamics(QSimConfigGroup.withHoles);
			config.qsim().setSnapshotStyle(QSimConfigGroup.SNAPSHOT_WITH_HOLES);
			config.setParam("WITH_HOLE", "HOLE_SPEED", GenerateFundamentalDiagramData.HOLE_SPEED);
		}
		
		if(GenerateFundamentalDiagramData.SEEPAGE_ALLOWED){
			config.setParam("seepage", "isSeepageAllowed", "true");
			config.setParam("seepage", "seepMode","bike");
			config.setParam("seepage", "isSeepModeStorageFree", "false");
		}
		config.vspExperimental().addParam("vspDefaultsCheckingLevel", VspExperimentalConfigGroup.ABORT) ;
		scenario = ScenarioUtils.createScenario(config);
		if(GenerateFundamentalDiagramData.writeInputFiles) new ConfigWriter(config).write(outputFolder+"/config.xml");
	}
	/**
	 * It will generate a triangular network. 
	 * Each link is subdivided in number of sub division factor.
	 */
	private void createTriangularNetwork(){
		GenerateFundamentalDiagramData.log.info("==========Creating network=========");
		Network network = scenario.getNetwork();
		int capMax = 100*LINK_CAPACITY;

		//nodes of the equilateral triangle base starting, left node at (0,0)
		for (int i = 0; i<SUBDIVISION_FACTOR+1; i++){
			double x=0, y=0;
			x = (LINK_LENGTH/SUBDIVISION_FACTOR)*i;
			Coord coord = scenario.createCoord(x, y);
			Id<Node> id = Id.createNodeId(i);

			Node node = scenario.getNetwork().getFactory().createNode(id, coord);
			network.addNode(node);	
		}
		//nodes of the triangle right side
		for (int i = 0; i<SUBDIVISION_FACTOR; i++){
			double x = LINK_LENGTH - ((LINK_LENGTH/SUBDIVISION_FACTOR))*Math.cos(Math.PI/3)*(i+1);
			double y = (LINK_LENGTH/SUBDIVISION_FACTOR)*Math.sin(Math.PI/3)*(i+1);
			Coord coord = scenario.createCoord(x, y);
			Id<Node> id = Id.createNodeId(SUBDIVISION_FACTOR+i+1);

			Node node = scenario.getNetwork().getFactory().createNode(id, coord);
			network.addNode(node);
		}
		//nodes of the triangle left side
		for (int i = 0; i<SUBDIVISION_FACTOR-1; i++){
			double x = LINK_LENGTH/2 - (LINK_LENGTH / SUBDIVISION_FACTOR)*Math.cos(Math.PI/3)*(i+1);
			double y = Math.tan(Math.PI/3)*x;
			Coord coord = scenario.createCoord(x, y);
			Id<Node> id = Id.createNodeId(2*SUBDIVISION_FACTOR+i+1);

			Node node = scenario.getNetwork().getFactory().createNode(id, coord);
			network.addNode(node);
		}
		//additional startNode and endNode for home and work activities
		Coord coord = scenario.createCoord(-50.0, 0.0);
		Node startNode = scenario.getNetwork().getFactory().createNode(Id.createNodeId(-1), coord);
		network.addNode(startNode);
		coord = scenario.createCoord(LINK_LENGTH+50.0, 0.0);
		Id<Node> endNodeId = Id.createNodeId(3*SUBDIVISION_FACTOR);
		Node endNode = scenario.getNetwork().getFactory().createNode(endNodeId, coord);
		network.addNode(endNode);

		// triangle links
		for (int i = 0; i<3*SUBDIVISION_FACTOR; i++){
			Id<Node> idFrom = Id.createNodeId(i);
			Id<Node> idTo;
			if (i != 3*SUBDIVISION_FACTOR-1)
				idTo = Id.createNodeId(i+1);
			else
				idTo = Id.createNodeId(0);
			Node from = network.getNodes().get(idFrom);
			Node to = network.getNodes().get(idTo);

			Link link =scenario.getNetwork().getFactory().createLink(Id.createLinkId(i), from, to);
			link.setCapacity(LINK_CAPACITY);
			link.setFreespeed(FREESPEED/3.6);
			link.setLength(calculateLength(from,to));
			link.setNumberOfLanes(NO_OF_LANES);
			Set<String> allowedModes = new HashSet<>();
			for(String mode : GenerateFundamentalDiagramData.TRAVELMODES){
				allowedModes.add(mode);
			}

			link.setAllowedModes(allowedModes);
			network.addLink(link);
		}
		//additional startLink and endLink for home and work activities
		Id<Link> startLinkId = Id.createLinkId(-1);
		Link startLink = scenario.getNetwork().getFactory().createLink(startLinkId, startNode, scenario.getNetwork().getNodes().get(Id.createNodeId(0)));
		startLink.setCapacity(capMax);
		startLink.setFreespeed(FREESPEED);
		startLink.setLength(25.);
		startLink.setNumberOfLanes(1.);
		network.addLink(startLink);
		Id<Link> endLinkId = Id.createLinkId(3*SUBDIVISION_FACTOR);
		Link endLink = scenario.getNetwork().getFactory().createLink(endLinkId, scenario.getNetwork().getNodes().get(Id.createNodeId(SUBDIVISION_FACTOR)), endNode);
		endLink.setCapacity(capMax);
		endLink.setFreespeed(FREESPEED);
		endLink.setLength(25.);
		endLink.setNumberOfLanes(1.);
		network.addLink(endLink);

		if(GenerateFundamentalDiagramData.writeInputFiles) new NetworkWriter(network).write(outputFolder+"/network.xml");
	}

	private void fillTravelModeData(){
		vehicle2TravelModesData = new HashMap<>();
		for (int i=0; i < GenerateFundamentalDiagramData.TRAVELMODES.length; i++){
			Id<VehicleType> modeId = Id.create(GenerateFundamentalDiagramData.TRAVELMODES[i],VehicleType.class);
			VehicleType vehicleType = VehicleUtils.getFactory().createVehicleType(modeId);
			vehicleType.setPcuEquivalents(MixedTrafficVehiclesUtils.getPCU(GenerateFundamentalDiagramData.TRAVELMODES[i]));
			vehicleType.setMaximumVelocity(MixedTrafficVehiclesUtils.getSpeed(GenerateFundamentalDiagramData.TRAVELMODES[i]));
			VehicleCapacity cap = new VehicleCapacityImpl();
			cap.setSeats(3);//this is default for now, could be improved with mode-dependent vehicle capacity
			vehicleType.setCapacity(cap);
			TravelModesFlowDynamicsUpdator modeData = new TravelModesFlowDynamicsUpdator(vehicleType);
			vehicle2TravelModesData.put(modeId, modeData);
		}
	}

	/**
	 * @param agentDistribution
	 * number of vehicles for each vehicle type
	 * @param gapInSecond
	 */
	void createWantedPopulation(List<Integer> agentDistribution, int gapInSecond){
		GenerateFundamentalDiagramData.log.info("==========Creating population=========");
		Population population = scenario.getPopulation();

		population.getPersons().clear();

		for (int i=0; i<agentDistribution.size(); i++){
			createAndAddPersonToPopulation(agentDistribution.get(i), GenerateFundamentalDiagramData.TRAVELMODES[i]);
		}
		if(GenerateFundamentalDiagramData.writeInputFiles) new PopulationWriter(population,scenario.getNetwork()).write(outputFolder+"/plans.xml.gz");
	}

	/**
	 * @param numberOfAgentOfVehicleType
	 * @param travelMode
	 * <p>
	 * Each agent will leave home randomly in first 15 minutes of simulation.
	 */
	private void createAndAddPersonToPopulation(int numberOfAgentOfVehicleType, String travelMode){
		Population population = scenario.getPopulation();
		for (long i = 0; i<numberOfAgentOfVehicleType; i++){
			int numberOfExistingPersonInPopulation = population.getPersons().size();
			Person person = population.getFactory().createPerson(Id.createPersonId(numberOfExistingPersonInPopulation+1));
			//			Map<String, Object> customMap = person.getCustomAttributes();

			Plan plan = population.getFactory().createPlan();
			plan.addActivity(createHome());

			//			customMap.put("transportMode", travelMode);
			Leg leg = population.getFactory().createLeg(travelMode);

			final Id<Link> startLinkId = Id.createLinkId(-1);
			final Id<Link> endLinkId = Id.createLinkId(3*SUBDIVISION_FACTOR);
			List<Id<Link>> routeDescription = new ArrayList<Id<Link>>();
			for (long k=0; k<3*SUBDIVISION_FACTOR;k++){
				routeDescription.add(Id.createLinkId(k));
			}
			NetworkRoute route = new LinkNetworkRouteImpl(startLinkId, endLinkId);
			route.setLinkIds(startLinkId, routeDescription, endLinkId);
			leg.setRoute(route);

			plan.addLeg(leg);
			plan.addActivity(createWork());

			person.addPlan(plan);
			population.addPerson(person);
		}
	}
	Scenario getScenario(){
		return scenario;
	}
	
	Map<Id<VehicleType>, TravelModesFlowDynamicsUpdator> getTravelMode2FlowDynamicsData(){
		return vehicle2TravelModesData;
	}
	
	private Activity createHome(){
		Id<Link> homeLinkId = Id.createLinkId(-1);
		Activity activity = scenario.getPopulation().getFactory().createActivityFromLinkId("home", homeLinkId);
		Random r = new Random();
		double endTime = r.nextDouble() * 900; 
		activity.setEndTime(endTime);
		return activity;
	}

	private Activity createWork(){
		Id<Link> workLinkId = Id.createLinkId(3*SUBDIVISION_FACTOR);
		Activity activity = scenario.getPopulation().getFactory().createActivityFromLinkId("work", workLinkId);
		return activity;
	}

	private double calculateLength(Node from, Node to){
		double x1 = from.getCoord().getX();
		double y1 = from.getCoord().getY();
		double x2 = to.getCoord().getX();
		double y2 = to.getCoord().getY();
		return Math.sqrt((x2-x1)*(x2-x1)+(y2-y1)*(y2-y1));
	}
}