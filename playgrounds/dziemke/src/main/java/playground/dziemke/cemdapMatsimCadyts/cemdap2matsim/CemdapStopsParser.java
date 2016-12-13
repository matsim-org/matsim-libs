/* *********************************************************************** *
 * project: org.matsim.*
 * UCSBStopsParser.java
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

package playground.dziemke.cemdapMatsimCadyts.cemdap2matsim;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.utils.objectattributes.ObjectAttributes;

/**
 * @author dziemke
 * @see balmermi: UCSBStopsParser
 */
public class CemdapStopsParser {

	private final static Logger LOG = Logger.getLogger(CemdapStopsParser.class);

	//private final Random r = MatsimRandom.getRandom();
	private final Coord DEFAULT_COORD;

	{
		final double x = -1.0;
		final double y = -1.0;
		DEFAULT_COORD = new Coord(x, y);
	}

	// cemdap stop file columns
//	private static final int HH_ID = 0;
	private static final int P_ID = 1;
//	private static final int TOUR_ID = 2;
//	private static final int STOP_ID = 3;
	private static final int ACT_TYPE = 4;
	private static final int START_TRAVEL_TO_STOP = 5;
	private static final int TRAVEL_TIME_TO_STOP = 6;
	private static final int STOP_DUR = 7;
	private static final int STOP_LOC_ZONE_ID = 8;
	private static final int ORIG_ZONE_ID = 9;
//	private static final int TRIP_DIST = 10;
	private static final int ACT_TYPE_AT_PREV_STOP = 11;
		
	private static final int TIME_OFFSET = 3*3600;
	public static final String ZONE = "zone";


	public CemdapStopsParser() {
	}

	
	public final void parse(String cemdapStopsFile, int planNumber, //Map<String, String> tourAttributesMap,
			Scenario scenario, ObjectAttributes personObjectAttributes, boolean stayHomePlan) {
		Population population = scenario.getPopulation();
		int lineCount = 0;

		try {
			BufferedReader bufferedReader = IOUtils.getBufferedReader(cemdapStopsFile);
			String currentLine = null;

			while ((currentLine = bufferedReader.readLine()) != null) {
				String[] entries = currentLine.split("\t", -1);
				lineCount++;
				
				if (lineCount % 1000000 == 0) {
					LOG.info("line "+lineCount+": "+population.getPersons().size()+" persons stored so far.");
					Gbl.printMemoryUsage();
				}

				Id<Person> personId = Id.create(Integer.parseInt(entries[P_ID]), Person.class);

				// Create a person if a person with that personId does not already exist
				Person person = population.getPersons().get(personId);
				if (person == null) {
					person = population.getFactory().createPerson(personId);
					population.addPerson(person);
				}

				// Create a new plan if plan with current plan number does not already exist
				if (person.getPlans().size() <= planNumber ) {
					person.addPlan(population.getFactory().createPlan());
				}

				// get plan with current number and write information from cemdap stops file to it
				Plan plan = person.getPlans().get(planNumber);				
				int departureTime = Integer.parseInt(entries[START_TRAVEL_TO_STOP])*60 + TIME_OFFSET;
				
				// if plan is empty, create a home activity and add it to the plan
				if (plan.getPlanElements().isEmpty()) {
					String zoneId = entries[ORIG_ZONE_ID].trim();
					
					personObjectAttributes.putAttribute(personId.toString(),ZONE+"0",zoneId);
					
					String activityType = transformActType(Integer.parseInt(entries[ACT_TYPE_AT_PREV_STOP]));
					
					Activity firstActivity = population.getFactory().createActivityFromCoord(activityType,DEFAULT_COORD);
					firstActivity.setEndTime(departureTime);
					plan.addActivity(firstActivity);
				}
				
				
				if (stayHomePlan == false) {
					Leg leg = population.getFactory().createLeg(TransportMode.car);
					
					plan.addLeg(leg);
					
					// add an activity to the plan
					String zoneId = entries[STOP_LOC_ZONE_ID];
				
					int actIndex = plan.getPlanElements().size()/2;
					personObjectAttributes.putAttribute(personId.toString(),ZONE+actIndex,zoneId);
					
					String activityType = transformActType(Integer.parseInt(entries[ACT_TYPE]));
					Activity activity = population.getFactory().createActivityFromCoord(activityType,DEFAULT_COORD);
					
					activity.setEndTime(departureTime + Integer.parseInt(entries[TRAVEL_TIME_TO_STOP])*60 + Integer.parseInt(entries[STOP_DUR])*60);
					plan.addActivity(activity);
				}

			}
		} catch (IOException e) {
			LOG.error(e);
			//Gbl.errorMsg(e);
		}
		LOG.info(lineCount+" lines parsed.");
		LOG.info(population.getPersons().size()+" persons stored.");
		// cleanUp(population);
		cleanUp(population, planNumber);
		LOG.info(population.getPersons().size()+" persons remaining.");
	}
	

	// information from Subodh, Nov 2014
	// see: shared-svn/projects/cemdapMatsimCadyts/cemdap_software/cemdap-11-2014/Activity_Mapping_Nov13.xlsx
	// coding from UCSB can remain according to this information
	private final String transformActType(int activityTypeNumber) {
		switch (activityTypeNumber) {
									//	Activity 	Mapping Code
		case 0: return "shop";		//	Shopping	0
		case 1: return "other";		//	Social Activity	1
		case 2: return "other";		//	Others	2
		case 3: return "leis";		//	Eating Out	3
		case 4: return "other";		//	Serving Passengers	4
		case 5: return "leis";		//	Entertainment	5
		case 6: return "leis";		//	Active Recreation	6
		case 7: return "other";		//	Visit friends/family	7
		case 8: return "work";		//	Work Related	8
		case 9: return "other";		//	Maintainence	9
		case 10: return "other";	//	Drop off to school	10
		case 11: return "other";	//	Pick up from school	11
		case 12: return "home";		//	Go home adult	12
		case 13: return "work";		//	Go work adult	13
		case 14: return "home";		//	Go home child	14
		case 15: return "educ";		//	Go school child	15
		case 16: return "leis";		//	Independent discretionary activity	16
		case 17: return "work";		//	Work	17
		case 18: return "home";		//	Home	18
		case 19: return "educ";		//	School	19
		case 20: return "leis";		//	Joint discretionary with child	20
		case 21: return "leis";		//	Joint discretionary with parents	21
		default:
			LOG.error(new IllegalArgumentException("actTypeNo="+activityTypeNumber+" not allowed."));
			return null;
		}
	}

		
	// called at the very end
	// private final void cleanUp(Population population) {
	private final void cleanUp(Population population, int planNumber) {
		Set<Id<Person>> pidsToRemove = new HashSet<>();
		for (Person person : population.getPersons().values()) {
			// Activity firstActivity = (Activity)person.getSelectedPlan().getPlanElements().get(0);
			if (person.getPlans().size() > planNumber) {
				Activity firstActivity = (Activity)person.getPlans().get(planNumber).getPlanElements().get(0);
				if (firstActivity.getEndTime() < 0.0) {
					pidsToRemove.add(person.getId());
					LOG.info("pid="+person.getId()+": first departure before 00:00:00. Will be removed from the population");
				}
			} else {
				LOG.warn("Person with ID=" + person.getId() + " has " + person.getPlans().size() + " plans.");
				pidsToRemove.add(person.getId());
			}
		}
		for (Id<Person> pid : pidsToRemove) {
			population.getPersons().remove(pid);
		}
		LOG.info("in total "+pidsToRemove.size()+" removed from the population.");
	}
}