/* *********************************************************************** *
 * project: org.matsim.*
 * PseudoSim.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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
package playground.johannes.socialnetworks.sim.interaction;

import gnu.trove.TObjectIntIterator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.LinkNetworkRoute;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.Route;
import org.matsim.contrib.sna.gis.CRSUtils;
import org.matsim.core.api.experimental.events.ActivityEndEvent;
import org.matsim.core.api.experimental.events.ActivityEvent;
import org.matsim.core.api.experimental.events.ActivityStartEvent;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.ActivityEndEventImpl;
import org.matsim.core.events.ActivityStartEventImpl;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.facilities.FacilitiesReaderMatsimV1;
import org.matsim.core.network.NetworkReaderMatsimV1;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PopulationReaderMatsimV4;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;
import org.matsim.core.trafficmonitoring.TravelTimeCalculatorConfigGroup;
import org.xml.sax.SAXException;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;

import playground.johannes.socialnetworks.graph.social.SocialPerson;
import playground.johannes.socialnetworks.graph.social.io.SocialGraphMLWriter;
import playground.johannes.socialnetworks.survey.ivt2009.graph.SocialSparseGraph;
import playground.johannes.socialnetworks.survey.ivt2009.graph.SocialSparseGraphBuilder;

/**
 * @author illenberger
 * 
 */
public class PseudoSim {

	private static final Logger logger = Logger.getLogger(PseudoSim.class);

	private Queue<ActivityEvent> eventQueue;

	private Network network;

	// private TravelTime travelTime;

	private final Comparator<? super ActivityEvent> comparator = new Comparator<ActivityEvent>() {

		@Override
		public int compare(ActivityEvent o1, ActivityEvent o2) {
			double r = o1.getTime() - o2.getTime();
			if (r > 0)
				return 1;
			if (r < 0)
				return -1;
			else {
				if (o1 == o2)
					return 0;
				else
					return o2.hashCode() - o1.hashCode();
			}
		}
	};

	public void run(Population population, Network network, TravelTime travelTime, EventsManagerImpl eventManager) {
		this.network = network;

		eventManager.resetHandlers(0);
		eventQueue = new PriorityQueue<ActivityEvent>(population.getPersons().size(), comparator);

		logger.info("Creating events...");

		for (Person person : population.getPersons().values()) {
			Plan plan = person.getSelectedPlan();
			List<PlanElement> elements = plan.getPlanElements();

			boolean carMode = true;
			for (int i = 1; i < elements.size(); i += 2) {
				if (((Leg) elements.get(i)).getMode() != TransportMode.car) {
					carMode = false;
					break;
				}
			}

			if (carMode) {

				double lastEndTime = 0;
				for (int i = 0; i < elements.size(); i += 2) {
					Activity act = (Activity) elements.get(i);
					double startTime = 0.0;
					double endTime = act.getEndTime();

					if (i > 0) {
						LinkNetworkRoute route = (LinkNetworkRoute) ((Leg) elements.get(i - 1)).getRoute();
						double tt = calcRouteTravelTime(route, lastEndTime, travelTime);
						startTime = tt + lastEndTime;
					}

					if(startTime >= 86400)
						startTime = 86399;
					
					if (i == elements.size() - 1) {
						endTime = 86400;
					}
					
					ActivityEvent startEvent = new ActivityStartEventImpl(startTime, person.getId(), act.getLinkId(),
							act.getFacilityId(), act.getType());
					ActivityEvent endEvent = new ActivityEndEventImpl(endTime, person.getId(), act.getLinkId(), act
							.getFacilityId(), act.getType());

					eventQueue.add(startEvent);
					eventQueue.add(endEvent);
					lastEndTime = endEvent.getTime();
				}
			}
		}

		logger.info("Processing events...");

		for (ActivityEvent event : eventQueue)
			eventManager.processEvent(event);
	}

	private double calcRouteTravelTime(LinkNetworkRoute route, double startTime, TravelTime travelTime) {
		double tt = 0;
		List<Id> ids = route.getLinkIds();
		for (int i = 1; i < ids.size(); i++) {
			tt += travelTime.getLinkTravelTime(network.getLinks().get(ids.get(i)), startTime);
		}
		return Math.max(tt, 1.0);
	}

	public static void main(String args[]) throws SAXException, ParserConfigurationException, IOException {
		String netFile = args[0];
		String facFile = args[1];
		String popFile = args[2];
		String graphFile = args[3];
		double proba = Double.parseDouble(args[4]);

		GeometryFactory geoFactory = new GeometryFactory();

		Scenario scenario = new ScenarioImpl();

		NetworkReaderMatsimV1 netReader = new NetworkReaderMatsimV1(scenario);
		netReader.parse(netFile);

		FacilitiesReaderMatsimV1 facReader = new FacilitiesReaderMatsimV1((ScenarioImpl) scenario);
		facReader.parse(facFile);

		PopulationReaderMatsimV4 reader = new PopulationReaderMatsimV4(scenario);
		reader.readFile(popFile);

		logger.info("Building empty graph...");
		SocialSparseGraphBuilder builder = new SocialSparseGraphBuilder(CRSUtils.getCRS(21781));
		SocialSparseGraph graph = builder.createGraph();

		for (Person person : scenario.getPopulation().getPersons().values()) {
			SocialPerson sPerson = new SocialPerson((PersonImpl) person);
			Coord home = ((Activity) person.getSelectedPlan().getPlanElements().get(0)).getCoord();
			builder.addVertex(graph, sPerson, geoFactory.createPoint(new Coordinate(home.getX(), home.getY())));
		}

		logger.info("Initializing interactors...");

		BefriendInteractor interactor = new BefriendInteractor(graph, builder, proba, 0);
		InteractionSelector selector = new InteractionSelector() {
			@Override
			public Collection<Id> select(Id v, Collection<Id> choiceSet) {
				return choiceSet;
			}
		};

		InteractionHandler handler = new InteractionHandler(selector, interactor);

		EventsManagerImpl manager = new EventsManagerImpl();
		manager.addHandler(handler);

		TravelTime travelTime = new TravelTimeCalculator(scenario.getNetwork(), 60 * 60, 24 * 60 * 60,
				new TravelTimeCalculatorConfigGroup());
		PseudoSim sim = new PseudoSim();
		logger.info("Running pseudo sim...");
		sim.run(scenario.getPopulation(), scenario.getNetwork(), travelTime, manager);

		TObjectIntIterator<String> it = interactor.getActTypes().iterator();
		for (int i = 0; i < interactor.getActTypes().size(); i++) {
			it.advance();
			logger.info(String.format("Act type = %1$s, edges = %2$s.", it.key(), it.value()));
		}

		logger.info("Writing graph...");
		SocialGraphMLWriter writer = new SocialGraphMLWriter();
		writer.setPopulationFile(popFile);
		writer.write(graph, graphFile);
		logger.info("Done.");

	}
}
