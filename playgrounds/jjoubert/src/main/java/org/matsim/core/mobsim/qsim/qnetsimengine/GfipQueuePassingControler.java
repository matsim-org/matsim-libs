/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,     *
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

package org.matsim.core.mobsim.qsim.qnetsimengine;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleEntersTrafficEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleLeavesTrafficEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.api.core.v01.replanning.PlanStrategyModule;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.QSimConfigGroup.SnapshotStyle;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.mobsim.framework.events.MobsimBeforeCleanupEvent;
import org.matsim.core.mobsim.framework.events.MobsimInitializedEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimBeforeCleanupListener;
import org.matsim.core.mobsim.framework.listeners.MobsimInitializedListener;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.interfaces.NetsimLink;
import org.matsim.core.mobsim.qsim.qnetsimengine.GfipMultimodalQSimFactory.QueueType;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.replanning.ReplanningContext;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.VehicleWriterV1;
import org.matsim.vehicles.VehiclesFactory;

import playground.southafrica.utilities.FileUtils;
import playground.southafrica.utilities.Header;

public class GfipQueuePassingControler {

	public static void main(String[] args){
		Header.printHeader(GfipQueuePassingControler.class.toString(), args);

		String inputDirectory = args[0];
		inputDirectory += inputDirectory.endsWith("/") ? "" : "/";
		String outputDirectory = args[2];
		outputDirectory += outputDirectory.endsWith("/") ? "" : "/";

		/* Build the scenario including network, plans and vehicles. */
		int numberOfAgents = Integer.parseInt(args[6]);
		buildTriLegScenario(inputDirectory, numberOfAgents);

		/* Set up the specific queue type. */
		final QueueType queueType = QueueType.valueOf(args[3]);

		Config config = getDefaultConfig(inputDirectory, queueType);
		config.parallelEventHandling().setNumberOfThreads(1);
		config.global().setNumberOfThreads(1);

		/* Clear and set the output directory. */
		FileUtils.delete(new File( outputDirectory ));
		config.controler().setOutputDirectory(outputDirectory);

		/* Get the correct network. */
		String networkFile = args[1];
		config.network().setInputFile(inputDirectory + networkFile);

		/* Link Ids to check. */
		String linkIdIn = args[4];
		String linkIdOut = args[5];

		// ---

		Scenario sc = ScenarioUtils.loadScenario(config);

		// ---

		final Controler controler = new Controler(sc) ;

		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				bindMobsim().toProvider(new GfipMultimodalQSimFactory(queueType));
			}
		});

		final LinkCounter linkCounter = new LinkCounter(sc, queueType, linkIdIn, linkIdOut);
		controler.getEvents().addHandler(linkCounter);
		controler.addControlerListener(linkCounter);
		controler.addControlerListener(new AssignVehiclesToAllRoutes());
		/* Mobsim. */
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				addMobsimListenerBinding().toInstance(linkCounter);
			}
		});
		/* Routers. The default module I'm adding here will take all network
		 * modes and give them the default network routing module. */
		//		controler.addOverridingModule(new TripRouterFactoryModule());

		/* Travel disutility bindings. */
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				addTravelTimeBinding(GfipMode.GFIP_A1.toString()).to(networkTravelTime());
				addTravelTimeBinding(GfipMode.GFIP_A2.toString()).to(networkTravelTime());
				addTravelTimeBinding(GfipMode.GFIP_B.toString()).to(networkTravelTime());
				addTravelTimeBinding(GfipMode.GFIP_C.toString()).to(networkTravelTime());

				addTravelDisutilityFactoryBinding(GfipMode.GFIP_A1.toString()).to(carTravelDisutilityFactoryKey());
				addTravelDisutilityFactoryBinding(GfipMode.GFIP_A2.toString()).to(carTravelDisutilityFactoryKey());
				addTravelDisutilityFactoryBinding(GfipMode.GFIP_B.toString()).to(carTravelDisutilityFactoryKey());
				addTravelDisutilityFactoryBinding(GfipMode.GFIP_C.toString()).to(carTravelDisutilityFactoryKey());
			}
		});




		controler.run();

		Header.printFooter();
	}


	private static Config getDefaultConfig(String inputDirectory, QueueType queueType){
		Config config  = ConfigUtils.createConfig();

		/* Fix the seed. */
		config.global().setRandomSeed(201602021240l);

		/* Set all the defaults we want. */
		config.controler().setLastIteration(1);
		config.controler().setWriteEventsInterval(1);

		switch (queueType) {
		case FIFO:
		case GFIP_FIFO:
			config.qsim().setLinkDynamics("FIFO");
			break;
		case BASIC_PASSING:
		case GFIP_PASSING:
			config.qsim().setLinkDynamics("PassingQ");
		default:
			break;
		}

		String[] modes ={
				GfipMode.GFIP_A1.toString(), 
				GfipMode.GFIP_A2.toString(),
				GfipMode.GFIP_B.toString(),
				GfipMode.GFIP_C.toString()};
		config.qsim().setMainModes( Arrays.asList(modes) );
		config.qsim().setEndTime(Time.UNDEFINED_TIME);
		config.qsim().setSnapshotStyle( SnapshotStyle.queue ) ;;
		config.qsim().setStuckTime(Time.parseTime("02:00:00"));
		config.plansCalcRoute().setNetworkModes(Arrays.asList(modes));

		/* Home activity */
		ActivityParams firstActivity = new ActivityParams("first");
		firstActivity.setTypicalDuration(Time.parseTime("00:10:00"));
		config.planCalcScore().addActivityParams(firstActivity);
		/* Other activity */
		ActivityParams finalActivity = new ActivityParams("final");
		finalActivity.setTypicalDuration(Time.parseTime("00:10:00"));
		config.planCalcScore().addActivityParams(finalActivity);

		/* Subpopulations */
		StrategySettings best = new StrategySettings();
		best.setWeight(1.0);
		best.setStrategyName(DefaultPlanStrategiesModule.DefaultSelector.ChangeExpBeta.toString());
		config.strategy().addStrategySettings(best);

		/* Set input files */
		config.plans().setInputFile(inputDirectory + (inputDirectory.endsWith("/") ? "" : "/") + "input_plans.xml.gz");

		/* Set up vehicle use. */
		config.vehicles().setVehiclesFile(inputDirectory + (inputDirectory.endsWith("/") ? "" : "/") + "input_vehicles.xml.gz");

		config.qsim().setTimeStepSize(0.05);
		return config;
	}

	/**
	 * <h7> Builds a scenario on an octagon network. All agents reside on node 1, 
	 * and then have 20 work activities, alternating between nodes 3 and 2, 
	 * before returning home at node 1. Each agent is randomly assigned a 
	 * vehicle of a specific type: [A1] represents motorcycles (10%) with 
	 * maximum speed of 140km/h and length 3.5m; [A2] is a light vehicle (70%) 
	 * with maximum speed 100km/h and length 7.5m; [B] is medium truck (10%) 
	 * with maximum speed 40km/h and length 15m; and [C] is a heavy truck (10%) 
	 * with maximum speed 20km/h and length 22.5m.<br><br>
	 * 
	 * All links have high free speed of 140km/h to clearly show the difference
	 * between the first-in-first-out (FIFO) queue and the standard passing 
	 * queue.<br><br>
	 * 
	 * All links are 10000m in length with 2 lanes, effectively accommodating
	 * (10000 * 2) / 7.5m = 2667 cells. The network capacity is tapered from
	 * 5000 vehicles per hour, down to 625 vehicles per hour, halving the 
	 * capacity of each subsequent link. This ensures a bottleneck in the
	 * network, and ultimately spill backs.  
	 * 
	 * @param inputDirectory where the scenario, i.e. network, plans and 
	 * 		  vehicles files are written to.
	 */
	private static void buildTriLegScenario(String inputDirectory, int numberOfAgents){
		Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());

		Set<String> modes = new TreeSet<>();
		modes.add(GfipMode.GFIP_A1.toString());
		modes.add(GfipMode.GFIP_A2.toString());
		modes.add(GfipMode.GFIP_B.toString());
		modes.add(GfipMode.GFIP_C.toString());
		modes.add(TransportMode.car);

		/* Build the linear tri-link network. */
		NetworkFactory nf = sc.getNetwork().getFactory();
		Node n1 = nf.createNode(Id.create("1", Node.class), new Coord((double) 0, (double) 0));
		sc.getNetwork().addNode(n1);
		Node n2 = nf.createNode(Id.create("2", Node.class), new Coord((double) 10000, (double) 0));
		sc.getNetwork().addNode(n2);
		Node n3 = nf.createNode(Id.create("3", Node.class), new Coord((double) 60000, (double) 0));
		sc.getNetwork().addNode(n3);
		Node n4 = nf.createNode(Id.create("4", Node.class), new Coord((double) 70000, (double) 0));
		sc.getNetwork().addNode(n4);

		/* Links with fixed capacity. */
		Link l12 = nf.createLink(Id.createLinkId("12"), n1, n2);
		l12.setLength(1000);
		l12.setCapacity(2500); 
		l12.setNumberOfLanes(1000);
		l12.setFreespeed(120.0/3.6);
		l12.setAllowedModes(modes);

		Link l23 = nf.createLink(Id.createLinkId("23"), n2, n3);
		l23.setLength(5000);
		l23.setCapacity(2500);
		l23.setNumberOfLanes(2);
		l23.setFreespeed(120.0/3.6);
		l23.setAllowedModes(modes);

		Link l34 = nf.createLink(Id.createLinkId("34"), n3, n4);
		l34.setLength(1000);
		l34.setCapacity(2500);
		l34.setNumberOfLanes(1000);
		l34.setFreespeed(120.0/3.6);
		l34.setAllowedModes(modes);

		sc.getNetwork().addLink(l12);
		sc.getNetwork().addLink(l23);
		sc.getNetwork().addLink(l34);

		new NetworkWriter(sc.getNetwork()).write(inputDirectory + (inputDirectory.endsWith("/") ? "" : "/") + "input_network.xml.gz");

		/* Chop the network into smaller bits. */
		//		Network nw0500 = LongLinkSplitter.splitNetwork(sc.getNetwork(), 500.0);
		//		new NetworkWriter(nw0500).write(outputDirectory + (outputDirectory.endsWith("/") ? "" : "/") + "input_network_0500.xml.gz");
		//		Network nw1000 = LongLinkSplitter.splitNetwork(sc.getNetwork(), 1000.0);
		//		new NetworkWriter(nw1000).write(outputDirectory + (outputDirectory.endsWith("/") ? "" : "/") + "input_network_1000.xml.gz");
		//		Network nw2000 = LongLinkSplitter.splitNetwork(sc.getNetwork(), 2000.0);
		//		new NetworkWriter(nw2000).write(outputDirectory + (outputDirectory.endsWith("/") ? "" : "/") + "input_network_2000.xml.gz");

		/* Create the vehicle types. */
		VehiclesFactory vf = VehicleUtils.getFactory();
		VehicleType A1 = vf.createVehicleType(Id.create(GfipMode.GFIP_A1.toString(), VehicleType.class));
		A1.setDescription("Motorcycle");
		A1.setMaximumVelocity(114.0/3.6);
		A1.setLength(0.5*7.5);
		A1.setPcuEquivalents(0.5);
		sc.getVehicles().addVehicleType(A1);
		//
		VehicleType A2 = vf.createVehicleType(Id.create(GfipMode.GFIP_A2.toString(), VehicleType.class));
		A2.setDescription("Light vehicle");
		A2.setMaximumVelocity(103.6/3.6);
		A2.setLength(1*7.5);
		A2.setPcuEquivalents(1.0);
		sc.getVehicles().addVehicleType(A2);
		//
		VehicleType B = vf.createVehicleType(Id.create(GfipMode.GFIP_B.toString(), VehicleType.class));
		B.setDescription("Medium vehicle");
		B.setMaximumVelocity(85.7/3.6);
		B.setLength(2*7.5);
		B.setPcuEquivalents(2.0);
		sc.getVehicles().addVehicleType(B);
		//
		VehicleType C = vf.createVehicleType(Id.create(GfipMode.GFIP_C.toString(), VehicleType.class));
		C.setDescription("Heavy vehicle");
		C.setMaximumVelocity(77.8/3.6);
		C.setLength(3*7.5);
		C.setPcuEquivalents(3.0);
		sc.getVehicles().addVehicleType(C);

		/* Create the population. */
		PopulationFactory pf = sc.getPopulation().getFactory();
		for(int i = 0; i < numberOfAgents; i++){ // 900v/h
			Person person = pf.createPerson(Id.createPersonId(i));
			Plan plan = pf.createPlan();

			/* Determine the mode of choice for this person. */
			double random = MatsimRandom.getRandom().nextDouble();
			GfipMode mode = null;
			if(random <= 0.25){ 
				mode = GfipMode.GFIP_A1;
			} else if( random <= 0.50){
				mode = GfipMode.GFIP_A2;
			} else if( random <= 0.75){
				mode = GfipMode.GFIP_B;
			} else {
				mode = GfipMode.GFIP_C;
			}

			/* Add the first activity. */
			Activity firstActivity = pf.createActivityFromCoord("first", n1.getCoord());
			firstActivity.setEndTime(MatsimRandom.getLocalInstance().nextDouble()*Time.parseTime("20:00:00"));
			plan.addActivity(firstActivity);
			Leg leg = pf.createLeg(mode.toString());
			plan.addLeg(leg);

			/* Add the final activity. */
			Activity finalActivity = pf.createActivityFromCoord("final", n4.getCoord());
			plan.addActivity(finalActivity);
			person.addPlan(plan);

			sc.getPopulation().addPerson(person);

			/* Add the vehicle for each person. */
			Vehicle vehicle = null;
			switch (mode) {
			case GFIP_A1:
				vehicle = vf.createVehicle(Id.create(person.getId().toString(), Vehicle.class), A1);
				break;
			case GFIP_A2:
				vehicle = vf.createVehicle(Id.create(person.getId().toString(), Vehicle.class), A2);
				break;
			case GFIP_B:
				vehicle = vf.createVehicle(Id.create(person.getId().toString(), Vehicle.class), B);
				break;
			case GFIP_C:
				vehicle = vf.createVehicle(Id.create(person.getId().toString(), Vehicle.class), C);
				break;
			default:
				break;
			}
			sc.getVehicles().addVehicle(vehicle);
		}
		new PopulationWriter(sc.getPopulation()).write(inputDirectory + (inputDirectory.endsWith("/") ? "" : "/") + "input_plans.xml.gz");
		new VehicleWriterV1(sc.getVehicles()).writeFile(inputDirectory + (inputDirectory.endsWith("/") ? "" : "/") + "input_vehicles.xml.gz");
	}


	private static class LinkCounter implements LinkEnterEventHandler, LinkLeaveEventHandler, 
	VehicleEntersTrafficEventHandler, VehicleLeavesTrafficEventHandler,
	IterationEndsListener, MobsimInitializedListener, MobsimBeforeCleanupListener{
		private final Logger log = Logger.getLogger(LinkCounter.class);
		private final QueueType queueType;
		private List<String> spaceTimeList;
		private List<String> allSpaceTimeList;
		private List<String> rhoList;
		private Map<Id<Link>, List<Tuple<Double,Integer>>> countMap;
		private Map<Id<Vehicle>, List<Double>> spaceTimeMap;
		private Map<Id<Vehicle>, Map<Id<Link>,List<Double>>> allSpaceTimeMap;
		private final Scenario sc;
		private Id<Link> checkedLinkIn;
		private Id<Link> checkedLinkOut;
		private QSim qsim;
		private Map<Id<Vehicle>, Double[]> timeMap;
		private Map<Id<Vehicle>, Map<Id<Link>, Double[]>> timeMapAll;
		GfipLinkSpeedCalculator calculator;

		public LinkCounter(final Scenario sc, QueueType queueType, 
				String linkIdIn, String linkIdOut) {
			this.sc = sc;
			this.queueType = queueType;
			this.spaceTimeList = new ArrayList<String>();
			this.spaceTimeMap = new TreeMap<Id<Vehicle>, List<Double>>();
			this.allSpaceTimeList = new ArrayList<String>();
			this.rhoList = new ArrayList<String>();
			this.countMap = new TreeMap<Id<Link>, List<Tuple<Double, Integer>>>();
			this.timeMap = new TreeMap<>();
			this.timeMapAll = new TreeMap<>();

			/* Initialise the link counts, knowing what the correct Ids are. */
			for(Id<Link> id : this.sc.getNetwork().getLinks().keySet()){
				this.countMap.put(id, new ArrayList<Tuple<Double, Integer>>());
				this.countMap.get(id).add(new Tuple<Double, Integer>(0.0, 0));
			}

			/* Setup link Id listeners */
			this.checkedLinkIn = Id.createLinkId(linkIdIn);
			this.checkedLinkOut = Id.createLinkId(linkIdOut);

			/* Initialise the all space-time map. */
			this.allSpaceTimeMap = new TreeMap<Id<Vehicle>, Map<Id<Link>, List<Double>>>();
			for(Id<Vehicle> vId : this.sc.getVehicles().getVehicles().keySet()){
				this.allSpaceTimeMap.put(vId, new TreeMap<Id<Link>, List<Double>>());
				this.timeMapAll.put(vId, new TreeMap<Id<Link>, Double[]>());
			}
		}

		@Override
		public void reset(int iteration) {
			this.spaceTimeList = new LinkedList<String>();
			this.allSpaceTimeList = new ArrayList<String>();
			this.countMap = new ConcurrentHashMap<Id<Link>, List<Tuple<Double, Integer>>>();
			this.rhoList = new ArrayList<>();
			this.spaceTimeMap = new ConcurrentHashMap<Id<Vehicle>, List<Double>>();
			this.timeMap = new TreeMap<>();
			this.timeMapAll = new TreeMap<>();

			/* Initialise the link counts, knowing what the correct Ids are. */
			for(Id<Link> id : this.sc.getNetwork().getLinks().keySet()){
				this.countMap.put(id, new ArrayList<Tuple<Double, Integer>>());
				this.countMap.get(id).add(new Tuple<Double, Integer>(0.0, 0));
			}
			/* Initialise the all space-time map. */
			this.allSpaceTimeMap = new TreeMap<Id<Vehicle>, Map<Id<Link>, List<Double>>>();
			for(Id<Vehicle> vId : this.sc.getVehicles().getVehicles().keySet()){
				this.allSpaceTimeMap.put(vId, new TreeMap<Id<Link>, List<Double>>());
				this.timeMapAll.put(vId, new TreeMap<Id<Link>, Double[]>());
			}
		}

		@Override
		public void handleEvent(LinkEnterEvent event) {
			/* Add the pcu-equivalents to the NetsimLink. */
			NetsimLink netsimLink = this.qsim.getNetsimNetwork().getNetsimLink(event.getLinkId());
			double pcuEquivalent = this.sc.getVehicles().getVehicles().get(event.getVehicleId()).getType().getPcuEquivalents();
			double oldPcuTotal = (double) netsimLink.getCustomAttributes().get("pcu");
			netsimLink.getCustomAttributes().put("pcu", oldPcuTotal+pcuEquivalent);

			/* Update the actual vehicle counts (ignoring pcu-equivalents) */
			double time = event.getTime();
			Id<Link> linkId = event.getLinkId();
			int oldCount = this.countMap.get(linkId).get( this.countMap.get(linkId).size()-1 ).getSecond();
			this.countMap.get(linkId).add(new Tuple<Double, Integer>(time, oldCount+1));

			/* Update the rho value for each link. */
			NetsimLink thisLink = this.qsim.getNetsimNetwork().getNetsimLink(linkId);
			double pcuEquivalents = (double) thisLink.getCustomAttributes().get("pcu");

			/* When calculating rho NOW, one should NOT add the PCU equivalents, 
			 * because this vehicle has already been put ON the link. */
			double rho = pcuEquivalents 
					/ ( (thisLink.getLink().getLength()/1000)*thisLink.getLink().getNumberOfLanes() );
			rhoList.add(String.format("%s,%.0f,%.2f", linkId.toString(), time, rho));

			/* Update the time discrepancy for ALL link 2 segments. */
			if(linkId.toString().startsWith("23")){
				Vehicle vehicle = this.sc.getVehicles().getVehicles().get( event.getVehicleId() ) ;
				GfipMode mode = GfipMode.valueOf( vehicle.getType().getId().toString().toUpperCase() ) ;

				Link link = this.sc.getNetwork().getLinks().get(event.getLinkId());

				double velocity = this.calculator.estimateModalVelocityFromDensity(rho, mode);

				double entryTime = event.getTime();
				double t = link.getLength() / velocity;
				Double[] da = {new Double(rho), new Double(entryTime), new Double(entryTime + t), 0.0, 0.0};
				timeMapAll.get(vehicle.getId()).put(linkId, da);
			}


			/* Update the entry time for ALL links. */
			Map<Id<Link>, List<Double>> linkMap = new TreeMap<>();
			linkMap.put(event.getLinkId(), new ArrayList<Double>());
			allSpaceTimeMap.put(event.getVehicleId(), linkMap);
			allSpaceTimeMap.get(event.getVehicleId()).get(event.getLinkId()).add(event.getTime());

			if(event.getLinkId().equals(checkedLinkIn)){ 
				spaceTimeMap.put(event.getVehicleId(), new ArrayList<Double>());
				spaceTimeMap.get(event.getVehicleId()).add(event.getTime());

				/* Get the entry time and estimated leave time. */
				double entryTime = event.getTime();

				//				QVehicle vehicle = this.qsim.getQNetsimEngine().getVehicles().get(event.getVehicleId());
				//				GfipMode mode = GfipMode.valueOf(vehicle.getVehicle().getType().getId().toString().toUpperCase());
				// Johan, you don't need qnetsim engine for this (or am I overlooking something?): kai, mar'15
				Vehicle vehicle = this.sc.getVehicles().getVehicles().get( event.getVehicleId() ) ;
				GfipMode mode = GfipMode.valueOf( vehicle.getType().getId().toString().toUpperCase() ) ;

				Link link = this.sc.getNetwork().getLinks().get(event.getLinkId());

				double velocity = this.calculator.estimateModalVelocityFromDensity(rho, mode);

				double t = link.getLength() / velocity;
				Double[] da = {new Double(rho), new Double(entryTime), new Double(entryTime + t), 0.0, 0.0};
				timeMap.put(event.getVehicleId(), da);
			} 

		}

		@Override
		public void handleEvent(VehicleEntersTrafficEvent event) {
			NetsimLink netsimLink = this.qsim.getNetsimNetwork().getNetsimLink(event.getLinkId());
			double pcuEquivalent = this.sc.getVehicles().getVehicles().get(event.getVehicleId()).getType().getPcuEquivalents();
			double oldPcuTotal = (double) netsimLink.getCustomAttributes().get("pcu");
			netsimLink.getCustomAttributes().put("pcu", oldPcuTotal+pcuEquivalent);

			/* Adds the first entry onto link 1. */
			Map<Id<Link>, List<Double>> linkMap = new TreeMap<>();
			linkMap.put(event.getLinkId(), new ArrayList<Double>());
			allSpaceTimeMap.put(event.getVehicleId(), linkMap);
			allSpaceTimeMap.get(event.getVehicleId()).get(event.getLinkId()).add(event.getTime());
		}


		@Override
		public void handleEvent(LinkLeaveEvent event) {
			/* Deduct the pcu-equivalents to the NetsimLink. */
			NetsimLink netsimLink = this.qsim.getNetsimNetwork().getNetsimLink(event.getLinkId());
			double pcuEquivalent = this.sc.getVehicles().getVehicles().get(event.getVehicleId()).getType().getPcuEquivalents();
			double oldPcuTotal = (double) netsimLink.getCustomAttributes().get("pcu");
			netsimLink.getCustomAttributes().put("pcu", oldPcuTotal-pcuEquivalent);
			double rho = (pcuEquivalent + this.sc.getVehicles().getVehicles().get(event.getVehicleId()).getType().getPcuEquivalents()) 
					/ ( (netsimLink.getLink().getLength()/1000)*netsimLink.getLink().getNumberOfLanes());

			double time = event.getTime();
			Id<Link> linkId = event.getLinkId();
			int oldCount = this.countMap.get(linkId).get( this.countMap.get(linkId).size()-1 ).getSecond();
			this.countMap.get(linkId).add(new Tuple<Double, Integer>(time, oldCount-1));

			/* Update the exit times for all links. */
			{
				List<Double> thisList = this.allSpaceTimeMap.get(event.getVehicleId()).get(linkId);
				thisList.add(event.getTime());
				String s = "";
				s += event.getVehicleId();
				s += "," + this.sc.getVehicles().getVehicles().get(event.getVehicleId()).getType().getId().toString();
				s += "," + linkId.toString();
				s += "," + rho;
				s += "," + String.valueOf(thisList.get(0));
				s += "," + String.valueOf(thisList.get(1));
				this.allSpaceTimeList.add(s);
				this.allSpaceTimeMap.get(event.getVehicleId()).remove(linkId);
			}

			/* Write the space-time chain to file, and then remove the person. */
			if(event.getLinkId().equals(checkedLinkOut) && this.spaceTimeMap.containsKey(event.getVehicleId())){
				List<Double> thisList = this.spaceTimeMap.get(event.getVehicleId());
				thisList.add(event.getTime());
				String s = "";
				s += event.getVehicleId();
				s += "," + this.sc.getVehicles().getVehicles().get(event.getVehicleId()).getType().getId().toString();
				s += "," + String.valueOf(thisList.get(0));
				s += "," + String.valueOf(thisList.get(1));
				this.spaceTimeList.add(s);
				this.spaceTimeMap.remove(event.getVehicleId());
			}

			/* Put the actual link leave time in the travel time observations. */
			/* First for all segments on link 2. */
			if(event.getLinkId().toString().startsWith("23")){
				Double[] da = timeMapAll.get(event.getVehicleId()).get(event.getLinkId());
				da[3] = rho;
				da[4] = event.getTime();
			}
			/* Then for only the observed links. */
			if(event.getLinkId().equals(checkedLinkOut)){ 
				Double[] da = timeMap.get(event.getVehicleId());
				da[3] = rho;
				da[4] = event.getTime();
			}
		}

		@Override
		public void handleEvent(VehicleLeavesTrafficEvent event) {
			NetsimLink netsimLink = this.qsim.getNetsimNetwork().getNetsimLink(event.getLinkId());
			double pcuEquivalent = this.sc.getVehicles().getVehicles().get(event.getPersonId()).getType().getPcuEquivalents();
			double oldPcuTotal = (double) netsimLink.getCustomAttributes().get("pcu");
			netsimLink.getCustomAttributes().put("pcu", oldPcuTotal-pcuEquivalent);

			/* Update the exit times for all links. */
			double rho = (pcuEquivalent + this.sc.getVehicles().getVehicles().get(event.getVehicleId()).getType().getPcuEquivalents()) 
					/ ( (netsimLink.getLink().getLength()/1000)*netsimLink.getLink().getNumberOfLanes());
			{
				Id<Link> linkId = event.getLinkId();
				List<Double> thisList = this.allSpaceTimeMap.get(event.getVehicleId()).get(linkId);
				thisList.add(event.getTime());
				String s = "";
				s += event.getVehicleId();
				s += "," + this.sc.getVehicles().getVehicles().get(event.getVehicleId()).getType().getId().toString();
				s += "," + linkId.toString();
				s += "," + rho;
				s += "," + String.valueOf(thisList.get(0));
				s += "," + String.valueOf(thisList.get(1));
				this.allSpaceTimeList.add(s);
				this.allSpaceTimeMap.get(event.getVehicleId()).remove(linkId);
			}
		}


		@Override
		public void notifyIterationEnds(IterationEndsEvent event) {
			/* Set up the max load map. */
			Map<Id<Link>, Integer> maxValues = new TreeMap<Id<Link>, Integer>();
			for(Id<Link> linkId : this.sc.getNetwork().getLinks().keySet()){
				maxValues.put(linkId, 0);
			}

			/* Write the load for each link. */
			for(Id<Link> linkId : this.countMap.keySet()){
				String filename = event.getServices().getControlerIO().getIterationFilename(event.getIteration(), "link_" + linkId.toString() + ".csv");
				log.info("Writing link counts to " + filename);

				BufferedWriter bw = IOUtils.getBufferedWriter(filename);
				try {
					bw.write("time,count");
					bw.newLine();

					for(Tuple<Double, Integer> tuple : this.countMap.get(linkId)){
						bw.write(String.valueOf(tuple.getFirst()));
						bw.write(",");
						bw.write(String.valueOf(tuple.getSecond()));
						bw.newLine();

						/* Update maximum load. */
						int load = tuple.getSecond();
						if(load > maxValues.get(linkId)){
							maxValues.put(linkId, load);
						}
					}
				} catch (IOException e) {
					e.printStackTrace();
					throw new RuntimeException("Cannot write link counts.");
				} finally{
					try {
						bw.close();
					} catch (IOException e) {
						e.printStackTrace();
						throw new RuntimeException("Cannot close link counts.");
					}
				}
			}

			/* Report maximum loads. */
			log.info("Maximum loads:");
			for(Id<Link> linkId : maxValues.keySet()){
				log.info("   \\_ " + linkId.toString() + ": " + maxValues.get(linkId));
			}

			/* Write the space-time observations. */
			String spaceTimeFilename = event.getServices().getControlerIO().getIterationFilename(event.getIteration(), "spaceTime.csv");
			log.info("Writing space-time observations to " + spaceTimeFilename);

			BufferedWriter bw = IOUtils.getBufferedWriter(spaceTimeFilename);
			try {
				bw.write("id,type,entry,exit");
				bw.newLine();

				for(String s : this.spaceTimeList){
					bw.write(s);
					bw.newLine();
				}
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException("Cannot write space-time observations.");
			} finally{
				try {
					bw.close();
				} catch (IOException e) {
					e.printStackTrace();
					throw new RuntimeException("Cannot close space-time observations.");
				}
			}

			/* Write the space-time observations for ALL links. */
			String allSpaceTimeFilename = event.getServices().getControlerIO().getIterationFilename(event.getIteration(), "allSpaceTime.csv");
			log.info("Writing all space-time observations to " + allSpaceTimeFilename);

			BufferedWriter bwAll = IOUtils.getBufferedWriter(allSpaceTimeFilename);
			try {
				bwAll.write("id,type,link,endRho,entry,exit");
				bwAll.newLine();

				for(String s : this.allSpaceTimeList){
					bwAll.write(s);
					bwAll.newLine();
				}
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException("Cannot write all space-time observations.");
			} finally{
				try {
					bwAll.close();
				} catch (IOException e) {
					e.printStackTrace();
					throw new RuntimeException("Cannot close all space-time observations.");
				}
			}

			/* Write the rho observations. */
			String rhoFilename = event.getServices().getControlerIO().getIterationFilename(event.getIteration(), "rho.csv");
			log.info("Writing space-time observations to " + rhoFilename);

			BufferedWriter bwRho = IOUtils.getBufferedWriter(rhoFilename);
			try {
				bwRho.write("linkId,time,rho");
				bwRho.newLine();

				for(String s : this.rhoList){
					bwRho.write(s);
					bwRho.newLine();
				}
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException("Cannot write rho observations.");
			} finally{
				try {
					bwRho.close();
				} catch (IOException e) {
					e.printStackTrace();
					throw new RuntimeException("Cannot close rho observations.");
				}
			}

			/* Write the estimated versus actual times to file. */
			String timeFilename = event.getServices().getControlerIO().getIterationFilename(event.getIteration(), "timeDiscrepancy.csv");
			log.info("Writing space-time observations to " + timeFilename);

			BufferedWriter bwTime = IOUtils.getBufferedWriter(timeFilename);
			try {
				bwTime.write("vehicleId,vehicleType,startRho,startTime,estimated,endRho,actual");
				bwTime.newLine();

				for(Id<Vehicle> id : this.timeMap.keySet()){
					String vehicleType = this.sc.getVehicles().getVehicles().get(id).getType().getId().toString();
					Double[] da = timeMap.get(id);
					bwTime.write(String.format("%s,%s,%.2f,%.2f,%.2f,%.2f,%.2f", id.toString(),  vehicleType, da[0], da[1], da[2], da[3], da[4]));
					bwTime.newLine();
				}
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException("Cannot write rho observations.");
			} finally{
				try {
					bwTime.close();
				} catch (IOException e) {
					e.printStackTrace();
					throw new RuntimeException("Cannot close rho observations.");
				}
			}

			/* Now write the estimated versus actual times for ALL link 2 
			 * segments to file. */
			String timeAllFilename = event.getServices().getControlerIO().getIterationFilename(event.getIteration(), "timeDiscrepancyAll.csv");
			log.info("Writing time discrepancy observations for ALL link 2 segments to " + timeAllFilename);

			BufferedWriter bwTimeAll = IOUtils.getBufferedWriter(timeAllFilename);
			try {
				bwTimeAll.write("vehicleId,vehicleType,linkId,startRho,startTime,estimated,endRho,actual");
				bwTimeAll.newLine();

				for(Id<Vehicle> id : this.timeMapAll.keySet()){
					for(Id<Link> linkId : this.timeMapAll.get(id).keySet()){
						String vehicleType = this.sc.getVehicles().getVehicles().get(id).getType().getId().toString();
						Double[] da = timeMapAll.get(id).get(linkId);
						bwTimeAll.write(String.format("%s,%s,%s,%.2f,%.2f,%.2f,%.2f,%.2f", 
								id.toString(),  vehicleType, linkId.toString(), da[0], da[1], da[2], da[3], da[4]));
						bwTimeAll.newLine();
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException("Cannot write rho observations.");
			} finally{
				try {
					bwTimeAll.close();
				} catch (IOException e) {
					e.printStackTrace();
					throw new RuntimeException("Cannot close rho observations.");
				}
			}
		}

		@Override
		public void notifyMobsimInitialized(MobsimInitializedEvent e) {
			this.log.info(" ==> Initialising the custom pcu attributes.");
			this.qsim = (QSim) e.getQueueSimulation();
			for(NetsimLink link : this.qsim.getNetsimNetwork().getNetsimLinks().values()){
				link.getCustomAttributes().put("pcu", 0.0);
			}
			this.calculator = new GfipLinkSpeedCalculator(this.sc.getVehicles(), this.qsim, this.queueType);
		}

		@Override
		public void notifyMobsimBeforeCleanup(MobsimBeforeCleanupEvent e) {
			/* Report all the link 'pcu' values. */
			log.info("==> Reporting the network's PCU equivalents on each link:");
			for(Id<Link> linkId : this.sc.getNetwork().getLinks().keySet()){
				NetsimLink netsimLink = this.qsim.getNetsimNetwork().getNetsimLink(linkId);
				log.info(String.format("  \\_%s: %.1f", linkId.toString(), netsimLink.getCustomAttributes().get("pcu")));
			}
		}
	}

	private static final class AssignVehiclesToAllRoutes implements IterationStartsListener{
		private final Logger log = Logger.getLogger(AssignVehiclesToAllRoutes.class);

		@Override
		public void notifyIterationStarts(IterationStartsEvent event) {
			log.info(" ====> Assigning vehicles to routes.");
			if(event.getIteration() ==  event.getServices().getConfig().controler().getFirstIteration()){
				for(Person p : event.getServices().getScenario().getPopulation().getPersons().values()){
					for(Plan plan : p. getPlans()){
						new SetVehicleInAllNetworkRoutes().handlePlan(plan);
					}
				}
			}
		}	
	}

	private static final class SetVehicleInAllNetworkRoutes implements PlanStrategyModule {
		private final String VEH_ID = "TransportModeToVehicleIdMap" ;

		private SetVehicleInAllNetworkRoutes() {
		}

		@Override
		public void prepareReplanning(ReplanningContext replanningContext) {}
		@Override
		public void handlePlan(Plan plan) {
			Id<Vehicle> vehId = Id.create(plan.getPerson().getId().toString(), Vehicle.class);
			for ( Leg leg : PopulationUtils.getLegs(plan) ) {
				if ( leg.getRoute()!=null && leg.getRoute() instanceof NetworkRoute ) {
					((NetworkRoute)leg.getRoute()).setVehicleId(vehId);
				}
			}
		}
		@Override
		public void finishReplanning() {}
	}

	public enum GfipMode{
		GFIP_A1, GFIP_A2, GFIP_B, GFIP_C;

		@Override
		public String toString(){
			return this.name().toLowerCase();
		}
	}

}
