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
package playground.johannes.socialnetworks.survey.mz2005;

import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.utils.geometry.CoordImpl;

/**
 * @author illenberger
 *
 */
public class PopulationGenerator {

	private static final Logger logger = Logger.getLogger(PopulationGenerator.class);
	
	private Scenario scenario = new ScenarioImpl();
	
	public Person createPerson(List<TripData> trips) {
		/*
		 * create a person and a plan
		 */
		Person person = new PersonImpl(scenario.createId(trips.get(0).personId));
		Plan plan = new PlanImpl(person);
		/*
		 * create the first home activity
		 */
		ActivityImpl act = new ActivityImpl(ActivityType.home.name(), new CoordImpl(0, 0));
		act.setCoord(null);
		plan.addActivity(act);
		/*
		 * add activities and legs
		 */
		for(TripData trip : trips) {
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
			 * create next activity
			 */
			ActivityImpl next = new ActivityImpl(activityType(trip.type), new CoordImpl(0, 0));
			next.setStartTime(trip.endTime * 60);
			next.setCoord(null);
			if(trip.destCoord !=  null) {
				next.setCoord(new CoordImpl(trip.destCoord[0], trip.destCoord[1]));
			}
			if(trip.outwardTrip) {
				next.setType(activityType(trip.type));
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
	
	private String activityType(int id) {
		if(id == 2 || id == 6 || id == 7)
			return "work";
		else if(id == 3)
			return "edu";
		else if(id == 4 || id == 5)
			return "shop";
		else if(id == 8)
			return "leisure";
		else if(id == 11)
			return "home";
		else
			return "unknown";
	}
	
	public static void main(String args[]) throws IOException {
		String basedir = "/Users/jillenberger/Work/work/socialnets/data/schweiz/mz2005/rawdata/";
		DataParser parser = new DataParser();
		
		Map<String, PersonData> persons = parser.readPersons(basedir + "Zielpersonen.dat");
		parser.readTrips(basedir + "Wegeinland.dat", persons);
		parser.readLegs(basedir + "Etappen.dat", persons);
		
		PopulationGenerator generator = new PopulationGenerator();
		Population pop = new PopulationImpl(null);
		for(PersonData pContainer : persons.values()) {
			if(pContainer.trips.size() > 0) {
			Collections.sort(pContainer.trips, new Comparator<TripData>() {

				@Override
				public int compare(TripData o1, TripData o2) {
					int result = o1.startTime - o2.endTime;
					if(result == 0) {
						if(o1 == o2)
							return 0;
						else
							return o1.hashCode() - o2.hashCode();
					} else
						return result;
				}
			});
			Person p = generator.createPerson(pContainer.trips);
			pop.addPerson(p);
			}
		}
		
		logger.info(String.format("Created %1$s persons with at least one leg.", pop.getPersons().size()));
		PopulationWriter writer = new PopulationWriter(pop, null);
		writer.write(basedir + "/plans.xml");
	}
}
