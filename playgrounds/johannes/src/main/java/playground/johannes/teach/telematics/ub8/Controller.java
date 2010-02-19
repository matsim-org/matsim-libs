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
package playground.johannes.teach.telematics.ub8;

import gnu.trove.TDoubleArrayList;
import gnu.trove.TIntObjectHashMap;
import gnu.trove.TObjectDoubleHashMap;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.apache.commons.math.stat.StatUtils;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.handler.AgentArrivalEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentDepartureEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkEnterEventHandler;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.corelisteners.PlansReplanning;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.events.BeforeMobsimEvent;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.ScoringEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.controler.listener.BeforeMobsimListener;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.ScoringListener;
import org.matsim.core.network.NetworkChangeEvent;
import org.matsim.core.network.NetworkChangeEvent.ChangeType;
import org.matsim.core.network.NetworkChangeEvent.ChangeValue;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.routes.LinkNetworkRouteFactory;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteWRefs;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Time;



/**
 * @author illenberger
 *
 */
public class Controller extends Controler {

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
		super.setUp();
		RouteTTObserver observer = new RouteTTObserver(this.getControlerIO().getOutputFilename("routeTravelTimes.txt"));
		NonSelectedPlanScorer scorer = new NonSelectedPlanScorer();
		scorer.observer = observer;
		this.addControlerListener(scorer);
		this.addControlerListener(observer);
		this.events.addHandler(observer);

		IncidentGenerator generator = new IncidentGenerator(getConfig().getParam("telematics", "incidentsFile"), getNetwork());
		this.addControlerListener(generator);

		ForceRouteSwitch routeSwitch = new ForceRouteSwitch();
		routeSwitch.proba = Double.parseDouble(getConfig().getParam("telematics", "p_forceSwitch"));
		this.addControlerListener(routeSwitch);
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

	public static class NonSelectedPlanScorer implements ScoringListener {

		private RouteTTObserver observer;

		public void notifyScoring(ScoringEvent event) {
			double alpha = Double.parseDouble(event.getControler().getConfig().getParam("planCalcScore", "learningRate"));

			for(Person p : event.getControler().getPopulation().getPersons().values()) {
				for(Plan plan : p.getPlans()) {
					double tt = 0;
					LegImpl leg = (LegImpl)plan.getPlanElements().get(1);
					RouteWRefs route = leg.getRoute();
					for(Id id : ((NetworkRoute) route).getLinkIds()) {
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

	private class ForceRouteSwitch implements BeforeMobsimListener {

		private double proba;

		public void notifyBeforeMobsim(BeforeMobsimEvent event) {
//			NetworkLayer network = event.getControler().getNetwork();
			Random random = new Random(config.global().getRandomSeed());

			LinkNetworkRouteFactory factory = new LinkNetworkRouteFactory(event.getControler().getNetwork());
			LinkNetworkRouteImpl route = (LinkNetworkRouteImpl) factory.createRoute(new IdImpl("1"), new IdImpl("6"));
			List<Id> linkIds = new ArrayList<Id>();
			linkIds.add(new IdImpl("1.2"));
			linkIds.add(new IdImpl("3"));
			linkIds.add(new IdImpl("5"));
			route.setLinkIds(route.getStartLinkId(), linkIds, route.getEndLinkId());

			for(Person p : event.getControler().getPopulation().getPersons().values()) {
				if(random.nextDouble() <= proba) {
					Plan plan = p.getSelectedPlan();
					((LegImpl)plan.getPlanElements().get(1)).setRoute(route);
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

		public RouteTTObserver(String filename) {
			try {
				writer = org.matsim.core.utils.io.IOUtils.getBufferedWriter(filename);
				writer.write("it\tn_1\tn_2\ttt_1\ttt_2");
				writer.newLine();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
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

				writer.newLine();
				writer.flush();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		public void notifyAfterMobsim(AfterMobsimEvent event) {
			TDoubleArrayList route1TTs = new TDoubleArrayList();
			TDoubleArrayList route2TTs = new TDoubleArrayList();

			for(Id p : route1) {
				route1TTs.add(personTTs.get(p));
			}
			for(Id p : route2) {
				route2TTs.add(personTTs.get(p));
			}

			avr_route1TTs = StatUtils.mean(route1TTs.toNativeArray());
			avr_route2TTs = StatUtils.mean(route2TTs.toNativeArray());

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


//		public void notifyShutdown(ShutdownEvent event) {
//			Distribution distribution = new Distribution();
//			distribution.addAll(personTTs.getValues());
//			try {
//				Distribution.writeHistogram(distribution.absoluteDistribution(10), event.getControler().getOutputFilename("tthist.txt"));
//			} catch (FileNotFoundException e) {
//				e.printStackTrace();
//			} catch (IOException e) {
//				e.printStackTrace();
//			}
//
//		}

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
				e.printStackTrace();
			} catch (IOException e) {
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
}
