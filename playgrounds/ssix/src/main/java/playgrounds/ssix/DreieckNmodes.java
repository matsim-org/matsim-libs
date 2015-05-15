/* *********************************************************************** *
 * project: org.matsim.*
 * DreieckNModes													   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playgrounds.ssix;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup.TrafficDynamics;
import org.matsim.core.config.groups.VspExperimentalConfigGroup.VspDefaultsCheckingLevel;
import org.matsim.core.config.groups.VspExperimentalConfigGroup;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.algorithms.EventWriterXML;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.core.mobsim.qsim.ActivityEngine;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.TeleportationEngine;
import org.matsim.core.mobsim.qsim.agents.AgentFactory;
import org.matsim.core.mobsim.qsim.agents.PopulationAgentSource;
import org.matsim.core.mobsim.qsim.changeeventsengine.NetworkChangeEventsEngine;
import org.matsim.core.mobsim.qsim.interfaces.MobsimVehicle;
import org.matsim.core.mobsim.qsim.interfaces.Netsim;
import org.matsim.core.mobsim.qsim.qnetsimengine.*;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vehicles.*;

import java.io.IOException;
import java.io.PrintStream;
import java.util.*;


/**
 * @author ssix
 */

public class DreieckNmodes {

	private static final Logger log = Logger.getLogger(DreieckNmodes.class);
	
	//CONFIGURATION: static variables used for aggregating configuration options
	public final static int subdivisionFactor = 1;//all sides of the triangle will be divided into subdivisionFactor links
	public final static double length = 1000.;//in m, length of one the triangle sides.
	public final static int NETWORK_CAPACITY = 2700;//in PCU/h
	public final static double END_TIME = 14*3600;
	public final static boolean PASSING_ALLOWED = true;
	private final static String OUTPUT_DIR = "./output/data_carTruck_withHoles.txt";
	private final static String OUTPUT_EVENTS = "./output/events_carTruck_withHoles.xml";
	
	private final static double FREESPEED = 60.;						//in km/h, maximum authorized velocity on the track
	public final static int NUMBER_OF_MEMORIZED_FLOWS = 10;
	public final static int NUMBER_OF_MODES = 2;
	public final static String[] NAMES= {"car", "trucks"};	//identification of the different modes
	public final static Double[] Probabilities = {0.5, 0.5}; //modal split
	public final static Double[] Pcus = {1., 3.}; 			//PCUs of the different possible modes
	public final static Double[] Speeds = {16.67, 8.33};		//maximum velocities of the vehicle types, in m/s
	private final static Integer[] startingPoint = {0, 0};

	private static class MyRoundAndRoundAgent implements MobsimDriverAgent{
		
		private MyPersonDriverAgentImpl delegate;
		public boolean goHome;
		
		public MyRoundAndRoundAgent(Person p, Plan unmodifiablePlan, QSim qSim) {
			this.delegate = new MyPersonDriverAgentImpl(p, PopulationUtils.unmodifiablePlan(p.getSelectedPlan()), qSim);
			this.goHome = false;//false at start, modified when all data is extracted.
			}

		@Override
		public final void endActivityAndComputeNextState(double now) {
			delegate.endActivityAndComputeNextState(now);
		}

		@Override
		public final void endLegAndComputeNextState(double now) {
			delegate.endLegAndComputeNextState(now);
		}

		@Override
		public final void setStateToAbort(double now) {
			delegate.setStateToAbort(now);
		}

		@Override
		public boolean equals(Object obj) {
			return delegate.equals(obj);
		}

		@Override
		public final double getActivityEndTime() {
			return delegate.getActivityEndTime();
		}

		@Override
		public final Id<Link> getCurrentLinkId() {
			return delegate.getCurrentLinkId();
		}

		@Override
		public final Double getExpectedTravelTime() {
			return delegate.getExpectedTravelTime();
		}

        @Override
        public Double getExpectedTravelDistance() {
            return delegate.getExpectedTravelDistance();
        }

        @Override
		public final String getMode() {
			return delegate.getMode();
		}

		@Override
		public final Id<Link> getDestinationLinkId() {
			return delegate.getDestinationLinkId();
		}

		@Override
		public final Id<Person> getId() {
			return delegate.getId();
		}

		@Override
		public State getState() {
			return delegate.getState();
		}

		@Override
		public final void notifyArrivalOnLinkByNonNetworkMode(Id<Link> linkId) {
			delegate.notifyArrivalOnLinkByNonNetworkMode(linkId);
		}

		@Override
		public Id<Link> chooseNextLinkId() {
			if (DreieckNmodes.funfunfun.isPermanent()){ goHome = true; }
			
			Id<Link> forcedLeftTurnLinkId = Id.create(3*DreieckNmodes.subdivisionFactor - 1, Link.class);
			Id<Link> forcedStraightForwardLinkId = Id.create(DreieckNmodes.subdivisionFactor - 1, Link.class);
			
			if (!(delegate.getCurrentLinkId().equals(forcedLeftTurnLinkId))){ // not 3n-1
				if (!(delegate.getCurrentLinkId().equals(forcedStraightForwardLinkId))){ //not 3n-1 and not n-1
					return delegate.chooseNextLinkId();
				} else { // n-1
					if (!(goHome)){ //n-1 but not goHome
						return delegate.chooseNextLinkId();
					} else { //n-1 and goHome
						Id<Link> afterGoingStraightForwardLinkId = Id.create(3*DreieckNmodes.subdivisionFactor, Link.class);
						delegate.setCachedNextLinkId(afterGoingStraightForwardLinkId);
						delegate.setCurrentLinkIdIndex(3*DreieckNmodes.subdivisionFactor);//This does work quite well so far and allows to end simulation.
						return afterGoingStraightForwardLinkId;
					}
				}
			} else { //3n-1
				Id<Link> afterLeftTurnLinkId = Id.create(0, Link.class);
				delegate.setCachedNextLinkId(afterLeftTurnLinkId);
				delegate.setCurrentLinkIdIndex(0);
				return afterLeftTurnLinkId;
			}
		}

		@Override
		public void notifyMoveOverNode(Id<Link> newLinkId) {
			delegate.notifyMoveOverNode(newLinkId);
		}

		@Override
		public void setVehicle(MobsimVehicle veh) {
			delegate.setVehicle(veh);
		}

		@Override
		public MobsimVehicle getVehicle() {
			return delegate.getVehicle();
		}

		@Override
		public Id<Vehicle> getPlannedVehicleId() {
			return delegate.getPlannedVehicleId();
		}
		@Override
		public boolean isWantingToArriveOnCurrentLink() {
			// The following is the old condition: Being at the end of the plan means you arrive anyways, no matter if you are on the right or wrong link.
			// kai, nov'14
			if ( this.chooseNextLinkId()==null ) {
				return true ;
			} else {
				return false ;
			}
		}

	}

	private static class MyAgentFactory implements AgentFactory {

		private QSim qSim;

		public MyAgentFactory(QSim qSim) {
		this.qSim = qSim;
		}

		@Override
		public MobsimAgent createMobsimAgentFromPerson(Person p) {
			MyRoundAndRoundAgent agent = new MyRoundAndRoundAgent(p, PopulationUtils.unmodifiablePlan(p.getSelectedPlan()), this.qSim);
			return agent;
		}

	}
	
	private PrintStream writer;
	private Scenario scenario;
	private static FundamentalDiagramsNmodes funfunfun;
	private Map<String, ModeData> modesData;
		
	
	public DreieckNmodes(int networkCapacity){
		//Checking that configuration data has the appropriate size:
		if (NAMES.length != NUMBER_OF_MODES){ throw new RuntimeException("There should be "+NUMBER_OF_MODES+" names for the different modes. Check your static variable NAMES!");}
		if (Probabilities.length != NUMBER_OF_MODES){ throw new RuntimeException("There should be "+NUMBER_OF_MODES+" probabilities for the different modes. Check your static variable Probabilities!");}
		if (Pcus.length != NUMBER_OF_MODES){ throw new RuntimeException("There should be "+NUMBER_OF_MODES+" PCUs for the different modes. Check your static variable Pcus!");}
		if (Speeds.length != NUMBER_OF_MODES){ throw new RuntimeException("There should be "+NUMBER_OF_MODES+" speeds for the different modes. Check your static variable Speeds!");}
		
		//Initializing scenario Config file
		Config config = ConfigUtils.createConfig();
		//config.addQSimConfigGroup(new QSimConfigGroup());
		config.qsim().setSnapshotStyle(QSimConfigGroup.SNAPSHOT_AS_QUEUE) ;
		config.qsim().setMainModes(Arrays.asList(NAMES));
		config.qsim().setStuckTime(100*3600.);//allows to overcome maximal density regime
		config.qsim().setEndTime(14*3600);//allows to set agents to abort after getting the wanted data.
		config.qsim().setTrafficDynamics(TrafficDynamics.withHoles);
		//todo: is for actual network configurations correct, needs dependency on bigger network length 
		config.vspExperimental().setVspDefaultsCheckingLevel( VspDefaultsCheckingLevel.abort );
		// this may lead to abort during execution.  In such cases, please fix the configuration.  if necessary, talk
		// to me (kn).
		this.scenario = ScenarioUtils.createScenario(config);
		ConfigWriter cWriter = new ConfigWriter(config);
		//cWriter.write("Z:\\WinHome\\Desktop\\workspace2\\playgrounds\\ssix\\output\\config.xml");
		
		//Initializing modeData objects//TODO: should be initialized when instancing FundamentalDiagrams, no workaround still found
		//Need to be currently initialized at this point to initialize output and modified QSim
		this.modesData = new HashMap<>();
		for (int i=0; i < NUMBER_OF_MODES; i++){
			String modeId = NAMES[i];
			VehicleType vehicleType = VehicleUtils.getFactory().createVehicleType(Id.create(modeId, VehicleType.class));
			vehicleType.setPcuEquivalents(Pcus[i]);
			vehicleType.setMaximumVelocity(Speeds[i]);
			VehicleCapacity cap = new VehicleCapacityImpl();
			cap.setSeats(3);//this is default for now, could be improved with mode-dependent vehicle capacity
			vehicleType.setCapacity(cap);
			ModeData modeData = new ModeData(modeId, vehicleType);
			this.modesData.put(modeId, modeData);
		}
	}
	
	public static void main(String[] args) {
		DreieckNmodes dreieck = new DreieckNmodes(NETWORK_CAPACITY);
		dreieck.fillNetworkData();
		dreieck.openFile(OUTPUT_DIR);
		
		
		dreieck.parametricRunAccordingToGivenModalSplit();
		//dreieck.parametricRunAccordingToDistribution(Arrays.asList(MaxAgentDistribution), Arrays.asList(Steps));
		//dreieck.singleRun(Arrays.asList(TEST_DISTRIBUTION));
		
		
		dreieck.closeFile();
	}
	
	private void parametricRunAccordingToDistribution(List<Integer> maxAgentDistribution, List<Integer> steps){
		//Check for size
		if ((maxAgentDistribution.size() != NUMBER_OF_MODES) || (steps.size() != NUMBER_OF_MODES)){ throw new RuntimeException("There should be as many maxValues and/or steps in the two given lists as there are modes in the simulation.");}
		
		List<List<Integer>> pointsToRun = this.createPointsToRun(maxAgentDistribution, steps);
		System.out.println(pointsToRun);
		for ( int i=0; i<pointsToRun.size(); i++){
			List<Integer> pointToRun = pointsToRun.get(i);
			System.out.println("Going into run "+pointToRun);
			this.singleRun(pointToRun);
		}
	}
	
	private void parametricRunAccordingToGivenModalSplit(){
		//NB: Due to the strict programming here, in some cases (numbers in Pcu or Probabilities prime to each other),
		//NB: this might give out only a few cases, which is not very interesting.
		//NB: Might be therefore useful to implement a more "permissive" programming.
		
		//Creating minimal configuration respecting modal split and integer agent numbers
		/* METHOD 1: mathematical
		List<Double> pcus = Arrays.asList(DreieckNmodes.Pcus);
		List<Integer> minSteps = new ArrayList<Integer>();
		for (double prob : Arrays.asList(DreieckNmodes.Probabilities)){
			minSteps.add(new Integer((int) (prob*100)));
		}
		int multiplier = 1;
		for (int i=0; i<DreieckNmodes.NUMBER_OF_MODES; i++){
			double pcu = pcus.get(i); 
			if ((pcu>1) && ((minSteps.get(i))%pcu != 0)){
				double ppcm = ppcm((int) pcu, minSteps.get(i));
				multiplier *= ppcm/minSteps.get(i);
			}
		}
		for (int i=0; i<DreieckNmodes.NUMBER_OF_MODES; i++){
			minSteps.set(i, (int) (minSteps.get(i)*multiplier/DreieckNmodes.Pcus[i]));
		}
		int pgcd = pgcd(minSteps);
		for (int i=0; i<DreieckNmodes.NUMBER_OF_MODES; i++){
			minSteps.set(i, minSteps.get(i)/pgcd);
		}*/
		//METHOD 2: setting it directly
		List<Integer> minSteps = new ArrayList<Integer>();
		minSteps.add(new Integer(3));
		minSteps.add(new Integer(1));
		
		//Deducing all possible points to run by multiplying smallest configuration until reaching max storage capacity
		//METHOD 1: mathematical
		/*
		int iterationStep = 0;
		int maxPCUcount = (int) (0.150 * DreieckNmodes.length * 3) ;
		double pcuPerAgent = 0.;
		for (int i=0; i<DreieckNmodes.NUMBER_OF_MODES; i++){
			iterationStep += minSteps.get(i);
			pcuPerAgent += DreieckNmodes.Probabilities[i] * DreieckNmodes.Pcus[i];
		}
		List<List<Integer>> pointsToRun = new ArrayList<List<Integer>>();
		int numberOfPoints = (int) ((maxPCUcount/pcuPerAgent)/iterationStep) + 20;//TODO still nowhere near the expected number of points
		for (int m=0; m<numberOfPoints; m++){
			List<Integer> pointToRun = new ArrayList<Integer>();
			for (int i=0; i<DreieckNmodes.NUMBER_OF_MODES; i++){
				pointToRun.add(minSteps.get(i)*m);
			}
			System.out.println(pointToRun);
			pointsToRun.add(pointToRun);//Could run directly here, not done for better code overview
		}
		System.out.println(pointsToRun);
		*/
		//METHOD 2: setting it directly
		List<List<Integer>> pointsToRun = new ArrayList<List<Integer>>();
		int numberOfPoints = 75;
		for (int m=0; m<numberOfPoints; m++){
			List<Integer> pointToRun = new ArrayList<Integer>();
			for (int i=0; i<DreieckNmodes.NUMBER_OF_MODES; i++){
				pointToRun.add(minSteps.get(i)*m);
			}
			System.out.println(pointToRun);
			pointsToRun.add(pointToRun);//Could run directly here, not done for better code overview
		}
		
		//Effective iteration over all points 
		for ( int i=0; i<pointsToRun.size(); i++){
			List<Integer> pointToRun = pointsToRun.get(i);
			System.out.println("Going into run "+pointToRun);
			this.singleRun(pointToRun);
		}
	}
	
	private List<List<Integer>> createPointsToRun(List<Integer> maxValues, List<Integer> steps) {
		//calculate number of points and creating starting point:
		int numberOfPoints = 1; 
		//TODO: set this back. Integer[] startingPoint = new Integer[maxValues.size()];
		//for (int i=0; i<maxValues.size(); i++){
		//	numberOfPoints *=  ( (maxValues.get(i).intValue() / steps.get(i).intValue()) + 1);
		//	startingPoint[i] = new Integer(0);
		//}
		numberOfPoints = 226;
		//Actually going through the n-dimensional grid
		BinaryAdditionModule iterationModule = new BinaryAdditionModule(maxValues, steps, startingPoint);
		List<List<Integer>> pointsToRun = new ArrayList<List<Integer>>();
		for (int i=0; i<numberOfPoints; i++){
			Integer[] newPoint = new Integer[maxValues.size()];
			for (int j=0; j<maxValues.size(); j++){
				newPoint[j] = (iterationModule.getPoint())[j];
			}
			pointsToRun.add(Arrays.asList(newPoint));
			String point = Arraytostring(iterationModule.getPoint());
			System.out.println("Just added point "+point+"to the collection.");
			if (i<numberOfPoints-1){
				iterationModule.add1();
			}
		}
		//System.out.println(pointsToRun.size());
		return pointsToRun;
	}
	
	private void singleRun(List<Integer> pointToRun) {
		this.createWantedPopulation(pointToRun, 2);
		for (int i=0; i<NAMES.length; i++){
			this.modesData.get(NAMES[i]).setnumberOfAgents(pointToRun.get(i));
		}
		
		EventsManager events = EventsUtils.createEventsManager();
		
		FundamentalDiagramsNmodes fundiN = new FundamentalDiagramsNmodes(this.scenario, this.modesData);
		this.modesData = fundiN.getModesData();
		DreieckNmodes.funfunfun = fundiN;
		events.addHandler(fundiN);
		events.addHandler(new EventWriterXML(OUTPUT_EVENTS));
		
		Netsim qSim = createModifiedQSim(this.scenario, events);
		
		qSim.run();
		
		//writer.doSomething
		writer.format("%d\t\t",fundiN.getGlobalData().numberOfAgents);
		for (int i=0; i < NUMBER_OF_MODES; i++){
			writer.format("%d\t", this.modesData.get(NAMES[i]).numberOfAgents);
		}
		writer.print("\t");
		writer.format("%.2f\t", fundiN.getGlobalData().getPermanentDensity());
		for (int i=0; i < NUMBER_OF_MODES; i++){
			writer.format("%.2f\t", this.modesData.get(NAMES[i]).getPermanentDensity());
		}
		writer.print("\t");
		writer.format("%.2f\t", fundiN.getGlobalData().getPermanentFlow());
		for (int i=0; i < NUMBER_OF_MODES; i++){
			writer.format("%.2f\t", this.modesData.get(NAMES[i]).getPermanentFlow());
		}
		writer.print("\t");
		writer.format("%.2f\t", fundiN.getGlobalData().getPermanentAverageVelocity());
		for (int i=0; i < NUMBER_OF_MODES; i++){
			writer.format("%.2f\t", this.modesData.get(NAMES[i]).getPermanentAverageVelocity());
		}
		writer.print("\n");
	}

	private void fillNetworkData(){
		Network network = scenario.getNetwork();
		int capMax = 100*DreieckNmodes.NETWORK_CAPACITY;
		
		//NODES
		//nodes of the triangle base
		for (int i = 0; i<subdivisionFactor+1; i++){
			double x=0, y=0;
			if (i>0){
				for (int j=0; j<i; j++){
					x += DreieckNmodes.length/DreieckNmodes.subdivisionFactor;
				}
			}
			Coord coord = scenario.createCoord(x, y);
			Id<Node> id = Id.create(i, Node.class);
			
			Node node = scenario.getNetwork().getFactory().createNode(id, coord);
			network.addNode(node);	
		}
		//nodes of the triangle right side
		for (int i = 0; i<DreieckNmodes.subdivisionFactor; i++){
			double x = DreieckNmodes.length - (DreieckNmodes.length/(2*DreieckNmodes.subdivisionFactor))*(i+1);
			double y = Math.sqrt(3.)*(DreieckNmodes.length-x);
			Coord coord = scenario.createCoord(x, y);
			Id<Node> id = Id.create(DreieckNmodes.subdivisionFactor+i+1, Node.class);
			
			Node node = scenario.getNetwork().getFactory().createNode(id, coord);
			network.addNode(node);
		}
		//nodes of the triangle left side
		for (int i = 0; i<DreieckNmodes.subdivisionFactor-1; i++){
			double x = DreieckNmodes.length/2 - (DreieckNmodes.length/(2*DreieckNmodes.subdivisionFactor))*(i+1);
			double y = Math.sqrt(3.)*x;
			Coord coord = scenario.createCoord(x, y);
			Id<Node> id = Id.create(2*DreieckNmodes.subdivisionFactor+i+1, Node.class);
			
			Node node = scenario.getNetwork().getFactory().createNode(id, coord);
			network.addNode(node);
		}
		//additional startNode and endNode for home and work activities
		Coord coord = scenario.createCoord(-50.0, 0.0);
		Id<Node> startId = Id.create(-1, Node.class);
		Node startNode = scenario.getNetwork().getFactory().createNode(startId, coord);
		network.addNode(startNode);
		coord = scenario.createCoord(DreieckNmodes.length+50.0, 0.0);
		Id<Node> endId = Id.create(3*DreieckNmodes.subdivisionFactor, Node.class);
		Node endNode = scenario.getNetwork().getFactory().createNode(endId, coord);
		network.addNode(endNode);
		
		//LINKS
		//all triangle links
		for (int i = 0; i<3*DreieckNmodes.subdivisionFactor; i++){
			Id<Node> idFrom = Id.create(i, Node.class);
			Id<Node> idTo;
			if (i != 3*DreieckNmodes.subdivisionFactor-1)
				idTo = Id.create(i+1, Node.class);
			else
				idTo = Id.create(0, Node.class);
			Node from = network.getNodes().get(idFrom);
			Node to = network.getNodes().get(idTo);
			
			Link link = this.scenario.getNetwork().getFactory().createLink(Id.create(idFrom, Link.class), from, to);
			link.setCapacity(DreieckNmodes.NETWORK_CAPACITY);
			link.setFreespeed(DreieckNmodes.FREESPEED/3.6);
			link.setLength(calculateLength(from,to));
			link.setNumberOfLanes(1.);
			network.addLink(link);
		}
		//additional startLink and endLink for home and work activities
		Link startLink = this.scenario.getNetwork().getFactory().createLink(Id.create(startId, Link.class), startNode, this.scenario.getNetwork().getNodes().get(Id.create(0, Node.class)));
		startLink.setCapacity(capMax);
		startLink.setFreespeed(DreieckNmodes.FREESPEED/3.6);
		startLink.setLength(25.);
		startLink.setNumberOfLanes(2.);
		network.addLink(startLink);
		Link endLink = this.scenario.getNetwork().getFactory().createLink(Id.create(endId, Link.class), this.scenario.getNetwork().getNodes().get(Id.create(DreieckNmodes.subdivisionFactor, Node.class)), endNode);
		endLink.setCapacity(capMax);
		endLink.setFreespeed(DreieckNmodes.FREESPEED/3.6);
		endLink.setLength(25.);
		endLink.setNumberOfLanes(1.);
		network.addLink(endLink);
		
		//check with .xml and Visualizer
		NetworkWriter writer = new NetworkWriter(network);
		writer.write("./output/dreieck_network.xml");
	}
	
	private void createWantedPopulation(List<Integer> agentDistribution, int sekundenAbstand){
		Population population = scenario.getPopulation();
		
		population.getPersons().clear();
		
		long numberOfPeople = 0;
		for (int i=0; i<agentDistribution.size(); i++){
			numberOfPeople += agentDistribution.get(i);
		}
		
		for (long i = 0; i<numberOfPeople; i++){
			
			Person person = population.getFactory().createPerson(Id.create(i+1, Person.class));
			Map<String, Object> customMap = person.getCustomAttributes();
			
			Plan plan = population.getFactory().createPlan();
			plan.addActivity(createHome(sekundenAbstand, numberOfPeople));
			
			//Assigning mode
			String transportMode="";
			boolean modeFound = false;
			int j=0; int sum=0;
			while (!(modeFound)){
				sum += agentDistribution.get(j);
				if (sum>i){
					transportMode = DreieckNmodes.NAMES[j];
					modeFound = true;
					//System.out.println("A "+DreieckNmodes.NAMES[j]+" was made.");
				}
				j++;
			}
			customMap.put("transportMode", transportMode);
			Leg leg = population.getFactory().createLeg(transportMode);
			
			//Handy route definition for making the agents stay on the track
			final Id<Link> startLinkId = Id.create(-1, Link.class);
			final Id<Link> endLinkId = Id.create(3*subdivisionFactor, Link.class);
			List<Id<Link>> routeDescription = new ArrayList<Id<Link>>();
			for (long k=0; k<3*subdivisionFactor;k++){
				routeDescription.add(Id.create(k, Link.class));
			}
			NetworkRoute route = new LinkNetworkRouteImpl(startLinkId, endLinkId);
			route.setLinkIds(startLinkId, routeDescription, endLinkId);
			leg.setRoute(route);
			//end of route definition
			plan.addLeg(leg);
			plan.addActivity(createWork());
			
			person.addPlan(plan);
			population.addPerson(person);
			//System.out.println("Just added person : "+(i+1)+" to the scenario population.");
		}
	}
	
	private Netsim createModifiedQSim(Scenario sc, EventsManager eventsManager) {
		//From QSimFactory inspired code
		QSimConfigGroup conf = sc.getConfig().qsim();
        if (conf == null) {
            throw new NullPointerException("There is no configuration set for the QSim. Please add the module 'qsim' to your config file.");
        }
        /**/
        QSim qSim = new QSim(sc, eventsManager);
        ActivityEngine activityEngine = new ActivityEngine(eventsManager, qSim.getAgentCounter());
		qSim.addMobsimEngine(activityEngine);
		qSim.addActivityHandler(activityEngine);
		
		//First modification: Mobsim needs to create queue links with mzilske's passing queue
        NetsimNetworkFactory<QNode, QLinkImpl> netsimNetworkFactory = new NetsimNetworkFactory<QNode, QLinkImpl>() {

            @Override
            public QLinkImpl createNetsimLink(final Link link, final QNetwork network, final QNode toQueueNode) {
                if (PASSING_ALLOWED){
                    return new QLinkImpl(link, network, toQueueNode, new MZilskePassingVehicleQ());
                } else {
                    return new QLinkImpl(link, network, toQueueNode, new FIFOVehicleQ());
                }
            }

            @Override
            public QNode createNetsimNode(final Node node, QNetwork network) {
                return new QNode(node, network);
            }


        };
        QNetsimEngine netsimEngine = new QNetsimEngine(qSim, netsimNetworkFactory);
		////////////////////////////////////////////////////////
		
		qSim.addMobsimEngine(netsimEngine);
		qSim.addDepartureHandler(netsimEngine.getDepartureHandler());
		TeleportationEngine teleportationEngine = new TeleportationEngine(scenario, eventsManager);
		qSim.addMobsimEngine(teleportationEngine);
        
		//Second modification: Mobsim needs to create my own used agents
		AgentFactory agentFactory = new MyAgentFactory(qSim);
		///////////////////////////////////////////////////////
		
		if (sc.getConfig().network().isTimeVariantNetwork()) {
			qSim.addMobsimEngine(new NetworkChangeEventsEngine());		
		}
        PopulationAgentSource agentSource = new PopulationAgentSource(sc.getPopulation(), agentFactory, qSim);
        
        //Third modification: Mobsim needs to know the different vehicle types (and their respective speeds)
        Map<String, VehicleType> modeVehicleTypes = new HashMap<String, VehicleType>();
        for (int i=0; i<modesData.size(); i++){
        	String id = modesData.get(NAMES[i]).getModeId();
        	VehicleType vT = modesData.get(NAMES[i]).getVehicleType();
        	modeVehicleTypes.put(id, vT);
        }
        agentSource.setModeVehicleTypes(modeVehicleTypes);
        //////////////////////////////////////////////////////
        
        qSim.addAgentSource(agentSource);
        return qSim;
	}
	
	private void openFile(String dir) {
		try {
			writer = new PrintStream(dir);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		writer.print("n\t");
		for (int i=0; i < NUMBER_OF_MODES; i++){
			String str = this.modesData.get(NAMES[i]).getModeId().substring(0, 3);
			String strn = "n_"+str;
			writer.print(strn+"\t");
		}
		writer.print("\tk\t");
		for (int i=0; i < NUMBER_OF_MODES; i++){
			String str = this.modesData.get(NAMES[i]).getModeId().substring(0, 3);
			String strk = "k_"+str;
			writer.print(strk+"\t");
		}
		writer.print("\tq\t");
		for (int i=0; i < NUMBER_OF_MODES; i++){
			String str = this.modesData.get(NAMES[i]).getModeId().substring(0, 3);
			String strq = "q_"+str;
			writer.print(strq+"\t");
		}
		writer.print("\tv\t");
		for (int i=0; i < NUMBER_OF_MODES; i++){
			String str = this.modesData.get(NAMES[i]).getModeId().substring(0, 3);
			String strv = "v_"+str;
			writer.print(strv+"\t");
		}
		writer.print("\n");
	}
	
	private void closeFile() {
		writer.close();
	}
	
	private Activity createHome(int sekundenFrequenz, long numberOfPeople){
		Id<Link> homeLinkId = Id.create(-1, Link.class);
		Activity activity = scenario.getPopulation().getFactory().createActivityFromLinkId("home", homeLinkId);
		
		Random r = new Random();
		/*Method 1: The order of leaving people is guaranteed by the minimum time step between people: first person 1 leaves, then 2, then 3 etc...
		long plannedEndTime = 6*3600 + (identifier-1)*sekundenFrequenz;
		double endTime = plannedEndTime - sekundenFrequenz/2.0 + r.nextDouble()*sekundenFrequenz;
		*/
		///*Method 2: With the expected frequency, the maximal departure time is computed and people are randomly departing within this huge time chunk.
		long TimeChunkSize = numberOfPeople * sekundenFrequenz;
		double endTime = /*6 * 3600 + */r.nextDouble() * /*TimeChunkSize*/900; 
		//*/
		//NB:Method 2 is significantly better for the quality of fundamental diagrams;
		activity.setEndTime(endTime);
		
		return activity;
	}
	
	private Activity createWork(){
		Id<Link> workLinkId = Id.create(3*DreieckNmodes.subdivisionFactor, Link.class);
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

	///*
	private static String Arraytostring(Integer[] list){
		int n = list.length;
		String str = "";
		for (Integer aList : list) {
			str += aList;
			str += " ";
		}
		return str;
	}//*/

}