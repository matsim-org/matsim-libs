/* *********************************************************************** *
 * project: org.matsim.*
 * LangeStreckeSzenario													   *
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

package playground.ssix;

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
import org.matsim.core.gbl.MatsimRandom;
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

import playgrounds.ssix.MyPersonDriverAgentImpl;

public class DreieckStreckeSzenarioTest {

	/**
	 * @param args
	 */
	private static final Logger log = Logger.getLogger(DreieckStreckeSzenarioTest.class);
	
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
			if (!(goHome)){
				Id forcedLeftTurnLinkId = new IdImpl((long)(3*DreieckStreckeSzenarioTest.subdivisionFactor - 1));
				if (!(delegate.getCurrentLinkId().equals(forcedLeftTurnLinkId))){
					return delegate.chooseNextLinkId();
				}
				Id afterLeftTurnLinkId = new IdImpl((long)(0));
				delegate.setCachedNextLinkId(afterLeftTurnLinkId);
				delegate.setCurrentLinkIdIndex(0);
				return afterLeftTurnLinkId;
			}
			Id forcedStraightForwardLinkId = new IdImpl((long)(DreieckStreckeSzenarioTest.subdivisionFactor - 1));
			if (!(delegate.getCurrentLinkId().equals(forcedStraightForwardLinkId))){
				return delegate.chooseNextLinkId();
			}
			Id afterGoingStraightForwardLinkId = new IdImpl((long)(3*DreieckStreckeSzenarioTest.subdivisionFactor));
			delegate.setCachedNextLinkId(afterGoingStraightForwardLinkId);
			delegate.setCurrentLinkIdIndex(3*DreieckStreckeSzenarioTest.subdivisionFactor);//This does work quite well so far and allows to end simulation.
			return afterGoingStraightForwardLinkId;
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
	public static double length = 166.67;//in m, length of one the triangle sides.
	public static int NETWORK_CAPACITY = 2700;//in PCU/h
	private static long NUMBER_OF_AGENTS = 160;
	private static String OUTPUT_DIR = "Z:\\WinHome\\Desktop\\workspace2\\playgrounds\\ssix\\output\\data_Patna_MZ_corrected_10kmh.txt";
	
	private static double FREESPEED = 50.;//in km/h, maximum authorized velocity on the track
	private static double P_TRUCK = 0.85;//no need to worry much about those, are normalized when choosing effective transport mode
	//private static double P_MED = 2.;
	private static double P_FAST = 0.15;
	protected static double PCU_TRUCK = 0.25;//PCUs of the different possible modes
	protected static double PCU_MED = 1.;
	protected static double PCU_FAST = 1.;
	protected static double VMAX_TRUCK = 4.16;//maximum velocities of the vehicle types
	protected static double VMAX_MED = 12.;
	protected static double VMAX_FAST = 13.88;
	
	private PrintStream writer;
	
	private Scenario scenario;
	
	private int networkCapacity;//the capacity all links of the network will have
	
	public DreieckStreckeSzenarioTest(int networkCapacity){
		this.networkCapacity = networkCapacity;

		Config config = ConfigUtils.createConfig();
		config.addQSimConfigGroup(new QSimConfigGroup());
		config.getQSimConfigGroup().setSnapshotStyle(QSimConfigGroup.SNAPSHOT_AS_QUEUE) ;
		config.getQSimConfigGroup().setMainModes(Arrays.asList("fast"/*,"med"*/,"truck"));
		config.getQSimConfigGroup().setStuckTime(100*3600.);//allows to overcome maximal density regime
		config.getQSimConfigGroup().setEndTime(16*3600);//allows to set agents to abort after getting the wanted data.
									//TODO: is for actual network configurations correct, needs dependency on bigger network length 
		
		config.vspExperimental().addParam("vspDefaultsCheckingLevel", VspExperimentalConfigGroup.ABORT) ;
		// this may lead to abort during execution.  In such cases, please fix the configuration.  if necessary, talk
		// to me (kn).
		
		this.scenario = ScenarioUtils.createScenario(config);
	}
	
	public static void main(String[] args) {
		DreieckStreckeSzenarioTest dreieck = new DreieckStreckeSzenarioTest(NETWORK_CAPACITY);
		dreieck.fillNetworkData();
		
		dreieck.openFile(OUTPUT_DIR);
		
		for (int number_cars = 0; number_cars < 81; number_cars+=10){//8
			for (int number_bikes = 0; number_bikes < 321 - number_cars; number_bikes+=20){//14
				dreieck.runParam(number_bikes,number_cars);
			}
		}
		dreieck.closeFile();//*/
		/*
		long number_of_iterations = NUMBER_OF_AGENTS + 1;//-10 if constantFastDensity... because 10 vehicles are already in the sim
		for (long i = 0; i < number_of_iterations; i+=10){
			dreieck.run(i, "onlyTruck");
		}
		dreieck.closeFile();
		//*/
	}
	
	private void runParam(int number_trucks, int number_fast){
		createWantedPopulation(number_trucks,0,number_fast,2);
		
		EventsManager events = EventsUtils.createEventsManager();
		
		FunDiagramsWithPassing fundi3 = new FunDiagramsWithPassing(this.scenario);
		events.addHandler(fundi3);
		
		runqsim(events);
		
		//writer.doSomething
		writer.format("%d\t", number_trucks);
		writer.format("%d\t\t", number_fast);
		
		log.info("Writing down data for "+number_trucks+number_fast+" people...");
		
		writer.format("%.2f\t", fundi3.getEndDensity());
		writer.format("%.2f\t", fundi3.getEndDensity_truck());
		//writer.format("%.2f\t", fundi3.getEndDensity_med());
		writer.format("%.2f\t\t", fundi3.getEndDensity_fast());
		
		writer.format("%.2f\t", fundi3.getEndFlow());
		writer.format("%.2f\t", fundi3.getEndFlow_truck());
		//writer.format("%.2f\t", fundi3.getEndFlow_med());
		writer.format("%.2f\t\t", fundi3.getEndFlow_fast());
		
		writer.format("%.2f\t", fundi3.getEndAverageVelocity());
		writer.format("%.2f\t", fundi3.getEndAverageVelocity_truck());
		//writer.format("%.2f\t", fundi3.getEndAverageVelocity_med());
		writer.format("%.2f\n", fundi3.getEndAverageVelocity_fast());
		log.info("Done!");
	}
	
	public void run(long numberOfPeople, String mode){
		//fillNetworkData();done in main
		
		makePopulation(numberOfPeople, mode);
		//createRandomPopulation((long)39,2);
		//createWantedPopulation(1,0,2,2);
		
		EventsManager events = EventsUtils.createEventsManager();
		
		FunDiagramsWithPassing fundi3 = new FunDiagramsWithPassing(this.scenario);
		events.addHandler(fundi3);
		
		runqsim(events);
		
		//writer.doSomething
		writer.format("%d\t\t", numberOfPeople);
		log.info("Writing down data for "+numberOfPeople+" people...");
		
		writer.format("%.2f\t", fundi3.getEndDensity());
		writer.format("%.2f\t", fundi3.getEndDensity_truck());
		//writer.format("%.2f\t", fundi3.getEndDensity_med());
		writer.format("%.2f\t\t", fundi3.getEndDensity_fast());
		
		writer.format("%.2f\t", fundi3.getEndFlow());
		writer.format("%.2f\t", fundi3.getEndFlow_truck());
		//writer.format("%.2f\t", fundi3.getEndFlow_med());
		writer.format("%.2f\t\t", fundi3.getEndFlow_fast());
		
		writer.format("%.2f\t", fundi3.getEndAverageVelocity());
		writer.format("%.2f\t", fundi3.getEndAverageVelocity_truck());
		//writer.format("%.2f\t", fundi3.getEndAverageVelocity_med());
		writer.format("%.2f\n", fundi3.getEndAverageVelocity_fast());
		log.info("Done!");
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
					x += DreieckStreckeSzenarioTest.length/DreieckStreckeSzenarioTest.subdivisionFactor;
				}
			}
			Coord coord = scenario.createCoord(x, y);
			Id id = new IdImpl((long)i);
			
			Node node = scenario.getNetwork().getFactory().createNode(id, coord);
			network.addNode(node);	
		}
		//nodes of the triangle right side
		for (int i = 0; i<DreieckStreckeSzenarioTest.subdivisionFactor; i++){
			double x = DreieckStreckeSzenarioTest.length - (DreieckStreckeSzenarioTest.length/(2*DreieckStreckeSzenarioTest.subdivisionFactor))*(i+1);
			double y = Math.sqrt(3.)*(DreieckStreckeSzenarioTest.length-x);
			Coord coord = scenario.createCoord(x, y);
			Id id = new IdImpl((long)(DreieckStreckeSzenarioTest.subdivisionFactor+i+1));
			
			Node node = scenario.getNetwork().getFactory().createNode(id, coord);
			network.addNode(node);
		}
		//nodes of the triangle left side
		for (int i = 0; i<DreieckStreckeSzenarioTest.subdivisionFactor-1; i++){
			double x = DreieckStreckeSzenarioTest.length/2 - (DreieckStreckeSzenarioTest.length/(2*DreieckStreckeSzenarioTest.subdivisionFactor))*(i+1);
			double y = Math.sqrt(3.)*x;
			Coord coord = scenario.createCoord(x, y);
			Id id = new IdImpl((long)(2*DreieckStreckeSzenarioTest.subdivisionFactor+i+1));
			
			Node node = scenario.getNetwork().getFactory().createNode(id, coord);
			network.addNode(node);
		}
		//additional startNode and endNode for home and work activities
		Coord coord = scenario.createCoord(-50.0, 0.0);
		Id startId = new IdImpl(-1);
		Node startNode = scenario.getNetwork().getFactory().createNode(startId, coord);
		network.addNode(startNode);
		coord = scenario.createCoord(DreieckStreckeSzenarioTest.length+50.0, 0.0);
		Id endId = new IdImpl(3*DreieckStreckeSzenarioTest.subdivisionFactor);
		Node endNode = scenario.getNetwork().getFactory().createNode(endId, coord);
		network.addNode(endNode);
		
		//LINKS
		//all triangle links
		for (int i = 0; i<3*DreieckStreckeSzenarioTest.subdivisionFactor; i++){
			Id idFrom = new IdImpl((long)i);
			Id idTo;
			if (i != 3*DreieckStreckeSzenarioTest.subdivisionFactor-1)
				idTo = new IdImpl((long)(i+1));
			else
				idTo = new IdImpl(0);
			Node from = network.getNodes().get(idFrom);
			Node to = network.getNodes().get(idTo);
			
			Link link = this.scenario.getNetwork().getFactory().createLink(idFrom, from, to);
			link.setCapacity(this.networkCapacity);
			link.setFreespeed(DreieckStreckeSzenarioTest.FREESPEED/3.6);
			link.setLength(calculateLength(from,to));
			link.setNumberOfLanes(1.);
			network.addLink(link);
		}
		//additional startLink and endLink for home and work activities
		Link startLink = this.scenario.getNetwork().getFactory().createLink(startId, startNode, this.scenario.getNetwork().getNodes().get(new IdImpl(0)));
		startLink.setCapacity(capMax);
		startLink.setFreespeed(DreieckStreckeSzenarioTest.FREESPEED/3.6);
		startLink.setLength(25.);
		startLink.setNumberOfLanes(2.);
		network.addLink(startLink);
		Link endLink = this.scenario.getNetwork().getFactory().createLink(endId, this.scenario.getNetwork().getNodes().get(new IdImpl(DreieckStreckeSzenarioTest.subdivisionFactor)), endNode);
		endLink.setCapacity(capMax);
		endLink.setFreespeed(DreieckStreckeSzenarioTest.FREESPEED/3.6);
		endLink.setLength(25.);
		endLink.setNumberOfLanes(1.);
		network.addLink(endLink);
		
		//check with .xml and Visualizer
		//NetworkWriter writer = new NetworkWriter(network);
		//writer.write("./output/dreieck_network.xml");
	}
	
	private void makePopulation(long n, String mode){
		
		if (mode.equals("constantModalSplit")){//This ModalSplit is dicted by P_TRUCK,P_MED and P_FAST
			long n_trucks = new Double(n*P_TRUCK).longValue();
			long n_med = 0;/*long n_med = new Double(n*P_MED).longValue();*/
			long n_fast = n - n_trucks - n_med;
			createWantedPopulation(n_trucks,n_med,n_fast,2);
		}
		
		//Idea1/1bis: Only one mode, see that you get back on the expected Q fundamental diagrams Simon/Esser/Nagel
		if (mode.equals("onlyTruck")){
			createWantedPopulation(n,0,0,2);
		}/*
		if (mode.equals("onlyMed")){
			createWantedPopulation(0,n,0,2);
		}*/
		if (mode.equals("onlyFast")){
			createWantedPopulation(0,0,n,2);
		}
				
		//Idea2: constant truck density, fast from 0 to max // constant fast density, truck from 0 to max.
		if (mode.equals("constantTruckDensity10veh")){
			createWantedPopulation(10, 0, n, 2);
		}
		if (mode.equals("constantFastDensity10veh")){
			createWantedPopulation(n, 0, 10, 2);
		}
		
		//Idea3: completely random
		if (mode.equals("random")){
			createRandomPopulation(n,2);
		}
		//TODO: other experimentations still need implementing?
	}
	
	private void createRandomPopulation(long numberOfPeople, int sekundenFrequenz){
		Population population = scenario.getPopulation();
		//population.clear()......?No need seen so far.
		Random rand = MatsimRandom.getRandom();//for more randomness use new Random()
		// other option get MatsimRandom(), is then more deterministic and allows introduction of a new person
		// without changing previous modal split
		
		for (long i = 0; i<numberOfPeople; i++){
			
			Person person = population.getFactory().createPerson(createId(i+1));
			Map<String, Object> customMap = person.getCustomAttributes();
			
			Plan plan = population.getFactory().createPlan();
			plan.addActivity(createHome(sekundenFrequenz, i+1, numberOfPeople));
			//Assigning this person to a randomly chosen transport mode
			String transportMode="";
			double p = rand.nextDouble();
			double psum = DreieckStreckeSzenarioTest.P_FAST/*+DreieckStreckeSzenarioTest.P_MED*/+DreieckStreckeSzenarioTest.P_TRUCK;
			//double p_med = DreieckStreckeSzenarioTest.P_MED/psum;
			double p_truck = DreieckStreckeSzenarioTest.P_TRUCK/psum;
			if (p<p_truck){
				transportMode = "truck";
				//System.out.println("A truck was made.");
			/*
			} else if (p<(p_truck+p_med)) {
				transportMode = "med";
				//System.out.println("A med was made.");
			*/
			} else {
				transportMode = "fast";
				//System.out.println("A fast was made.");
			}
			customMap.put("transportMode", transportMode);
			Leg leg = population.getFactory().createLeg(transportMode);
			
			//following modification goes with the modification in the prepareForSim method
			final Id startLinkId = new IdImpl(-1);
			final Id endLinkId = new IdImpl(3*DreieckStreckeSzenarioTest.subdivisionFactor);
			//NetworkRoute route = new CompressedNetworkRouteImpl();
			List<Id> routeDescription = new ArrayList<Id>();
			for (long j=0; j<3*DreieckStreckeSzenarioTest.subdivisionFactor;j++){
				routeDescription.add(new IdImpl(j));
			}
			NetworkRoute route = new LinkNetworkRouteImpl(startLinkId, endLinkId);
			route.setLinkIds(startLinkId, routeDescription, endLinkId);
			leg.setRoute(route);
			//end of modification//works!
			plan.addLeg(leg);
			plan.addActivity(createWork());
			
			person.addPlan(plan);
			population.addPerson(person);
		}
		
		//check with xml
		//PopulationWriter writer = new PopulationWriter(population, scenario.getNetwork());
		//writer.write("./input/plans.xml");
	}

	private void createWantedPopulation(long numberOfTrucks, long numberOfMed, long numberOfFast, int sekundenFrequenz){
		Population population = scenario.getPopulation();
		
		population.getPersons().clear();
		
		long numberOfPeople =  numberOfTrucks+numberOfMed+numberOfFast;
		
		for (long i = 0; i<numberOfPeople; i++){
			
			Person person = population.getFactory().createPerson(createId(i+1));
			Map<String, Object> customMap = person.getCustomAttributes();
			
			Plan plan = population.getFactory().createPlan();
			plan.addActivity(createHome(sekundenFrequenz, i+1, numberOfPeople));

			String transportMode="";

			if (i<numberOfTrucks){
				transportMode = "truck";
				//System.out.println("A truck was made.");
			/*
			} else if (i<numberOfMed)) {
				transportMode = "med";
				//System.out.println("A med was made.");
			*/
			} else {
				transportMode = "fast";
				//System.out.println("A fast was made.");
			}
			customMap.put("transportMode", transportMode);
			Leg leg = population.getFactory().createLeg(transportMode);
			
			//following modification goes with the modification in the prepareForSim method
			final Id startLinkId = new IdImpl(-1);
			final Id endLinkId = new IdImpl(3*DreieckStreckeSzenarioTest.subdivisionFactor);
			List<Id> routeDescription = new ArrayList<Id>();
			for (long j=0; j<3*DreieckStreckeSzenarioTest.subdivisionFactor;j++){
				routeDescription.add(new IdImpl(j));
			}
			NetworkRoute route = new LinkNetworkRouteImpl(startLinkId, endLinkId);
			route.setLinkIds(startLinkId, routeDescription, endLinkId);
			leg.setRoute(route);
			//end of modification//works!
			plan.addLeg(leg);
			plan.addActivity(createWork());
			
			person.addPlan(plan);
			population.addPerson(person);
		}
	}
	
	private void runqsim(EventsManager events){
		//Normal QSim with default agents (will go to work right away)
		//Netsim qSim = new QSimFactory().createMobsim(scenario, events);
		
		//Modified QSim with modified agents that go round and round.
		Netsim qSim = createModifiedQSim(this.scenario, events);

		//prepareForSim();//-obsolete
		
		//TODO: Visualizer doesn't work anymore for quite obscure reasons.
		//OnTheFlyServer server = OTFVis.startServerAndRegisterWithQSim(scenario.getConfig(), scenario, events, (QSim)qSim);
		//OTFClientLive.run(scenario.getConfig(), server);
		
		qSim.run();
	}
	
	/*obsolete method
	private void prepareForSim() {
		// make sure all routes are calculated.
		//Calculating routes this way will make them direct. On the contrary we want the drivers to go all the way around
		// 	-> All routes are now implemented in the createPopulation method while creating legs
		
		
		ParallelPersonAlgorithmRunner.run(scenario.getPopulation(), scenario.getConfig().global().getNumberOfThreads(),
				new ParallelPersonAlgorithmRunner.PersonAlgorithmProvider() {
			@Override
			public AbstractPersonAlgorithm getPersonAlgorithm() {
				TravelTimeCalculator
 travelTimes = new TravelTimeCalculatorFactoryImpl().createTravelTimeCalculator(scenario.getNetwork(), scenario.getConfig().travelTimeCalculator());
				TravelDisutility travelCosts = new TravelCostCalculatorFactoryImpl().createTravelDisutility(travelTimes, scenario.getConfig().planCalcScore());
				PlansCalcRoute plansCalcRoute = new PlansCalcRoute(scenario.getConfig().plansCalcRoute(), scenario.getNetwork(), travelCosts, travelTimes, new DijkstraFactory(), ((PopulationFactoryImpl)(scenario.getPopulation().getFactory())).getModeRouteFactory());
				return new PersonPrepareForSim(plansCalcRoute, (ScenarioImpl)scenario);
			}
		});
				
	}*/
	
	private QSim createModifiedQSim(Scenario sc, EventsManager events){
		//From QSimFactory inspired code
		QSimConfigGroup conf = sc.getConfig().getQSimConfigGroup();
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
						return new QLinkImpl(link, network, toQueueNode, new MZilskePassingVehicleQ());
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
		VehicleType truck = VehicleUtils.getFactory().createVehicleType(new IdImpl("truck"));
		truck.setPcuEquivalents(PCU_TRUCK);
		truck.setMaximumVelocity(VMAX_TRUCK);
		VehicleCapacity cap = new VehicleCapacityImpl();
		cap.setSeats(3);
		truck.setCapacity(cap);
		modeVehicleTypes.put("truck", truck);
		VehicleType med = VehicleUtils.getFactory().createVehicleType(new IdImpl("med"));
		med.setPcuEquivalents(PCU_MED);
		med.setMaximumVelocity(VMAX_MED);
		med.setCapacity(cap);
		//modeVehicleTypes.put("med", med);
		VehicleType fast = VehicleUtils.getFactory().createVehicleType(new IdImpl("fast"));
		fast.setPcuEquivalents(PCU_FAST);
		fast.setMaximumVelocity(VMAX_FAST);
		fast.setCapacity(cap);
		modeVehicleTypes.put("fast", fast);
		
        agentSource.setModeVehicleTypes(modeVehicleTypes);
        //////////////////////////////////////////////////////
        
        qSim.addAgentSource(agentSource);
        return qSim;
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
	
	private void openFile(String dir) {
		try {
			writer = new PrintStream(dir);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		writer.format("%s\t%s\t\t%s\t\t%s\t\t%s\t\t\t%s\t\t%s\t%s\t\t%s\t\t%s\t\t%s\n",	
				     "n_t", "n_f",  "k", "k_t","k_f", "q","q_t","q_f","v","v_t","v_f");
		
		//writer.format("%s\t%s\t%s\t%s\n",
		//			  "n_bikes","n_cars","flow","speed");
	}
	
	private void closeFile() {
		writer.close();
	}
	
	private Activity createWork(){
		Id workLinkId = new IdImpl(3*DreieckStreckeSzenarioTest.subdivisionFactor);
		Activity activity = scenario.getPopulation().getFactory().createActivityFromLinkId("work", workLinkId);
		return activity;
	}
	
	private Id createId(long id){
		return new IdImpl(id);
	}
	
	private double calculateLength(Node from, Node to){
		double x1 = from.getCoord().getX();
		double y1 = from.getCoord().getY();
		double x2 = to.getCoord().getX();
		double y2 = to.getCoord().getY();
		return Math.sqrt((x2-x1)*(x2-x1)+(y2-y1)*(y2-y1));
	}
}
