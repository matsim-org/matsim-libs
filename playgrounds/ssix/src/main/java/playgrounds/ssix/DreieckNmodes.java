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

import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.apache.log4j.Logger;
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
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup;
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
import org.matsim.core.mobsim.qsim.qnetsimengine.FIFOVehicleQ;
import org.matsim.core.mobsim.qsim.qnetsimengine.NetsimNetworkFactory;
import org.matsim.core.mobsim.qsim.qnetsimengine.QLinkImpl;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetsimEngine;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetsimEngineFactory;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetwork;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNode;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.PopulationUtils;
import org.matsim.vehicles.VehicleCapacity;
import org.matsim.vehicles.VehicleCapacityImpl;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
//import org.apache.log4j.Logger;

/**
 * @author ssix
 */

public class DreieckNmodes {
	
	private static Integer[] TEST_DISTRIBUTION = {8,4,1};

	private static final Logger log = Logger.getLogger(DreieckNmodes.class);
	
	private static class MyRoundAndRoundAgent implements MobsimDriverAgent{
		
		private MyPersonDriverAgentImpl delegate;
		public boolean goHome;
		
		public MyRoundAndRoundAgent(Person p, Plan unmodifiablePlan, QSim qSim) {
			this.delegate = new MyPersonDriverAgentImpl(p, PopulationUtils.unmodifiablePlan(p.getSelectedPlan()), qSim);
			this.goHome = false;//false at start, modified when all data is extracted.
			}

		public final void endActivityAndComputeNextState(double now) {
			delegate.endActivityAndComputeNextState(now);
		}

		public final void endLegAndComputeNextState(double now) {
			delegate.endLegAndComputeNextState(now);
		}

		public final void abort(double now) {
			delegate.abort(now);
		}

		public boolean equals(Object obj) {
			return delegate.equals(obj);
		}

		public final double getActivityEndTime() {
			return delegate.getActivityEndTime();
		}

		public final Id getCurrentLinkId() {
			return delegate.getCurrentLinkId();
		}

		public final Double getExpectedTravelTime() {
			return delegate.getExpectedTravelTime();
		}

		public final String getMode() {
			return delegate.getMode();
		}

		public final Id getDestinationLinkId() {
			return delegate.getDestinationLinkId();
		}

		public final Id getId() {
			return delegate.getId();
		}

		public State getState() {
			return delegate.getState();
		}

		public final void notifyArrivalOnLinkByNonNetworkMode(Id linkId) {
			delegate.notifyArrivalOnLinkByNonNetworkMode(linkId);
		}

		@Override
		public Id chooseNextLinkId() {
			if (DreieckNmodes.funfunfun.isPermanent()){ goHome = true; }
			
			Id forcedLeftTurnLinkId = new IdImpl((long)(3*DreieckNmodes.subdivisionFactor - 1));
			Id forcedStraightForwardLinkId = new IdImpl((long)(DreieckNmodes.subdivisionFactor - 1));
			
			if (!(delegate.getCurrentLinkId().equals(forcedLeftTurnLinkId))){ // not 3n-1
				if (!(delegate.getCurrentLinkId().equals(forcedStraightForwardLinkId))){ //not 3n-1 and not n-1
					return delegate.chooseNextLinkId();
				} else { // n-1
					if (!(goHome)){ //n-1 but not goHome
						return delegate.chooseNextLinkId();
					} else { //n-1 and goHome
						Id afterGoingStraightForwardLinkId = new IdImpl((long)(3*DreieckNmodes.subdivisionFactor));
						delegate.setCachedNextLinkId(afterGoingStraightForwardLinkId);
						delegate.setCurrentLinkIdIndex(3*DreieckNmodes.subdivisionFactor);//This does work quite well so far and allows to end simulation.
						return afterGoingStraightForwardLinkId;
					}
				}
			} else { //3n-1
				Id afterLeftTurnLinkId = new IdImpl((long)(0));
				delegate.setCachedNextLinkId(afterLeftTurnLinkId);
				delegate.setCurrentLinkIdIndex(0);
				return afterLeftTurnLinkId;
			}
		}

		@Override
		public void notifyMoveOverNode(Id newLinkId) {
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
		public Id getPlannedVehicleId() {
			return delegate.getPlannedVehicleId();
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
	
	//CONFIGURATION: static variables used for aggregating configuration options
	public static int subdivisionFactor=3;//all sides of the triangle will be divided into subdivisionFactor links
	public static double length = 333.33;//in m, length of one the triangle sides.
	public static int NETWORK_CAPACITY = 2700;//in PCU/h
	public static boolean PASSING_ALLOWED = true;
	private static String OUTPUT_DIR = "Z:\\WinHome\\Desktop\\workspace2\\playgrounds\\ssix\\output\\data_test.txt";
	private static String OUTPUT_EVENTS = "Z:\\WinHome\\Desktop\\workspace2\\playgrounds\\ssix\\output\\events_test.xml";
	
	private static double FREESPEED = 60.;						//in km/h, maximum authorized velocity on the track
	public static int NUMBER_OF_MEMORIZED_FLOWS = 5;
	public static int NUMBER_OF_MODES = 3;
	public static String[] NAMES= {"bicycles","motorbikes","cars"};	//identification of the different modes
	public static Double[] Probabilities = {1/3., 1/3., 1/3.}; //modal split
	public static Double[] Pcus = {0.25, 0.25, 1.}; 			//PCUs of the different possible modes
	public static Double[] Speeds = {4.17, 16.67, 16.67};		//maximum velocities of the vehicle types, in m/s
	private static Integer[] MaxAgentDistribution = {1,1,1};
	private static Integer[] Steps = {1,1,1};
	
	private PrintStream writer;
	private Scenario scenario;
	private static FundamentalDiagramsNmodes funfunfun;
	private Map<Id, ModeData> modesData;
	private int networkCapacity;//the capacity all links of the network will have
		
	
	public DreieckNmodes(int networkCapacity){
		this.networkCapacity = networkCapacity;
		
		//Checking that configuration data has the appropriate size:
		if (NAMES.length != NUMBER_OF_MODES){	throw new RuntimeException("There should be "+NUMBER_OF_MODES+" names for the different modes. Check your static variable NAMES!");}
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
		//todo: is for actual network configurations correct, needs dependency on bigger network length 
		config.vspExperimental().addParam("vspDefaultsCheckingLevel", VspExperimentalConfigGroup.ABORT) ;
		// this may lead to abort during execution.  In such cases, please fix the configuration.  if necessary, talk
		// to me (kn).
		this.scenario = ScenarioUtils.createScenario(config);
		
		//Initializing modeData objects//TODO: should be initialized when instancing FundamentalDiagrams, no workaround still found
		//Need to be currently initialized at this point to initialize output and modified QSim
		this.modesData = new HashMap<Id, ModeData>();
		for (int i=0; i < NUMBER_OF_MODES; i++){
			Id modeId = new IdImpl(NAMES[i]);
			VehicleType vehicleType = VehicleUtils.getFactory().createVehicleType(modeId);
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
		//dreieck.parametricRunAccordingToDistribution(Arrays.asList(MaxAgentDistribution), Arrays.asList(Steps));
		dreieck.singleRun(Arrays.asList(TEST_DISTRIBUTION));//TODO: debug this case so that it returns non null values.
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
	
	private List<List<Integer>> createPointsToRun(List<Integer> maxValues, List<Integer> steps) {
		//calculate number of points and creating starting point:
		int numberOfPoints = 1; 
		Integer[] startingPoint = new Integer[maxValues.size()];
		for (int i=0; i<maxValues.size(); i++){
			numberOfPoints *=  ( (maxValues.get(i).intValue() / steps.get(i).intValue()) + 1);
			startingPoint[i] = new Integer(0);
		}
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
		System.out.println(pointsToRun.size());
		return pointsToRun;
	}
	
	private void singleRun(List<Integer> pointToRun) {
		this.createWantedPopulation(pointToRun, 2);//ok
		for (int i=0; i<NAMES.length; i++){
			this.modesData.get(new IdImpl(NAMES[i])).setnumberOfAgents(pointToRun.get(i).intValue());//ok
			//System.out.println("Setting "+NAMES[i]+"'s numberOfAgents to "+pointToRun.get(i).intValue());
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
			writer.format("%d\t", this.modesData.get(new IdImpl(NAMES[i])).numberOfAgents);
		}
		writer.print("\t");
		writer.format("%.2f\t", fundiN.getGlobalData().getPermanentDensity());
		for (int i=0; i < NUMBER_OF_MODES; i++){
			writer.format("%.2f\t", this.modesData.get(new IdImpl(NAMES[i])).getPermanentDensity());
		}
		writer.print("\t");
		writer.format("%.2f\t", fundiN.getGlobalData().getPermanentFlow());
		for (int i=0; i < NUMBER_OF_MODES; i++){
			writer.format("%.2f\t", this.modesData.get(new IdImpl(NAMES[i])).getPermanentFlow());
		}
		writer.print("\t");
		writer.format("%.2f\t", fundiN.getGlobalData().getPermanentAverageVelocity());
		for (int i=0; i < NUMBER_OF_MODES; i++){
			writer.format("%.2f\t", this.modesData.get(new IdImpl(NAMES[i])).getPermanentAverageVelocity());
		}
		writer.print("\n");
	}

	private void fillNetworkData(){
		Network network = scenario.getNetwork();
		int capMax = 100*networkCapacity;
		
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
			Id id = new IdImpl((long)i);
			
			Node node = scenario.getNetwork().getFactory().createNode(id, coord);
			network.addNode(node);	
		}
		//nodes of the triangle right side
		for (int i = 0; i<DreieckNmodes.subdivisionFactor; i++){
			double x = DreieckNmodes.length - (DreieckNmodes.length/(2*DreieckNmodes.subdivisionFactor))*(i+1);
			double y = Math.sqrt(3.)*(DreieckNmodes.length-x);
			Coord coord = scenario.createCoord(x, y);
			Id id = new IdImpl((long)(DreieckNmodes.subdivisionFactor+i+1));
			
			Node node = scenario.getNetwork().getFactory().createNode(id, coord);
			network.addNode(node);
		}
		//nodes of the triangle left side
		for (int i = 0; i<DreieckNmodes.subdivisionFactor-1; i++){
			double x = DreieckNmodes.length/2 - (DreieckNmodes.length/(2*DreieckNmodes.subdivisionFactor))*(i+1);
			double y = Math.sqrt(3.)*x;
			Coord coord = scenario.createCoord(x, y);
			Id id = new IdImpl((long)(2*DreieckNmodes.subdivisionFactor+i+1));
			
			Node node = scenario.getNetwork().getFactory().createNode(id, coord);
			network.addNode(node);
		}
		//additional startNode and endNode for home and work activities
		Coord coord = scenario.createCoord(-50.0, 0.0);
		Id startId = new IdImpl(-1);
		Node startNode = scenario.getNetwork().getFactory().createNode(startId, coord);
		network.addNode(startNode);
		coord = scenario.createCoord(DreieckNmodes.length+50.0, 0.0);
		Id endId = new IdImpl(3*DreieckNmodes.subdivisionFactor);
		Node endNode = scenario.getNetwork().getFactory().createNode(endId, coord);
		network.addNode(endNode);
		
		//LINKS
		//all triangle links
		for (int i = 0; i<3*DreieckNmodes.subdivisionFactor; i++){
			Id idFrom = new IdImpl((long)i);
			Id idTo;
			if (i != 3*DreieckNmodes.subdivisionFactor-1)
				idTo = new IdImpl((long)(i+1));
			else
				idTo = new IdImpl(0);
			Node from = network.getNodes().get(idFrom);
			Node to = network.getNodes().get(idTo);
			
			Link link = this.scenario.getNetwork().getFactory().createLink(idFrom, from, to);
			link.setCapacity(this.networkCapacity);
			link.setFreespeed(DreieckNmodes.FREESPEED/3.6);
			link.setLength(calculateLength(from,to));
			link.setNumberOfLanes(1.);
			network.addLink(link);
		}
		//additional startLink and endLink for home and work activities
		Link startLink = this.scenario.getNetwork().getFactory().createLink(startId, startNode, this.scenario.getNetwork().getNodes().get(new IdImpl(0)));
		startLink.setCapacity(capMax);
		startLink.setFreespeed(DreieckNmodes.FREESPEED/3.6);
		startLink.setLength(25.);
		startLink.setNumberOfLanes(2.);
		network.addLink(startLink);
		Link endLink = this.scenario.getNetwork().getFactory().createLink(endId, this.scenario.getNetwork().getNodes().get(new IdImpl(DreieckNmodes.subdivisionFactor)), endNode);
		endLink.setCapacity(capMax);
		endLink.setFreespeed(DreieckNmodes.FREESPEED/3.6);
		endLink.setLength(25.);
		endLink.setNumberOfLanes(1.);
		network.addLink(endLink);
		
		//check with .xml and Visualizer
		//NetworkWriter writer = new NetworkWriter(network);
		//writer.write("./output/dreieck_network.xml");
	}
	
	private void createWantedPopulation(List<Integer> agentDistribution, int sekundenAbstand){
		Population population = scenario.getPopulation();
		
		population.getPersons().clear();
		
		long numberOfPeople = 0;
		for (int i=0; i<agentDistribution.size(); i++){
			numberOfPeople += agentDistribution.get(i);
		}
		
		for (long i = 0; i<numberOfPeople; i++){
			
			Person person = population.getFactory().createPerson(new IdImpl(i+1));
			Map<String, Object> customMap = person.getCustomAttributes();
			
			Plan plan = population.getFactory().createPlan();
			plan.addActivity(createHome(sekundenAbstand, i+1, numberOfPeople));
			
			//Assigning mode
			String transportMode="";
			boolean modeFound = false;
			int j=0; int sum=0;
			while (!(modeFound)){
				sum += agentDistribution.get(j);
				if (i<sum){
					transportMode = DreieckNmodes.NAMES[j];
					modeFound = true;
					//System.out.println("A "+DreieckNmodes.NAMES[j]+" was made.");
				}
				j++;
			}
			customMap.put("transportMode", transportMode);
			Leg leg = population.getFactory().createLeg(transportMode);
			
			//Handy route definition for making the agents stay on the track
			final Id startLinkId = new IdImpl(-1);
			final Id endLinkId = new IdImpl(3*subdivisionFactor);
			List<Id> routeDescription = new ArrayList<Id>();
			for (long k=0; k<3*subdivisionFactor;k++){
				routeDescription.add(new IdImpl(k));
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
	
	private Netsim createModifiedQSim(Scenario sc, EventsManager events) {
		//From QSimFactory inspired code
		QSimConfigGroup conf = sc.getConfig().qsim();
        if (conf == null) {
            throw new NullPointerException("There is no configuration set for the QSim. Please add the module 'qsim' to your config file.");
        }
        /**/
        QSim qSim = new QSim(sc, events);
        ActivityEngine activityEngine = new ActivityEngine();
		qSim.addMobsimEngine(activityEngine);
		qSim.addActivityHandler(activityEngine);
		
		//First modification: Mobsim needs to create queue links with mzilske's passing queue
		QNetsimEngine netsimEngine = new QNetsimEngineFactory() {
			
			@Override
			public QNetsimEngine createQSimEngine(Netsim sim) {
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
				return new QNetsimEngine((QSim) sim, netsimNetworkFactory) ;
			}
		}.createQSimEngine(qSim);
		////////////////////////////////////////////////////////
		
		qSim.addMobsimEngine(netsimEngine);
		qSim.addDepartureHandler(netsimEngine.getDepartureHandler());
		TeleportationEngine teleportationEngine = new TeleportationEngine();
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
        	String id = modesData.get(new IdImpl(NAMES[i])).getModeId().toString();
        	VehicleType vT = modesData.get(new IdImpl(NAMES[i])).getVehicleType();
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
			String str = this.modesData.get(new IdImpl(NAMES[i])).getModeId().toString().substring(0, 3);
			String strn = "n_"+str;
			writer.print(strn+"\t");
		}
		writer.print("\tk\t");
		for (int i=0; i < NUMBER_OF_MODES; i++){
			String str = this.modesData.get(new IdImpl(NAMES[i])).getModeId().toString().substring(0, 3);
			String strk = "k_"+str;
			writer.print(strk+"\t");
		}
		writer.print("\tq\t");
		for (int i=0; i < NUMBER_OF_MODES; i++){
			String str = this.modesData.get(new IdImpl(NAMES[i])).getModeId().toString().substring(0, 3);
			String strq = "q_"+str;
			writer.print(strq+"\t");
		}
		writer.print("\tv\t");
		for (int i=0; i < NUMBER_OF_MODES; i++){
			String str = this.modesData.get(new IdImpl(NAMES[i])).getModeId().toString().substring(0, 3);
			String strv = "v_"+str;
			writer.print(strv+"\t");
		}
		writer.print("\n");
	}
	
	private void closeFile() {
		writer.close();
	}
	
	private Activity createHome(int sekundenFrequenz, long identifier, long numberOfPeople){
		Id homeLinkId = new IdImpl(-1);
		Activity activity = scenario.getPopulation().getFactory().createActivityFromLinkId("home", homeLinkId);
		
		Random r = new Random();
		/*Method 1: The order of leaving people is guaranteed by the minimum time step between people: first person 1 leaves, then 2, then 3 etc...
		long plannedEndTime = 6*3600 + (identifier-1)*sekundenFrequenz;
		double endTime = plannedEndTime - sekundenFrequenz/2.0 + r.nextDouble()*sekundenFrequenz;
		*/
		///*Method 2: With the expected frequency, the maximal departure time is computed and people are randomly departing within this huge time chunk.
		long TimeChunkSize = numberOfPeople * sekundenFrequenz;
		double endTime = 6 * 3600 + r.nextDouble() * TimeChunkSize; 
		//*/
		//NB:Method 2 is significantly better for the quality of fundamental diagrams;
		activity.setEndTime(endTime);
		
		return activity;
	}
	
	private Activity createWork(){
		Id workLinkId = new IdImpl(3*DreieckNmodes.subdivisionFactor);
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
		for (int i=0; i<n; i++){
			str += list[i].intValue();
			str += " ";
		}
		return str;
	}//*/
}