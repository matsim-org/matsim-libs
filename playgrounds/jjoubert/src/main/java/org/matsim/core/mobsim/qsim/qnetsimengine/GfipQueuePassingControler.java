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
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

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
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.api.experimental.network.NetworkWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.PlanStrategyRegistrar;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.mobsim.qsim.qnetsimengine.GfipQueuePassingQSimFactory.QueueType;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Time;

import playground.southafrica.utilities.FileUtils;
import playground.southafrica.utilities.Header;

public class GfipQueuePassingControler extends Controler{

	public GfipQueuePassingControler(Scenario scenario) {
		super(scenario);
		
		this.setMobsimFactory(new GfipQueuePassingQSimFactory(QueueType.FIFO));
		
		/*TODO May want to add a time/density listener in here... */
		
		
	}
	
	public static void main(String[] args){
		Header.printHeader(GfipQueuePassingControler.class.toString(), args);

		String outputDirectory = args[0];
		FileUtils.delete(new File(outputDirectory));
		
		Config config = getDefaultConfig();
		config.controler().setOutputDirectory(outputDirectory);
		
		Scenario sc = ScenarioUtils.loadScenario(config);
		
		buildOctagonScenario(sc);
//		new NetworkWriter(sc.getNetwork()).write("/Users/jwjoubert/Downloads/network.xml");
//		new PopulationWriter(sc.getPopulation()).write("/Users/jwjoubert/Downloads/population.xml");
		
		GfipQueuePassingControler controler = new GfipQueuePassingControler(sc);
		
		LinkCounter linkCounter = controler.new LinkCounter(sc);
		controler.getEvents().addHandler(linkCounter);
		controler.addControlerListener(linkCounter);
		controler.run();
		
		Header.printFooter();
	}
	
	
	private static Config getDefaultConfig(){
		Config config  = ConfigUtils.createConfig();
		
		/* Set all the defaults we want. */
		config.controler().setLastIteration(1);
		config.controler().setWriteEventsInterval(1);
		
		String[] modes ={"car"};
		config.qsim().setMainModes( Arrays.asList(modes) );
		config.qsim().setEndTime(Double.POSITIVE_INFINITY);
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
		best.setProbability(1.0);
		best.setModuleName(PlanStrategyRegistrar.Selector.ChangeExpBeta.toString());
		config.strategy().addStrategySettings(best);
		
		return config;
	}
	
	
	private static void buildOctagonScenario(Scenario sc){
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
		
		Link l12 = nf.createLink(Id.createLinkId("12"), n1, n2);
		l12.setLength(2000.0);
		l12.setCapacity(5000.0);
		l12.setNumberOfLanes(2);
		l12.setFreespeed(100.0/3.6);
		Link l23 = nf.createLink(Id.createLinkId("23"), n2, n3);
		l23.setLength(2000.0);
		l23.setCapacity(5000.0);
		l23.setNumberOfLanes(2);
		l23.setFreespeed(100.0/3.6);
		Link l34 = nf.createLink(Id.createLinkId("34"), n3, n4);
		l34.setLength(2000.0);
		l34.setCapacity(5000.0);
		l34.setNumberOfLanes(2);
		l34.setFreespeed(100.0/3.6);
		Link l45 = nf.createLink(Id.createLinkId("45"), n4, n5);
		l45.setLength(2000.0);
		l45.setCapacity(5000.0);
		l45.setNumberOfLanes(2);
		l45.setFreespeed(100.0/3.6);
		Link l56 = nf.createLink(Id.createLinkId("56"), n5, n6);
		l56.setLength(2000.0);
		l56.setCapacity(5000.0);
		l56.setNumberOfLanes(2);
		l56.setFreespeed(100.0/3.6);
		Link l67 = nf.createLink(Id.createLinkId("67"), n6, n7);
		l67.setLength(2000.0);
		l67.setCapacity(5000.0);
		l67.setNumberOfLanes(2);
		l67.setFreespeed(100.0/3.6);
		Link l78 = nf.createLink(Id.createLinkId("78"), n7, n8);
		l78.setLength(2000.0);
		l78.setCapacity(5000.0);
		l78.setNumberOfLanes(2);
		l78.setFreespeed(100.0/3.6);
		Link l81 = nf.createLink(Id.createLinkId("81"), n8, n1);
		l81.setLength(2000.0);
		l81.setCapacity(5000.0);
		l81.setNumberOfLanes(2);
		l81.setFreespeed(100.0/3.6);
		sc.getNetwork().addLink(l12);
		sc.getNetwork().addLink(l23);
		sc.getNetwork().addLink(l34);
		sc.getNetwork().addLink(l45);
		sc.getNetwork().addLink(l56);
		sc.getNetwork().addLink(l67);
		sc.getNetwork().addLink(l78);
		sc.getNetwork().addLink(l81);
		
		/* Create the population. */
		PopulationFactory pf = sc.getPopulation().getFactory();
		for(int i = 0; i < 500; i++){
			Person person = pf.createPerson(Id.createPersonId(i));
			Plan plan = pf.createPlan();
			
			/* Add the first home activity. */
//			int homeNode = MatsimRandom.getLocalInstance().nextInt(8)+1;
			int homeNode = 1;
			Activity home1 = pf.createActivityFromCoord("home", sc.getNetwork().getNodes().get(Id.createNodeId(homeNode)).getCoord());
//			home1.setEndTime(MatsimRandom.getLocalInstance().nextDouble()*Time.parseTime("20:00:00"));
			home1.setEndTime(Time.parseTime("00:01:00"));
			plan.addActivity(home1);
			Leg leg = pf.createLeg("car");
			plan.addLeg(leg);
			
			int activityNodeA = 3;
			Activity activityA = pf.createActivityFromCoord("other", sc.getNetwork().getNodes().get(Id.createNodeId(activityNodeA)).getCoord());
			activityA.setMaximumDuration(Time.parseTime("00:01:00")); /* 1 minute */
//			activityA.setMaximumDuration(MatsimRandom.getLocalInstance().nextDouble()*Time.parseTime("00:01:00")); /* 1 minute */
			plan.addActivity(activityA);
			Leg anotherLeg = pf.createLeg("car");
			plan.addLeg(anotherLeg);
			
//			
//			/* Add 100 activities. */
//			for(int j = 0; j < 50; j++){
////				int activityNode = MatsimRandom.getLocalInstance().nextInt(8)+1;
////				Activity activity = pf.createActivityFromCoord("other", sc.getNetwork().getNodes().get(Id.createNodeId(activityNode)).getCoord());
////				activity.setMaximumDuration(Time.parseTime("00:01:00")); /* 1 minute */
////				plan.addActivity(activity);
////				Leg anotherLeg = pf.createLeg("car");
////				plan.addLeg(anotherLeg);
//
//				int activityNodeA = 3;
//				Activity activityA = pf.createActivityFromCoord("other", sc.getNetwork().getNodes().get(Id.createNodeId(activityNodeA)).getCoord());
//				activityA.setMaximumDuration(MatsimRandom.getLocalInstance().nextDouble()*Time.parseTime("00:01:00")); /* 1 minute */
//				plan.addActivity(activityA);
//				Leg anotherLeg = pf.createLeg("car");
//				plan.addLeg(anotherLeg);
//
//				int activityNodeB = 2;
//				Activity activityB = pf.createActivityFromCoord("other", sc.getNetwork().getNodes().get(Id.createNodeId(activityNodeB)).getCoord());
//				activityB.setMaximumDuration(MatsimRandom.getLocalInstance().nextDouble()*Time.parseTime("00:01:00")); /* 1 minute */
//				plan.addActivity(activityB);
//				Leg yetAnotherLeg = pf.createLeg("car");
//				plan.addLeg(yetAnotherLeg);
//			}
			
			/* Add the final home activity. */
			Activity home2 = pf.createActivityFromCoord("home", home1.getCoord());
//			home2.setStartTime(Time.parseTime("24:00:00"));
			plan.addActivity(home2);
			person.addPlan(plan);
			
			sc.getPopulation().addPerson(person);
		}
		
	}
	
	private class LinkCounter implements LinkEnterEventHandler, LinkLeaveEventHandler, IterationEndsListener{
		private final Logger log = Logger.getLogger(LinkCounter.class);
		private List<String> list;
		private Map<Id<Link>, List<Tuple<Double,Integer>>> countMap;
		private final Scenario sc;
		
		public LinkCounter(final Scenario sc) {
			this.sc = sc;
			this.list = new ArrayList<String>();
			this.countMap = new TreeMap<Id<Link>, List<Tuple<Double, Integer>>>();
			
			/* Initialise the link counts, knowing what the correct Ids are. */
			for(Id<Link> id : this.sc.getNetwork().getLinks().keySet()){
				this.countMap.put(id, new ArrayList<Tuple<Double, Integer>>());
				this.countMap.get(id).add(new Tuple<Double, Integer>(0.0, 0));
			}
		}

		@Override
		public void reset(int iteration) {
			this.list = new ArrayList<String>();
			this.countMap = new TreeMap<Id<Link>, List<Tuple<Double, Integer>>>();
			
			/* Initialise the link counts, knowing what the correct Ids are. */
			for(Id<Link> id : this.sc.getNetwork().getLinks().keySet()){
				this.countMap.put(id, new ArrayList<Tuple<Double, Integer>>());
				this.countMap.get(id).add(new Tuple<Double, Integer>(0.0, 0));
			}
		}

		@Override
		public void handleEvent(LinkEnterEvent event) {
			double time = event.getTime();
			Id<Link> linkId = event.getLinkId();
			int oldCount = this.countMap.get(linkId).get( this.countMap.get(linkId).size()-1 ).getSecond();
			this.countMap.get(linkId).add(new Tuple<Double, Integer>(time, oldCount+1));
		}

		@Override
		public void notifyIterationEnds(IterationEndsEvent event) {
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
		}

		@Override
		public void handleEvent(LinkLeaveEvent event) {
			double time = event.getTime();
			Id<Link> linkId = event.getLinkId();
			int oldCount = this.countMap.get(linkId).get( this.countMap.get(linkId).size()-1 ).getSecond();
			this.countMap.get(linkId).add(new Tuple<Double, Integer>(time, oldCount-1));
		}
		
	}
	
}
