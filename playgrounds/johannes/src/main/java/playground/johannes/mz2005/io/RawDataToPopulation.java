/* *********************************************************************** *
 * project: org.matsim.*
 * PlanGenerator.java
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
package playground.johannes.mz2005.io;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.misc.ConfigUtils;

import playground.johannes.mz2005.validate.ActLegSequence;
import playground.johannes.mz2005.validate.PlanValidator;

/**
 * @author illenberger
 *
 */
public class RawDataToPopulation {
	
	private static final Logger logger = Logger.getLogger(RawDataToPopulation.class);
	
	private Scenario scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
	
	private EscortData escortData;
	
	public EscortData getEscortData() {
		return escortData;
	}
	
	public Population create(String basedir, List<Integer> days) {
		RawDataParser parser = new RawDataParser();
		escortData = new EscortData();
		/*
		 * load raw data
		 */
		Map<String, PersonDataContainer> persons = null;
		try {
			persons = parser.readPersons(basedir + "Zielpersonen.dat");
			parser.readTrips(basedir + "Wegeinland.xy.dat", persons);
			parser.readLegs(basedir + "Etappen.dat", persons);			
		} catch (IOException e) {
			e.printStackTrace();
		}
		/*
		 * generate population
		 */
		Population pop = new PopulationImpl(null);
		TripComparator comparator = new TripComparator();
		for (PersonDataContainer pContainer : persons.values()) {
			if (pContainer.trips.size() > 0) {
				Collections.sort(pContainer.trips, comparator);
				
				if (days == null || days.contains(pContainer.referenceDay)) {
					Person p = createPerson(pContainer.trips);
					pop.addPerson(p);
				}
			}
		}
		/*
		 * validate plans
		 */
		int invalid = 0;
		Set<Person> remove = new HashSet<Person>();
		PlanValidator validator = new ActLegSequence();
		for(Person person : pop.getPersons().values()) {
			Plan plan = person.getSelectedPlan();
			if(!validator.validate(plan)) {
				remove.add(person);
			}
		}
		
		for(Person person : remove) {
			pop.getPersons().remove(person.getId());
			invalid++;
		}
		
		if(invalid > 0)
			logger.info(String.format("Removed %1$s invalid plans.", invalid));
		
		return pop;
	
	}

	private Person createPerson(List<TripDataContaienr> trips) {
		/*
		 * create a person and a plan
		 */
		Person person = new PersonImpl(scenario.createId(trips.get(0).personId));
		Plan plan = new PlanImpl(person);
		/*
		 * create the first home activity
		 */
		ActivityImpl act = new ActivityImpl(ActivityType.home.name(), new CoordImpl(0, 0));
		act.setStartTime(0);
		act.setCoord(null);
		plan.addActivity(act);
		/*
		 * add activities and legs
		 */
		for(TripDataContaienr trip : trips) {
			/*
			 * complete previous activity
			 */
			ActivityImpl previous = (ActivityImpl) plan.getPlanElements().get(plan.getPlanElements().size() - 1);
			previous.setEndTime(trip.startTime * 60);
			if(trip.startCoord != null && previous.getCoord() == null)
				previous.setCoord(new CoordImpl(trip.startCoord[0], trip.startCoord[1]));
			/*
			 * create leg
			 */
			LegImpl leg = new LegImpl(trip.aggrMode.name());
			plan.addLeg(leg);
			/*
			 * create route
			 */
			LinkNetworkRouteImpl route = new LinkNetworkRouteImpl(null, null);
			if(trip.distance != -99)
				route.setDistance(trip.distance * 1000);
			
			if(trip.duration != -99)
				route.setTravelTime(trip.duration * 60);
			
			route.setLinkIds(new IdImpl(0), new ArrayList<Id>(0), new IdImpl(0));
			leg.setRoute(route);
			
			if(trip.accompanists > 0)
				escortData.add(person, plan.getPlanElements().size() - 1, trip.accompanists);
			/*
			 * create next activity
			 */
			ActivityImpl next = new ActivityImpl(activityType(trip.type, trip.leisureType), new CoordImpl(0, 0));
			next.setStartTime(trip.endTime * 60);
			next.setCoord(null);
			if(trip.destCoord !=  null) {
				next.setCoord(new CoordImpl(trip.destCoord[0], trip.destCoord[1]));
			}
			if(trip.outwardTrip) {
				next.setType(activityType(trip.type, trip.leisureType));
			} else
				next.setType(ActivityType.home.name());
			
			plan.addActivity(next);
		}
		/*
		 * if the last activity is not a home activity, add the home trip
		 */
		ActivityImpl previous = (ActivityImpl) plan.getPlanElements().get(plan.getPlanElements().size() - 1);
		if(!previous.getType().equalsIgnoreCase(ActivityType.home.name())) {
			previous.setEndTime(86399);
			LegImpl leg = new LegImpl("undefined");
			plan.addLeg(leg);
			
			act = new ActivityImpl(ActivityType.home.name(), ((Activity)plan.getPlanElements().get(0)).getCoord());
			act.setStartTime(86399);
			act.setEndTime(86400);
		}
		person.addPlan(plan);
		return person;
	}
	
	private String activityType(int id, int ltype) {
		if (id == 2 || id == 6 || id == 7)
			return ActivityType.work.name();
		else if (id == 3)
			return ActivityType.edu.name();
		else if (id == 4 || id == 5)
			return ActivityType.shop.name();
		else if (id == 8) {
			if (ltype == 1 || ltype == 13 || ltype == 14)
				return ActivityType.visit.name();
			else if (ltype == 2)
				return ActivityType.gastro.name();
			else if (ltype == 3 || (ltype >= 6 && ltype <= 9) || ltype == 12)
				return ActivityType.culture.name();
			else if(ltype == 4 || ltype == 5)
				return ActivityType.sports.name();
			else if(ltype == 10)
				return ActivityType.work.name();
			else if(ltype == 11)
				return ActivityType.shop.name();
			else
				return ActivityType.unknown.name();
		}

		else if (id == 11)
			return ActivityType.home.name();
		else
			return ActivityType.unknown.name();
	}
	
	private static class TripComparator implements Comparator<TripDataContaienr> {
		
		@Override
		public int compare(TripDataContaienr o1, TripDataContaienr o2) {
			int result = o1.startTime - o2.endTime;
			if (result == 0) {
				if (o1 == o2)
					return 0;
				else
					return o1.hashCode() - o2.hashCode();
			} else
				return result;
		}
	}
}
