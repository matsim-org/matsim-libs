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

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.*;
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
import org.matsim.core.mobsim.framework.events.MobsimInitializedEvent;
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
import org.matsim.vehicles.*;
import playground.southafrica.utilities.FileUtils;
import playground.southafrica.utilities.Header;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class GfipQueuePassingControler extends Controler{

	public GfipQueuePassingControler(Scenario scenario, QueueType queueType) {
		super(scenario);
		this.setMobsimFactory(new GfipQueuePassingQSimFactory(queueType));
	}
	
	public static void main(String[] args){
		Header.printHeader(GfipQueuePassingControler.class.toString(), args);

		/* Clear and set the output directory. */
		String outputDirectory = args[0];
		FileUtils.delete(new File(outputDirectory));
		new File(outputDirectory).mkdirs();
		
		/* Build the scenario including network, plans and vehicles. */
		buildOctagonScenario(outputDirectory);

		/* Set up the specific queue type. */
		QueueType queueType = QueueType.valueOf(args[1]);
		
		Config config = getDefaultConfig(outputDirectory, queueType);
		Scenario sc = ScenarioUtils.loadScenario(config);
		GfipQueuePassingControler controler = new GfipQueuePassingControler(sc, queueType);
		
		LinkCounter linkCounter = controler.new LinkCounter(sc);
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
		config.qsim().setEndTime(Double.POSITIVE_INFINITY);
		config.qsim().setSnapshotStyle("queue");
		config.qsim().setStuckTime(Time.parseTime("00:10:00"));
		config.plansCalcRoute().setNetworkModes(Arrays.asList(modes));
		
		/* Home activity */
		ActivityParams home = new ActivityParams("home");
		
		home.setTypicalDuration(Time.parseTime("00:01:00"));
		config.planCalcScore().addActivityParams(home);
		/* Other activity */
		ActivityParams other = new ActivityParams("other");
		other.setTypicalDuration(Time.parseTime("00:01:00"));
		config.planCalcScore().addActivityParams(other);
		
		/* Subpopulations */
		StrategySettings best = new StrategySettings();
		best.setWeight(1.0);
		best.setStrategyName(DefaultPlanStrategiesModule.DefaultSelectors.ChangeExpBeta.toString());
		config.strategy().addStrategySettings(best);
		
		/* Set input files */
		config.plans().setInputFile(outputDirectory + (outputDirectory.endsWith("/") ? "" : "/") + "input_plans.xml.gz");
		config.network().setInputFile(outputDirectory + (outputDirectory.endsWith("/") ? "" : "/") + "input_network.xml.gz");
		
		/*TODO Set up vehicle use. */
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
	 * All links are 2000m in length with 2 lanes, effectively accommodating
	 * (2000 * 2) / 7.5m = 533 cells. The network capacity is tapered from
	 * 5000 vehicles per hour, down to 625 vehicles per hour, halving the 
	 * capacity of each subsequent link. This ensures a bottleneck in the
	 * network, and ultimately spill backs.  
	 * 
	 * @param outputDirectory where the scenario, i.e. network, plans and 
	 * 		  vehicles files are written to.
	 */
	private static void buildOctagonScenario(String outputDirectory){
		Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		
		/* Build the octagon network. */
		NetworkFactory nf = sc.getNetwork().getFactory();
		double a = 2000.0;
		double b = a / Math.sqrt(2.0);
		Node n1 = nf.createNode(Id.create("1", Node.class), new CoordImpl(b, 0));
		sc.getNetwork().addNode(n1);
		Node n2 = nf.createNode(Id.create("2", Node.class), new CoordImpl(b+a, 0));
		sc.getNetwork().addNode(n2);
		Node n3 = nf.createNode(Id.create("3", Node.class), new CoordImpl(2*b+a, b));
		sc.getNetwork().addNode(n3);
		Node n4 = nf.createNode(Id.create("4", Node.class), new CoordImpl(2*b+a, b+a));
		sc.getNetwork().addNode(n4);
		Node n5 = nf.createNode(Id.create("5", Node.class), new CoordImpl(b+a, 2*b+a));
		sc.getNetwork().addNode(n5);
		Node n6 = nf.createNode(Id.create("6", Node.class), new CoordImpl(b, 2*b+a));
		sc.getNetwork().addNode(n6);
		Node n7 = nf.createNode(Id.create("7", Node.class), new CoordImpl(0d, b+a));
		sc.getNetwork().addNode(n7);
		Node n8 = nf.createNode(Id.create("8", Node.class), new CoordImpl(0d, b));
		sc.getNetwork().addNode(n8);
		
		/* Links with fixed capacity. */
		Link l12 = nf.createLink(Id.createLinkId("12"), n1, n2);
		l12.setLength(2000.0);
		l12.setCapacity(5000.0); 
		l12.setNumberOfLanes(2);
		l12.setFreespeed(140.0/3.6);
		Link l23 = nf.createLink(Id.createLinkId("23"), n2, n3);
		l23.setLength(2000.0);
		l23.setCapacity(5000.0);
		l23.setNumberOfLanes(2);
		l23.setFreespeed(140.0/3.6);
		Link l34 = nf.createLink(Id.createLinkId("34"), n3, n4);
		l34.setLength(2000.0);
		l34.setCapacity(5000.0);
		l34.setNumberOfLanes(2);
		l34.setFreespeed(140.0/3.6);
		Link l45 = nf.createLink(Id.createLinkId("45"), n4, n5);
		l45.setLength(2000.0);
		l45.setCapacity(5000.0);
		l45.setNumberOfLanes(2);
		l45.setFreespeed(140.0/3.6);
		Link l56 = nf.createLink(Id.createLinkId("56"), n5, n6);
		l56.setLength(2000.0);
		l56.setCapacity(5000.0);
		l56.setNumberOfLanes(2);
		l56.setFreespeed(140.0/3.6);
		
		/* Links with reducing capacity. */
		Link l67 = nf.createLink(Id.createLinkId("67"), n6, n7);
		l67.setLength(2000.0);
		l67.setCapacity(2500.0);
		l67.setNumberOfLanes(2);
		l67.setFreespeed(140.0/3.6);
		
		Link l78 = nf.createLink(Id.createLinkId("78"), n7, n8);
		l78.setLength(2000.0);
		l78.setCapacity(1250.0);
		l78.setNumberOfLanes(2);
		l78.setFreespeed(140.0/3.6);
		
		Link l81 = nf.createLink(Id.createLinkId("81"), n8, n1);
		l81.setLength(2000.0);
		l81.setCapacity(625.0);
		l81.setNumberOfLanes(2);
		l81.setFreespeed(140.0/3.6);
		
		sc.getNetwork().addLink(l12);
		sc.getNetwork().addLink(l23);
		sc.getNetwork().addLink(l34);
		sc.getNetwork().addLink(l45);
		sc.getNetwork().addLink(l56);
		sc.getNetwork().addLink(l67);
		sc.getNetwork().addLink(l78);
		sc.getNetwork().addLink(l81);
		
		new NetworkWriter(sc.getNetwork()).write(outputDirectory + (outputDirectory.endsWith("/") ? "" : "/") + "input_network.xml.gz");
		
		/* Create the vehicle types. */
		((ScenarioImpl)sc).createVehicleContainer();
		VehiclesFactory vf = VehicleUtils.getFactory();
		VehicleType A1 = vf.createVehicleType(Id.create(GfipMode.GFIP_A1.toString(), VehicleType.class));
		A1.setDescription("Motorcycle");
		A1.setMaximumVelocity(140.0/3.6);
		A1.setLength(0.5*7.5);
		A1.setPcuEquivalents(0.5);
		sc.getVehicles().addVehicleType(A1);
		//
		VehicleType A2 = vf.createVehicleType(Id.create(GfipMode.GFIP_A2.toString(), VehicleType.class));
		A2.setDescription("Light vehicle");
		A2.setMaximumVelocity(110.0/3.6);
		A2.setLength(1*7.5);
		A2.setPcuEquivalents(1.0);
		sc.getVehicles().addVehicleType(A2);
		//
		VehicleType B = vf.createVehicleType(Id.create(GfipMode.GFIP_B.toString(), VehicleType.class));
		B.setDescription("Medium vehicle");
		B.setMaximumVelocity(40.0/3.6);
		B.setLength(2*7.5);
		B.setPcuEquivalents(2.0);
		sc.getVehicles().addVehicleType(B);
		//
		VehicleType C = vf.createVehicleType(Id.create(GfipMode.GFIP_C.toString(), VehicleType.class));
		C.setDescription("Heavy vehicle");
		C.setMaximumVelocity(20.0/3.6);
		C.setLength(3*7.5);
		C.setPcuEquivalents(3.0);
		sc.getVehicles().addVehicleType(C);
		
		/* Create the population. */
		PopulationFactory pf = sc.getPopulation().getFactory();
		for(int i = 0; i < 2500; i++){
			Person person = pf.createPerson(Id.createPersonId(i));
			Plan plan = pf.createPlan();
			
			/* Determine the mode of choice for this person. */
			double random = MatsimRandom.getRandom().nextDouble();
			GfipMode mode = null;
			if(random <= 0.1){ 
				mode = GfipMode.GFIP_A1;
			} else if( random <= 0.80){
				mode = GfipMode.GFIP_A2;
			} else if( random <= 0.90){
				mode = GfipMode.GFIP_B;
			} else {
				mode = GfipMode.GFIP_C;
			}
			
			/* Add the first home activity. */
			int homeNode = 1;
			Activity home1 = pf.createActivityFromCoord("home", sc.getNetwork().getNodes().get(Id.createNodeId(homeNode)).getCoord());
			home1.setEndTime(MatsimRandom.getLocalInstance().nextDouble()*Time.parseTime("05:00:00"));
			plan.addActivity(home1);
			Leg leg = pf.createLeg(mode.toString());
			plan.addLeg(leg);
			
			/* Add 20 activities. */
			for(int j = 0; j < 10; j++){
//				int activityNode = MatsimRandom.getLocalInstance().nextInt(8)+1;
//				Activity activity = pf.createActivityFromCoord("other", sc.getNetwork().getNodes().get(Id.createNodeId(activityNode)).getCoord());
//				activity.setMaximumDuration(Time.parseTime("00:01:00")); /* 1 minute */
//				plan.addActivity(activity);
//				Leg anotherLeg = pf.createLeg("car");
//				plan.addLeg(anotherLeg);

				int activityNodeA = 3;
//				int activityNodeA = MatsimRandom.getLocalInstance().nextInt(8)+1;
				Activity activityA = pf.createActivityFromCoord("other", sc.getNetwork().getNodes().get(Id.createNodeId(activityNodeA)).getCoord());
				activityA.setMaximumDuration(MatsimRandom.getLocalInstance().nextDouble()*Time.parseTime("00:01:00")); /* 1 minute */
				plan.addActivity(activityA);
				Leg anotherLeg = pf.createLeg(mode.toString());
				plan.addLeg(anotherLeg);

				int activityNodeB = 2;
//				int activityNodeB = MatsimRandom.getLocalInstance().nextInt(8)+1;
				Activity activityB = pf.createActivityFromCoord("other", sc.getNetwork().getNodes().get(Id.createNodeId(activityNodeB)).getCoord());
				activityB.setMaximumDuration(MatsimRandom.getLocalInstance().nextDouble()*Time.parseTime("00:01:00")); /* 1 minute */
				plan.addActivity(activityB);
				Leg yetAnotherLeg = pf.createLeg(mode.toString());
				plan.addLeg(yetAnotherLeg);
			}
			
			/* Add the final home activity. */
			Activity home2 = pf.createActivityFromCoord("home", home1.getCoord());
//			home2.setStartTime(Time.parseTime("24:00:00"));
			plan.addActivity(home2);
			person.addPlan(plan);
			
			sc.getPopulation().addPerson(person);
			
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
	IterationEndsListener, MobsimInitializedListener{
		private final Logger log = Logger.getLogger(LinkCounter.class);
		private List<String> spaceTimeList;
		private Map<Id<Link>, List<Tuple<Double,Integer>>> countMap;
		private Map<Id<Vehicle>, List<Double>> spaceTimeMap;
		private final Scenario sc;
		private Id<Link> initialiser;
		private List<Id<Link>> linksChecked;
		private QSim qsim;
		
		public LinkCounter(final Scenario sc) {
			this.sc = sc;
			this.spaceTimeList = new ArrayList<String>();
			this.countMap = new TreeMap<Id<Link>, List<Tuple<Double, Integer>>>();
			this.spaceTimeMap = new TreeMap<Id<Vehicle>, List<Double>>();
			
			/* Initialise the link counts, knowing what the correct Ids are. */
			for(Id<Link> id : this.sc.getNetwork().getLinks().keySet()){
				this.countMap.put(id, new ArrayList<Tuple<Double, Integer>>());
				this.countMap.get(id).add(new Tuple<Double, Integer>(0.0, 0));
			}
			
			/* Setup link Id listeners */
			this.initialiser = Id.createLinkId("34");
			this.linksChecked = new ArrayList<Id<Link>>();
			this.linksChecked.add(Id.createLinkId("45"));
			this.linksChecked.add(Id.createLinkId("56"));
			this.linksChecked.add(Id.createLinkId("67"));
			this.linksChecked.add(Id.createLinkId("78"));
			this.linksChecked.add(Id.createLinkId("81"));
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
			
			/* Only check the space-time phenomenon for 01:00:00 - 02:00:00 */
			if(event.getTime() >= Time.parseTime("01:00:00") &&
					event.getTime() <= Time.parseTime("24:00:00")){

				if(event.getLinkId().equals(initialiser)){ 
					spaceTimeMap.put(event.getVehicleId(), new ArrayList<Double>());
					spaceTimeMap.get(event.getVehicleId()).add(event.getTime());
				} else if(this.linksChecked.contains(event.getLinkId()) && spaceTimeMap.containsKey(event.getVehicleId())){
					spaceTimeMap.get(event.getVehicleId()).add(event.getTime());
				}
			}
		}

		
		@Override
		public void handleEvent(LinkLeaveEvent event) {
			/* Deduct the pcu-equivalents to the NetsimLink. */
			NetsimLink netsimLink = this.qsim.getNetsimNetwork().getNetsimLink(event.getLinkId());
			double pcuEquivalent = this.sc.getVehicles().getVehicles().get(event.getVehicleId()).getType().getPcuEquivalents();
			double oldPcuTotal = (double) netsimLink.getCustomAttributes().get("pcu");
			netsimLink.getCustomAttributes().put("pcu", oldPcuTotal-pcuEquivalent);

			double time = event.getTime();
			Id<Link> linkId = event.getLinkId();
			int oldCount = this.countMap.get(linkId).get( this.countMap.get(linkId).size()-1 ).getSecond();
			this.countMap.get(linkId).add(new Tuple<Double, Integer>(time, oldCount-1));
			
			/* Write the space-time chain to file, and then remove the person. */
			if(event.getLinkId().equals(Id.createLinkId("81")) && this.spaceTimeMap.containsKey(event.getVehicleId())){
				List<Double> thisList = this.spaceTimeMap.get(event.getVehicleId());
				thisList.add(event.getTime());
				if(thisList.size() == 7){
					String s = "";
					s += event.getVehicleId() + ",";
					s += this.sc.getVehicles().getVehicles().get(event.getVehicleId()).getType().getId().toString() + ",";
					for(int i = 0; i < thisList.size()-1; i++){
						s += String.valueOf(thisList.get(i)) + ",";
					}
					s += String.valueOf(thisList.get(thisList.size()-1));
					this.spaceTimeList.add(s);
				}
				this.spaceTimeMap.remove(event.getVehicleId());
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
				bw.write("id,type,link34,link45,link56,link67,link78,link81,end");
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
			
			
			
			
		}

		@Override
		public void notifyMobsimInitialized(MobsimInitializedEvent e) {
			this.log.info(" ==> Initialising the custom pcu attributes.");
			this.qsim = (QSim) e.getQueueSimulation();
			for(NetsimLink link : this.qsim.getNetsimNetwork().getNetsimLinks().values()){
				link.getCustomAttributes().put("pcu", 0.0);
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
