/* *********************************************************************** *
 * project: org.matsim.*
 * AnalyzeTravelTimes.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package playground.christoph.icem2011;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.apache.log4j.Logger;
import org.geotools.data.FeatureSource;
import org.geotools.feature.Feature;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.AgentStuckEvent;
import org.matsim.core.api.experimental.events.handler.AgentArrivalEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentDepartureEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentStuckEventHandler;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.misc.Counter;
import org.matsim.core.utils.misc.Time;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;

public class AnalyzeTravelTimes implements AgentArrivalEventHandler, AgentDepartureEventHandler, AgentStuckEventHandler, IterationEndsListener {

	private static final Logger log = Logger.getLogger(AnalyzeTravelTimes.class);
	
	private Scenario scenario;
	private String citySHPFile;
	private String cantonSHPFile;
	private MultiPolygon cityPolygon;
	private MultiPolygon cantonPolygon;
	private Collection<Id> affectedAgents;
	private Collection<Id> replanningAgents;
	
	private Collection<Id> activityInCityZurich;
	private Collection<Id> activityInCantonZurich;
	private Collection<Id> tripThroughCityZurich;
	private Collection<Id> tripThroughCantonZurich;
	private Map<Id, Double> activeTrips;
	private double affectedAgentsTravelTimes;
	private double replanningAgentsTravelTimes;
	private double activityInCityZurichTravelTimes;
	private double activityInCantonZurichTravelTimes;
	private double tripThroughCityZurichTravelTimes;
	private double tripThroughCantonZurichTravelTimes;
	private int affectedAgentsCount;
	private int replanningAgentsCount;
	private int activityInCityZurichCount;
	private int activityInCantonZurichCount;
	private int tripThroughCityZurichCount;
	private int tripThroughCantonZurichCount;
	
	public AnalyzeTravelTimes(Scenario scenario, String citySHPFile, String cantonSHPFile, Collection<Id> affectedAgents, Collection<Id> replanningAgents) {
		this.scenario = scenario;
		this.citySHPFile = citySHPFile;
		this.cantonSHPFile = cantonSHPFile;
		this.affectedAgents = affectedAgents;
		this.replanningAgents = replanningAgents;
		
		activityInCityZurich = new HashSet<Id>();
		activityInCantonZurich = new HashSet<Id>();
		tripThroughCityZurich = new HashSet<Id>();
		tripThroughCantonZurich = new HashSet<Id>();
		
		try {
			readSHPFiles();
		} catch (IOException e) {
			log.error("Error when reading SHP Files.");
			return;
		}
		
		identifyActivitiesInCityZurich();
		identifyActivitiesInCantonZurich();
		identifyTripsThroughCityZurich();
		identifyTripsThroughCantonZurich();
		
		reset(0);
	}
	
	private void readSHPFiles() throws IOException {
		
		log.info("Reading SHP Files...");
		FeatureSource featureSource;
		featureSource = ShapeFileReader.readDataFile(citySHPFile);
		for (Object o : featureSource.getFeatures()) {
			Feature feature = (Feature) o;
			cityPolygon = (MultiPolygon)feature.getAttribute(0);
		}
		
		featureSource = ShapeFileReader.readDataFile(cantonSHPFile);
		for (Object o : featureSource.getFeatures()) {
			Feature feature = (Feature) o;
			cantonPolygon = (MultiPolygon)feature.getAttribute(0);
		}
		log.info("done.");
	}
	
	private void identifyActivitiesInCityZurich() {
		
		log.info("Identify people with an activity inside the city of Zurich...");
		
		GeometryFactory geoFac = new GeometryFactory();
		Counter counter = new Counter("People with an activity inside the city of Zurich: ");
		
		for (Person person : scenario.getPopulation().getPersons().values()) {
			Plan plan = person.getSelectedPlan();
			
			for (PlanElement planElement : plan.getPlanElements()) {
				if (planElement instanceof Activity) {
					Activity activity = (Activity) planElement;
					Coord coord = activity.getCoord();
					Coordinate coordinate = new Coordinate(coord.getX(), coord.getY());
					Point point = geoFac.createPoint(coordinate);
					
					if (cityPolygon.contains(point)) {
						activityInCityZurich.add(person.getId());
						counter.incCounter();
						break;
					}
				}
			}
		}
		counter.printCounter();
		log.info("done.");
	}
	
	private void identifyActivitiesInCantonZurich() {
		
		log.info("Identify people with an activity inside the canton of Zurich...");
		
		GeometryFactory geoFac = new GeometryFactory();
		Counter counter = new Counter("People with an activity inside the canton of Zurich: ");
		
		for (Person person : scenario.getPopulation().getPersons().values()) {
			Plan plan = person.getSelectedPlan();
			
			for (PlanElement planElement : plan.getPlanElements()) {
				if (planElement instanceof Activity) {
					Activity activity = (Activity) planElement;
					Coord coord = activity.getCoord();
					Coordinate coordinate = new Coordinate(coord.getX(), coord.getY());
					Point point = geoFac.createPoint(coordinate);
					
					if (cantonPolygon.contains(point)) {
						activityInCantonZurich.add(person.getId());
						counter.incCounter();
						break;
					}
				}
			}
		}
		counter.printCounter();
		log.info("done.");
	}
	
	private void identifyTripsThroughCityZurich() {
		
		log.info("Identify people with a trip through the city of Zurich...");
		
		GeometryFactory geoFac = new GeometryFactory();
		Counter counter = new Counter("People with a trip through the city of Zurich: ");
		
		for (Person person : scenario.getPopulation().getPersons().values()) {
			Plan plan = person.getSelectedPlan();
			
			for (PlanElement planElement : plan.getPlanElements()) {
				if (planElement instanceof Leg) {
					Leg leg = (Leg) planElement;
					Route route = leg.getRoute();
					
					Coord coord;
					Coordinate coordinate;
					Point point;
					
					// check start link
					coord = scenario.getNetwork().getLinks().get(route.getStartLinkId()).getCoord();
					coordinate = new Coordinate(coord.getX(), coord.getY());
					point = geoFac.createPoint(coordinate);
					if (cityPolygon.contains(point)) {
						tripThroughCityZurich.add(person.getId());
						counter.incCounter();
						break;
					}
					
					// check end link
					coord = scenario.getNetwork().getLinks().get(route.getEndLinkId()).getCoord();
					coordinate = new Coordinate(coord.getX(), coord.getY());
					point = geoFac.createPoint(coordinate);
					if (cityPolygon.contains(point)) {
						tripThroughCityZurich.add(person.getId());
						counter.incCounter();
						break;
					}
				
					// check links on the route
					if (route instanceof NetworkRoute) {
						boolean inside = false;
						for (Id linkId : ((NetworkRoute) route).getLinkIds()) {
							coord = scenario.getNetwork().getLinks().get(linkId).getCoord();
							coordinate = new Coordinate(coord.getX(), coord.getY());
							point = geoFac.createPoint(coordinate);
							if (cityPolygon.contains(point)) {
								tripThroughCityZurich.add(person.getId());
								counter.incCounter();
								inside = true;
								break;
							}
						}
						if (inside) break;
					}
					
				}
			}
		}
		counter.printCounter();
		log.info("done.");
	}
	
	private void identifyTripsThroughCantonZurich() {
		
		log.info("Identify people with a trip through the canton of Zurich...");
		
		GeometryFactory geoFac = new GeometryFactory();
		Counter counter = new Counter("People with a trip through the canton of Zurich: ");
		
		for (Person person : scenario.getPopulation().getPersons().values()) {
			Plan plan = person.getSelectedPlan();
			
			for (PlanElement planElement : plan.getPlanElements()) {
				if (planElement instanceof Leg) {
					Leg leg = (Leg) planElement;
					Route route = leg.getRoute();
					
					Coord coord;
					Coordinate coordinate;
					Point point;
					
					// check start link
					coord = scenario.getNetwork().getLinks().get(route.getStartLinkId()).getCoord();
					coordinate = new Coordinate(coord.getX(), coord.getY());
					point = geoFac.createPoint(coordinate);
					if (cantonPolygon.contains(point)) {
						tripThroughCantonZurich.add(person.getId());
						counter.incCounter();
						break;
					}
					
					// check end link
					coord = scenario.getNetwork().getLinks().get(route.getEndLinkId()).getCoord();
					coordinate = new Coordinate(coord.getX(), coord.getY());
					point = geoFac.createPoint(coordinate);
					if (cantonPolygon.contains(point)) {
						tripThroughCantonZurich.add(person.getId());
						counter.incCounter();
						break;
					}
				
					// check links on the route
					if (route instanceof NetworkRoute) {
						boolean inside = false;
						for (Id linkId : ((NetworkRoute) route).getLinkIds()) {
							coord = scenario.getNetwork().getLinks().get(linkId).getCoord();
							coordinate = new Coordinate(coord.getX(), coord.getY());
							point = geoFac.createPoint(coordinate);
							if (cantonPolygon.contains(point)) {
								tripThroughCantonZurich.add(person.getId());
								counter.incCounter();
								inside = true;
								break;
							}
						}
						if (inside) break;
					}
					
				}
			}
		}
		counter.printCounter();
		log.info("done.");
	}

	@Override
	public void handleEvent(AgentArrivalEvent event) {
		Double time = activeTrips.remove(event.getPersonId());
		
		if (time != null) {
			// calculate trip travel time
			time = event.getTime() - time;
			
			if (affectedAgents.contains(event.getPersonId())) {
				affectedAgentsTravelTimes += time;
				affectedAgentsCount++;
			}
			
			if (replanningAgents.contains(event.getPersonId())) {
				replanningAgentsTravelTimes += time;
				replanningAgentsCount++;
			}
			
			if (activityInCityZurich.contains(event.getPersonId())) {
				activityInCityZurichTravelTimes += time;
				activityInCityZurichCount++;
			}
			
			if (activityInCantonZurich.contains(event.getPersonId())) {
				activityInCantonZurichTravelTimes += time;
				activityInCantonZurichCount++;
			}
			
			if (tripThroughCityZurich.contains(event.getPersonId())) {
				tripThroughCityZurichTravelTimes += time;
				tripThroughCityZurichCount++;
			}
			
			if (tripThroughCantonZurich.contains(event.getPersonId())) {
				tripThroughCantonZurichTravelTimes += time;
				tripThroughCantonZurichCount++;
			}
		}
	}

	@Override
	public void handleEvent(AgentDepartureEvent event) {
		activeTrips.put(event.getPersonId(), event.getTime());
	}

	@Override
	public void handleEvent(AgentStuckEvent event) {
		activeTrips.remove(event.getPersonId());
	}
	
	@Override
	public void reset(int iteration) {
		activeTrips = new HashMap<Id, Double>();
		affectedAgentsTravelTimes = 0.0;
		replanningAgentsTravelTimes = 0.0;
		activityInCityZurichTravelTimes = 0.0;
		activityInCantonZurichTravelTimes = 0.0;
		tripThroughCityZurichTravelTimes = 0.0;
		tripThroughCantonZurichTravelTimes = 0.0;
		affectedAgentsCount = 0;
		replanningAgentsCount = 0;
		activityInCityZurichCount = 0;
		activityInCantonZurichCount = 0;
		tripThroughCityZurichCount = 0;
		tripThroughCantonZurichCount = 0;
	}

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		log.info("Number of Agents with Trips planned over affected Links: " + affectedAgents.size());
		log.info("Number of Agents with Trips through the replanning area: " + replanningAgents.size());
		log.info("Number of Agents with Trips through the City of Zurich: " + tripThroughCityZurich.size());
		log.info("Number of Agents with Trips through the Canton of Zurich: " + tripThroughCantonZurich.size());
		log.info("Number of Agents with Activity in the City of Zurich: " + activityInCityZurich.size());
		log.info("Number of Agents with Activity in the Canton of Zurich: " + activityInCantonZurich.size());

		log.info("Mean Travel Time per Agent with Trip planned over affected Links: " + Time.writeTime(affectedAgentsTravelTimes / affectedAgentsCount));
		log.info("Mean Travel Time per Agent with Trip through the replanning area: " + Time.writeTime(replanningAgentsTravelTimes / replanningAgentsCount));
		log.info("Mean Travel Time per Agent with Trip through the City of Zurich: " + Time.writeTime(tripThroughCityZurichTravelTimes / tripThroughCityZurichCount));
		log.info("Mean Travel Time per Agent with Trip through the Canton of Zurich: " + Time.writeTime(tripThroughCantonZurichTravelTimes / tripThroughCantonZurichCount));
		log.info("Mean Travel Time per Agent with Activity in the City of Zurich: " + Time.writeTime(activityInCityZurichTravelTimes / activityInCityZurichCount));
		log.info("Mean Travel Time per Agent with Activity in the Canton of Zurich: " + Time.writeTime(activityInCantonZurichTravelTimes / activityInCantonZurichCount));
	}
}