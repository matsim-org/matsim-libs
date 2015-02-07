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
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.Wait2LinkEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.Wait2LinkEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.api.core.v01.replanning.PlanStrategyModule;
import org.matsim.core.api.experimental.network.NetworkWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
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
import org.matsim.core.mobsim.qsim.qnetsimengine.GfipQueuePassingQSimFactory.QueueType;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.replanning.DefaultPlanStrategiesModule;
import org.matsim.core.replanning.ReplanningContext;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.VehicleWriterV1;
import org.matsim.vehicles.VehiclesFactory;

import playground.southafrica.utilities.FileUtils;
import playground.southafrica.utilities.Header;

public class GfipQueuePassingControler_v2 extends Controler{

	public GfipQueuePassingControler_v2(Scenario scenario, QueueType queueType) {
		super(scenario);
		this.setMobsimFactory(new GfipQueuePassingQSimFactory(queueType));
	}

	public static void main(String[] args){
		Header.printHeader(GfipQueuePassingControler_v1.class.toString(), args);

		/* Clear and set the output directory. */
		String outputDirectory = args[0];
		FileUtils.delete(new File( outputDirectory + (outputDirectory.endsWith("/") ? "" : "/") + "output/" ));

		/* Build the scenario including network, plans and vehicles. */
//		buildTriLegScenario(outputDirectory);

		/* Set up the specific queue type. */
		QueueType queueType = QueueType.valueOf(args[1]);

		Config config = getDefaultConfig(outputDirectory, queueType);
		config.parallelEventHandling().setNumberOfThreads(1);
		Scenario sc = ScenarioUtils.loadScenario(config);
		GfipQueuePassingControler_v2 controler = new GfipQueuePassingControler_v2(sc, queueType);
		
		/* Set number of threads to ONE. */
		controler.config.global().setNumberOfThreads(1);

		LinkCounter linkCounter = controler.new LinkCounter(sc, queueType);
		controler.getEvents().addHandler(linkCounter);
		controler.addControlerListener(linkCounter);
		controler.addControlerListener(controler.new AssignVehiclesToAllRoutes());
		controler.getMobsimListeners().add(linkCounter);
		controler.run();

		Header.printFooter();
	}


	private static Config getDefaultConfig(String outputDirectory, QueueType queueType){
		Config config  = ConfigUtils.createConfig();

		/* Set all the defaults we want. */
		config.controler().setLastIteration(0);
		config.controler().setWriteEventsInterval(1);
		config.controler().setOutputDirectory(outputDirectory + (outputDirectory.endsWith("/") ? "" : "/") + "output");

		switch (queueType) {
		case FIFO:
			config.qsim().setLinkDynamics("FIFO");
			break;
		case BASIC_PASSING:
		case GFIP_PASSING:
			config.qsim().setLinkDynamics("PassingQ");
		default:
			break;
		}

		/* Random seed!! */
		config.global().setRandomSeed(2014092401);

		String[] modes ={
				GfipMode.GFIP_A1.toString(), 
				GfipMode.GFIP_A2.toString(),
				GfipMode.GFIP_B.toString(),
				GfipMode.GFIP_C.toString()};
		config.qsim().setMainModes( Arrays.asList(modes) );
		config.qsim().setEndTime(Time.parseTime("30:00:00"));
		config.qsim().setSnapshotStyle("queue");
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
		config.plans().setInputFile(outputDirectory + (outputDirectory.endsWith("/") ? "" : "/") + "input_plans.xml.gz");
		config.network().setInputFile(outputDirectory + (outputDirectory.endsWith("/") ? "" : "/") + "input_network.xml.gz");

		/* Set up vehicle use. */
		config.scenario().setUseVehicles(true);
		config.transit().setVehiclesFile(outputDirectory + (outputDirectory.endsWith("/") ? "" : "/") + "input_vehicles.xml.gz");

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
	 * @param outputDirectory where the scenario, i.e. network, plans and 
	 * 		  vehicles files are written to.
	 */
	private static void buildTriLegScenario(String outputDirectory){
		Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());

		/* Build the octagon network. */
		NetworkFactory nf = sc.getNetwork().getFactory();
		Node n1 = nf.createNode(Id.create("1", Node.class), new CoordImpl(0, 0));
		sc.getNetwork().addNode(n1);
		Node n2 = nf.createNode(Id.create("2", Node.class), new CoordImpl(10000, 0));
		sc.getNetwork().addNode(n2);
		Node n3 = nf.createNode(Id.create("3", Node.class), new CoordImpl(60000, 0));
		sc.getNetwork().addNode(n3);
		Node n4 = nf.createNode(Id.create("4", Node.class), new CoordImpl(70000, 0));
		sc.getNetwork().addNode(n4);

		/* Links with fixed capacity. */
		Link l12 = nf.createLink(Id.createLinkId("12"), n1, n2);
		l12.setLength(10000);
		l12.setCapacity(2500); 
		l12.setNumberOfLanes(1000);
		l12.setFreespeed(120.0/3.6);

		Link l23 = nf.createLink(Id.createLinkId("23"), n2, n3);
		l23.setLength(50000);
		l23.setCapacity(2500);
		l23.setNumberOfLanes(2);
		l23.setFreespeed(120.0/3.6);

		Link l34 = nf.createLink(Id.createLinkId("34"), n3, n4);
		l34.setLength(100000);
		l34.setCapacity(2500);
		l34.setNumberOfLanes(1000);
		l34.setFreespeed(120.0/3.6);

		sc.getNetwork().addLink(l12);
		sc.getNetwork().addLink(l23);
		sc.getNetwork().addLink(l34);

		new NetworkWriter(sc.getNetwork()).write(outputDirectory + (outputDirectory.endsWith("/") ? "" : "/") + "input_network.xml.gz");

		/* Create the vehicle types. */
		((ScenarioImpl)sc).createVehicleContainer();
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
		for(int i = 0; i < 30000; i++){ // 900v/h
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
		new PopulationWriter(sc.getPopulation()).write(outputDirectory + (outputDirectory.endsWith("/") ? "" : "/") + "input_plans.xml.gz");
		new VehicleWriterV1(sc.getVehicles()).writeFile(outputDirectory + (outputDirectory.endsWith("/") ? "" : "/") + "input_vehicles.xml.gz");
	}
	

	private class LinkCounter implements LinkEnterEventHandler, LinkLeaveEventHandler, 
	Wait2LinkEventHandler, PersonArrivalEventHandler,
	IterationEndsListener, MobsimInitializedListener, MobsimBeforeCleanupListener{
		private final Logger log = Logger.getLogger(LinkCounter.class);
		private final QueueType queueType;
		private List<String> spaceTimeList;
		private List<String> rhoList;
		private Map<Id<Link>, List<Tuple<Double,Integer>>> countMap;
		private Map<Id<Vehicle>, List<Double>> spaceTimeMap;
		private final Scenario sc;
		private Id<Link> checkedLink;
		private QSim qsim;
		private Map<Id<Vehicle>, Double[]> timeMap;
		GfipLinkSpeedCalculator calculator;

		public LinkCounter(final Scenario sc, QueueType queueType) {
			this.sc = sc;
			this.queueType = queueType;
			this.spaceTimeList = new ArrayList<String>();
			this.rhoList = new ArrayList<String>();
			this.countMap = new TreeMap<Id<Link>, List<Tuple<Double, Integer>>>();
			this.spaceTimeMap = new TreeMap<Id<Vehicle>, List<Double>>();
			this.timeMap = new TreeMap<>();

			/* Initialise the link counts, knowing what the correct Ids are. */
			for(Id<Link> id : this.sc.getNetwork().getLinks().keySet()){
				this.countMap.put(id, new ArrayList<Tuple<Double, Integer>>());
				this.countMap.get(id).add(new Tuple<Double, Integer>(0.0, 0));
			}

			/* Setup link Id listeners */
			this.checkedLink = Id.createLinkId("23");
		}

		@Override
		public void reset(int iteration) {
			this.spaceTimeList = new LinkedList<String>();
			this.countMap = new ConcurrentHashMap<Id<Link>, List<Tuple<Double, Integer>>>();
			this.spaceTimeMap = new ConcurrentHashMap<Id<Vehicle>, List<Double>>();

			/* Initialise the link counts, knowing what the correct Ids are. */
			for(Id<Link> id : this.sc.getNetwork().getLinks().keySet()){
				this.countMap.put(id, new ArrayList<Tuple<Double, Integer>>());
				this.countMap.get(id).add(new Tuple<Double, Integer>(0.0, 0));
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
					/ ( (thisLink.getLink().getLength()/1000)*thisLink.getLink().getNumberOfLanes());
			rhoList.add(String.format("%s,%.0f,%.2f", linkId.toString(), time, rho));
			
			if(event.getLinkId().equals(checkedLink)){ 
				spaceTimeMap.put(event.getVehicleId(), new ArrayList<Double>());
				spaceTimeMap.get(event.getVehicleId()).add(event.getTime());
				
				/* Get the entry time and estimated leave time. */
				double entryTime = event.getTime();
				QVehicle vehicle = this.qsim.getQNetsimEngine().getVehicles().get(event.getVehicleId());
				Link link = this.sc.getNetwork().getLinks().get(event.getLinkId());
				
				GfipMode mode = GfipMode.valueOf(vehicle.getVehicle().getType().getId().toString().toUpperCase());
				double velocity = this.calculator.estimateModalVelocityFromDensity(rho, mode);
				
				double t = link.getLength() / velocity;
				Double[] da = {new Double(rho), new Double(entryTime), new Double(entryTime + t), 0.0, 0.0};
				timeMap.put(event.getVehicleId(), da);
			} 
			
		}

		@Override
		public void handleEvent(Wait2LinkEvent event) {
			NetsimLink netsimLink = this.qsim.getNetsimNetwork().getNetsimLink(event.getLinkId());
			double pcuEquivalent = this.sc.getVehicles().getVehicles().get(event.getVehicleId()).getType().getPcuEquivalents();
			double oldPcuTotal = (double) netsimLink.getCustomAttributes().get("pcu");
			netsimLink.getCustomAttributes().put("pcu", oldPcuTotal+pcuEquivalent);
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

			/* Write the space-time chain to file, and then remove the person. */
			if(event.getLinkId().equals(checkedLink) && this.spaceTimeMap.containsKey(event.getVehicleId())){
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
			if(event.getLinkId().equals(checkedLink)){ 
				Double[] da = timeMap.get(event.getVehicleId());
				da[3] = rho;
				da[4] = event.getTime();
			}
		}


		@Override
		public void handleEvent(PersonArrivalEvent event) {
			NetsimLink netsimLink = this.qsim.getNetsimNetwork().getNetsimLink(event.getLinkId());
			double pcuEquivalent = this.sc.getVehicles().getVehicles().get(event.getPersonId()).getType().getPcuEquivalents();
			double oldPcuTotal = (double) netsimLink.getCustomAttributes().get("pcu");
			netsimLink.getCustomAttributes().put("pcu", oldPcuTotal-pcuEquivalent);
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
				String filename = getControlerIO().getIterationFilename(event.getIteration(), "link_" + linkId.toString() + ".csv");
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
			String spaceTimeFilename = getControlerIO().getIterationFilename(event.getIteration(), "spaceTime.csv");
			log.info("Writing space-time observations to " + spaceTimeFilename);

			BufferedWriter bw = IOUtils.getBufferedWriter(spaceTimeFilename);
			try {
				bw.write("id,type,link23,end");
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

			/* Write the rho observations. */
			String rhoFilename = getControlerIO().getIterationFilename(event.getIteration(), "rho.csv");
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
			String timeFilename = getControlerIO().getIterationFilename(event.getIteration(), "timeDiscrepancy.csv");
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
		}

		@Override
		public void notifyMobsimInitialized(MobsimInitializedEvent e) {
			this.log.info(" ==> Initialising the custom pcu attributes.");
			this.qsim = (QSim) e.getQueueSimulation();
			for(NetsimLink link : this.qsim.getNetsimNetwork().getNetsimLinks().values()){
				link.getCustomAttributes().put("pcu", 0.0);
			}
			this.calculator = new GfipLinkSpeedCalculator(this.qsim, this.queueType);
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

	private final class AssignVehiclesToAllRoutes implements IterationStartsListener{
		private final Logger log = Logger.getLogger(AssignVehiclesToAllRoutes.class);

		@Override
		public void notifyIterationStarts(IterationStartsEvent event) {
			log.info(" ====> Assigning vehicles to routes.");
			if(event.getIteration() == getConfig().controler().getFirstIteration()){
				for(Person p : getScenario().getPopulation().getPersons().values()){
					for(Plan plan : p. getPlans()){
						new SetVehicleInAllNetworkRoutes().handlePlan(plan);
					}
				}
			}
		}	
	}

	private final class SetVehicleInAllNetworkRoutes implements PlanStrategyModule {
		private final String VEH_ID = "TransportModeToVehicleIdMap" ;

		private SetVehicleInAllNetworkRoutes() {
		}

		@Override
		public void prepareReplanning(ReplanningContext replanningContext) {}
		@Override
		public void handlePlan(Plan plan) {
			@SuppressWarnings("unchecked")
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

		public String toString(){
			return this.name().toLowerCase();
		}
	}

}
