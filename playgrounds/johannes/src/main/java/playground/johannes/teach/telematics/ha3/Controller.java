/* *********************************************************************** *
 * project: org.matsim.*
 * Controller.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

/**
 * 
 */
package playground.johannes.teach.telematics.ha3;

import gnu.trove.TDoubleArrayList;
import gnu.trove.TIntObjectHashMap;
import gnu.trove.TObjectDoubleHashMap;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.commons.math.stat.StatUtils;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.handler.AgentArrivalEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentDepartureEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkEnterEventHandler;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.controler.corelisteners.PlansReplanning;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.events.BeforeMobsimEvent;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.events.ScoringEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.controler.listener.BeforeMobsimListener;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.controler.listener.ScoringListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.network.NetworkChangeEvent;
import org.matsim.core.network.NetworkChangeEvent.ChangeType;
import org.matsim.core.network.NetworkChangeEvent.ChangeValue;
import org.matsim.core.population.routes.NetworkRouteWRefs;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.withinday.WithindayControler;
import org.matsim.withinday.mobsim.WithindayQueueSimulation;
import org.matsim.withinday.trafficmanagement.TrafficManagement;

import playground.johannes.eut.EstimReactiveLinkTT;
import playground.johannes.eut.EventBasedTTProvider;




/**
 * @author illenberger
 *
 */
public class Controller extends WithindayControler {

	private TravelTime reactTTs;
	
	private double equipmentFraction;
	
	private GuidedAgentFactory factory2;
	
	public Controller(String[] args) {
		super(args);
		setOverwriteFiles(true);
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Controller c = new Controller(args);
		c.setCreateGraphs(false);
		c.setWriteEventsInterval(0);
		c.run();
	}
	
	@Override
	protected void setUp() {
		
		
		
		this.equipmentFraction = string2Double(config.getParam("telematics", "equipment"));
		
		String type = config.getParam("telematics", "infotype");
		if("reactive".equalsIgnoreCase(type)) {
			this.reactTTs = new EventBasedTTProvider(30);
		} else if("estimated".equalsIgnoreCase(type)) {
			this.reactTTs = new EstimReactiveLinkTT(1, this.network);
		} else
			throw new IllegalArgumentException("Travel time information type \"" + type + "\" is unknown!");
		
		this.events.addHandler((EventHandler) this.reactTTs);
		((EventHandler) this.reactTTs).reset(getIterationNumber());
		
		factory2 = new GuidedAgentFactory(network, config.charyparNagelScoring(), reactTTs, equipmentFraction, config.global().getRandomSeed());
		RouteTTObserver observer = new RouteTTObserver(this.getControlerIO().getOutputFilename("routeTravelTimes.txt"));
		observer.factory = factory2;
		NonSelectedPlanScorer scorer = new NonSelectedPlanScorer();
		scorer.observer = observer;
		
		this.addControlerListener(scorer);
		this.addControlerListener(observer);
		this.events.addHandler(observer);
		
		IncidentGenerator generator = new IncidentGenerator(getConfig().getParam("telematics", "incidentsFile"), getNetwork());
		this.addControlerListener(generator);
		
		addControlerListener(new WithindayControlerListener());
		super.setUp();
	}
	
	@Override
	protected void loadCoreListeners() {

		/* The order how the listeners are added is very important!
		 * As dependencies between different listeners exist or listeners
		 * may read and write to common variables, the order is important.
		 * Example: The RoadPricing-Listener modifies the scoringFunctionFactory,
		 * which in turn is used by the PlansScoring-Listener.
		 * Note that the execution order is contrary to the order the listeners are added to the list.
		 */

		this.addCoreControlerListener(new CoreControlerListener());

		// the default handling of plans
//		this.plansScoring = new PlansScoring();
//		this.addCoreControlerListener(this.plansScoring);


		// load road pricing, if requested
//		if (this.config.roadpricing().getTollLinksFile() != null) {
//			this.roadPricing = new RoadPricing();
//			this.addCoreControlerListener(this.roadPricing);
//		}

		this.addCoreControlerListener(new PlansReplanning());
//		this.addCoreControlerListener(new PlansDumping());
	}
	
	@Override
	protected void runMobSim() {

		this.config.withinday().addParam("contentThreshold", "1");
		this.config.withinday().addParam("replanningInterval", "1");

		WithindayQueueSimulation sim = new WithindayQueueSimulation(this.scenarioData, this.events, this);
		this.trafficManagement = new TrafficManagement();
		sim.setTrafficManagement(this.trafficManagement);
		
		sim.run();
	}

	public Set<Person> getGuidedPersons() {
		return ((GuidedAgentFactory)factory).getGuidedPersons();
	}
	
	private class WithindayControlerListener implements StartupListener, IterationStartsListener {

		public void notifyStartup(StartupEvent event) {
//			factory2 = new GuidedAgentFactory(Controller.this.network, Controller.this.config.charyparNagelScoring(), Controller.this.reactTTs, Controller.this.equipmentFraction);
			Controller.this.factory = factory2;

		}

		public void notifyIterationStarts(IterationStartsEvent event) {
			((GuidedAgentFactory)Controller.this.factory).reset();
		}

	}

	
	public static class NonSelectedPlanScorer implements ScoringListener {

		private RouteTTObserver observer;
		
		public void notifyScoring(ScoringEvent event) {
			double alpha = Double.parseDouble(event.getControler().getConfig().getParam("planCalcScore", "learningRate"));
			
			for(Person p : event.getControler().getPopulation().getPersons().values()) {
				for(Plan plan : p.getPlans()) {
					double tt = 0;
					Leg leg = (Leg)plan.getPlanElements().get(1);
					Route route = leg.getRoute();
					for(Id id : ((NetworkRouteWRefs) route).getLinkIds()) {
						if(id.toString().equals("4")) {
							
							tt = observer.avr_route1TTs;
							break;
						} else if(id.toString().equals("5")) {
							tt = observer.avr_route2TTs;
							break;
						}
					}
					
					Double oldScore = plan.getScore();
					if(oldScore == null)
						oldScore = 0.0;
					
					plan.setScore(alpha * -tt/3600.0 + (1-alpha)*oldScore);
				}
			
				
			}
		}
	}
	
	public static class RouteTTObserver implements AgentDepartureEventHandler, AgentArrivalEventHandler, LinkEnterEventHandler, IterationEndsListener, AfterMobsimListener {

		private Set<Id> route1;
		
		private Set<Id> route2;
		
		private TObjectDoubleHashMap<Id> personTTs;
		
		private TObjectDoubleHashMap<Id> departureTimes;
		
		private BufferedWriter writer;
		
		private double avr_route1TTs;
		
		private double avr_route2TTs;
		
		private double avr_unguidedTTs;
		
		private double avr_guidedTTs;
		
		private GuidedAgentFactory factory;
			
		public RouteTTObserver(String filename) {
			try {
				writer = org.matsim.core.utils.io.IOUtils.getBufferedWriter(filename);
				writer.write("it\tn_1\tn_2\ttt_1\ttt_2\tn_guided\tn_unguided\ttt_guided\ttt_unguided");
				writer.newLine();
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			this.reset(0);
		}
		
		public void handleEvent(AgentDepartureEvent event) {
			departureTimes.put(event.getPersonId(), event.getTime());
		}

		public void reset(int iteration) {
			route1 = new HashSet<Id>();
			route2 = new HashSet<Id>();
			personTTs = new TObjectDoubleHashMap<Id>();
			departureTimes = new TObjectDoubleHashMap<Id>();
		}

		public void handleEvent(AgentArrivalEvent event) {
			double depTime = departureTimes.get(event.getPersonId());
			if(depTime == 0)
				throw new RuntimeException("Agent departure time not found!");
			
			personTTs.put(event.getPersonId(), event.getTime() - depTime);
		}

		public void handleEvent(LinkEnterEvent event) {
			if(event.getLinkId().toString().equals("4")) {
				route1.add(event.getPersonId());
			} else if(event.getLinkId().toString().equals("5")) {
				route2.add(event.getPersonId());
			}
		}

		public void notifyIterationEnds(IterationEndsEvent event) {
			
			
			try {
				writer.write(String.valueOf(event.getIteration()));
				writer.write("\t");
				writer.write(String.valueOf(route1.size()));
				writer.write("\t");
				writer.write(String.valueOf(route2.size()));
				writer.write("\t");
				
				if(route1.isEmpty())
					writer.write("0");
				else
					writer.write(String.valueOf(avr_route1TTs));
				writer.write("\t");
				
				if(route2.isEmpty())
					writer.write("0");
				else
					writer.write(String.valueOf(avr_route2TTs));
				
				writer.write("\t");
				writer.write(String.valueOf(factory.getGuidedPersons().size()));
				writer.write("\t");
				writer.write(String.valueOf(factory.getUnguidedPersons().size()));
				writer.write("\t");
				
				writer.write(String.valueOf(avr_guidedTTs));
				writer.write("\t");
				writer.write(String.valueOf(avr_unguidedTTs));
				
				
				writer.newLine();
				writer.flush();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		public void notifyAfterMobsim(AfterMobsimEvent event) {
			TDoubleArrayList route1TTs = new TDoubleArrayList();
			TDoubleArrayList route2TTs = new TDoubleArrayList();
			
			TDoubleArrayList unguidedTTs = new TDoubleArrayList();
			TDoubleArrayList guidedTTs = new TDoubleArrayList();
			
			for(Id p : route1) {
				route1TTs.add(personTTs.get(p));
			}
			for(Id p : route2) {
				route2TTs.add(personTTs.get(p));
			}
			
			avr_route1TTs = StatUtils.mean(route1TTs.toNativeArray());
			avr_route2TTs = StatUtils.mean(route2TTs.toNativeArray());
			
			for(Person p : factory.getGuidedPersons())
				guidedTTs.add(personTTs.get(p.getId()));
			
			for(Person p : factory.getUnguidedPersons())
				unguidedTTs.add(personTTs.get(p.getId()));
			
			avr_guidedTTs = StatUtils.mean(guidedTTs.toNativeArray());
			avr_unguidedTTs = StatUtils.mean(unguidedTTs.toNativeArray());
			
			if(Double.isNaN(avr_route1TTs)) {
				avr_route1TTs = getFreespeedTravelTime(event.getControler().getNetwork().getLinks().get(new IdImpl("2")));
				avr_route1TTs += getFreespeedTravelTime(event.getControler().getNetwork().getLinks().get(new IdImpl("4")));
				avr_route1TTs += getFreespeedTravelTime(event.getControler().getNetwork().getLinks().get(new IdImpl("6")));
			} if(Double.isNaN(avr_route2TTs)) {
				avr_route2TTs = getFreespeedTravelTime(event.getControler().getNetwork().getLinks().get(new IdImpl("3")));
				avr_route2TTs += getFreespeedTravelTime(event.getControler().getNetwork().getLinks().get(new IdImpl("5")));
				avr_route2TTs += getFreespeedTravelTime(event.getControler().getNetwork().getLinks().get(new IdImpl("6")));
			}
		}
		
		private double getFreespeedTravelTime(final Link link) {
			return link.getLength() / link.getFreespeed(Time.UNDEFINED_TIME);
		}

	}
	
	private class IncidentGenerator implements BeforeMobsimListener {

		private TIntObjectHashMap<List<NetworkChangeEvent>> changeEvents;
		
		public IncidentGenerator(String filename, Network network) {
			try {
				changeEvents = new TIntObjectHashMap<List<NetworkChangeEvent>>();
				
				BufferedReader reader = IOUtils.getBufferedReader(filename);
				String line = null;
				while((line = reader.readLine()) != null) {
					String[] tokens = line.split("\t");
					
					NetworkChangeEvent badEvent = new NetworkChangeEvent(0);
					badEvent.addLink(network.getLinks().get(new IdImpl(tokens[1])));
					badEvent.setFlowCapacityChange(new ChangeValue(ChangeType.FACTOR, Double.parseDouble(tokens[2])));
//					
					int it = Integer.parseInt(tokens[0]);
					List<NetworkChangeEvent> events = changeEvents.get(it);
					if(events == null) {
						events = new LinkedList<NetworkChangeEvent>();
						changeEvents.put(it, events);
					}
					events.add(badEvent);
				}
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		public void notifyBeforeMobsim(BeforeMobsimEvent event) {
			List<NetworkChangeEvent> events = changeEvents.get(event.getIteration());
			if(events != null) {
				(event.getControler().getNetwork()).setNetworkChangeEvents(events);
			} else
				(event.getControler().getNetwork()).setNetworkChangeEvents(new LinkedList<NetworkChangeEvent>());
		}
		
	}

	private double string2Double(String str) {
		if(str.endsWith("%"))
			return Integer.parseInt(str.substring(0, str.length()-1))/100.0;
		else
			return Double.parseDouble(str);

	}
}
