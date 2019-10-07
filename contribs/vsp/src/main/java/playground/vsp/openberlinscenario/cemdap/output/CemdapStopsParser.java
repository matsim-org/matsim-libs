/* *********************************************************************** *
 * project: org.matsim.*                                                   *
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

package playground.vsp.openberlinscenario.cemdap.output;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.utils.objectattributes.ObjectAttributes;

/**
 * @author dziemke
 */
public class CemdapStopsParser {

	private final static Logger LOG = Logger.getLogger(CemdapStopsParser.class);

	private final Coord DEFAULT_COORD = new Coord(-1.0, -1.0);

	// Cemdap stop file columns
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

	private int activityDurationThreshold_s = Integer.MIN_VALUE;

	public static final String CEMDAP_STOP_DURATION_S_ATTRIBUTE_NAME = "cemdapStopDuration_s";
	
	public CemdapStopsParser() {
	}

	
	public final void parse(String cemdapStopsFile, int planNumber, Population population,
							ObjectAttributes personZoneAttributes, String outputDirectory) {
		int lineCount = 0;

		try {
			BufferedReader bufferedReader = IOUtils.getBufferedReader(cemdapStopsFile);
			String currentLine;

			CemdapOutputAnalyzer analyzer = new CemdapOutputAnalyzer();

			while ((currentLine = bufferedReader.readLine()) != null) {
				String[] entries = currentLine.split("\t", -1);
				lineCount++;
				
				if (lineCount % 1000000 == 0) {
					LOG.info("Line " + lineCount);// + ": " + population.getPersons().size() + " persons stored so far.");
					Gbl.printMemoryUsage();
				}

				Id<Person> personId = Id.create(Integer.parseInt(entries[P_ID]), Person.class);

				// Create a person if a person with that personId does not already exist
				Person person = population.getPersons().get(personId);
				if (person == null) {
					if (planNumber != 0) {
						throw new RuntimeException("Person "+personId.toString()+" must not be null here since even home-staying personsn should have been added by now.");
					}
					person = population.getFactory().createPerson(personId);
					population.addPerson(person);
					analyzer.increaseNumberOfAgents();
				}

				// Create a new plan if plan with current plan number does not already exist and add a home activity
				if (person.getPlans().size() <= planNumber ) {
					person.addPlan(population.getFactory().createPlan());
				}

				// Get plan with current number and write information from cemdap stops file to it
				Plan plan = person.getPlans().get(planNumber);
				int departureTime = Integer.parseInt(entries[START_TRAVEL_TO_STOP]) * 60 + TIME_OFFSET;
				
				// If plan is empty, create a home activity and add it to the plan
				if (plan.getPlanElements().isEmpty()) {
					String zoneId = entries[ORIG_ZONE_ID].trim();
					
					personZoneAttributes.putAttribute(personId.toString(), ZONE + "0", zoneId);
					
					String activityType = transformActType(Integer.parseInt(entries[ACT_TYPE_AT_PREV_STOP]));
					Activity firstActivity = population.getFactory().createActivityFromCoord(activityType, DEFAULT_COORD);
					firstActivity.setEndTime(departureTime);
					plan.addActivity(firstActivity);
					analyzer.registerTrip(firstActivity.getType(), departureTime);
				}
				
				plan.addLeg(population.getFactory().createLeg(TransportMode.car));
				
				String zoneId = entries[STOP_LOC_ZONE_ID];

				int actIndex = plan.getPlanElements().size()/2;
				personZoneAttributes.putAttribute(personId.toString(), ZONE + actIndex, zoneId);

				String activityType = transformActType(Integer.parseInt(entries[ACT_TYPE]));
				Activity activity = population.getFactory().createActivityFromCoord(activityType, DEFAULT_COORD);

				int stopDuration = Integer.parseInt(entries[STOP_DUR]) * 60;
				double endTime = departureTime + Integer.parseInt(entries[TRAVEL_TIME_TO_STOP]) * 60 + stopDuration;
				activity.getAttributes().putAttribute(CEMDAP_STOP_DURATION_S_ATTRIBUTE_NAME, stopDuration);
				if (endTime < 97200.) { // Set end time only if it is not the last activity of the day; 97200s = 24h+3h
					if (stopDuration <= activityDurationThreshold_s) {
						activity.setMaximumDuration(stopDuration);
					} else {
						activity.setEndTime(endTime);
					}
				}
				
				plan.addActivity(activity);

				analyzer.registerTrip(activityType, stopDuration);
			}

			analyzer.writeOutput(outputDirectory + "/statistics.txt");
		} catch (IOException e) {
			LOG.error(e);
		}
		LOG.info(lineCount + " lines parsed.");
		LOG.info(population.getPersons().size() + " persons stored.");
//		removed this method because of unnecessary use
//		cleanUp(population, planNumber);
		LOG.info(population.getPersons().size()+" persons remaining.");
	}
	

	// Information from Subodh, Nov 2014
	// see: shared-svn/projects/cemdapMatsimCadyts/cemdap_software/cemdap-11-2014/Activity_Mapping_Nov13.xlsx
	// Also found this conversion in code from UCSB
	private String transformActType(int activityTypeNumber) {
		switch (activityTypeNumber) {
													//	Activity 	Mapping Code
		case 0: return ActivityTypes.SHOPPING;		//	Shopping	0
		case 1: return ActivityTypes.OTHER;			//	Social Activity	1
		case 2: return ActivityTypes.OTHER;			//	Others	2
		case 3: return ActivityTypes.LEISURE;		//	Eating Out	3
		case 4: return ActivityTypes.OTHER;			//	Serving Passengers	4
		case 5: return ActivityTypes.LEISURE;		//	Entertainment	5
		case 6: return ActivityTypes.LEISURE;		//	Active Recreation	6
		case 7: return ActivityTypes.OTHER;			//	Visit friends/family	7
		case 8: return ActivityTypes.WORK;			//	Work Related	8
		case 9: return ActivityTypes.OTHER;			//	Maintainence	9
		case 10: return ActivityTypes.OTHER;		//	Drop off to school	10
		case 11: return ActivityTypes.OTHER;		//	Pick up from school	11
		case 12: return ActivityTypes.HOME;			//	Go home adult	12
		case 13: return ActivityTypes.WORK;			//	Go work adult	13
		case 14: return ActivityTypes.HOME;			//	Go home child	14
		case 15: return ActivityTypes.EDUCATION;	//	Go school child	15
		case 16: return ActivityTypes.LEISURE;		//	Independent discretionary activity	16
		case 17: return ActivityTypes.WORK;			//	Work	17
		case 18: return ActivityTypes.HOME;			//	Home	18
		case 19: return ActivityTypes.EDUCATION;	//	School	19
		case 20: return ActivityTypes.LEISURE;		//	Joint discretionary with child	20
		case 21: return ActivityTypes.LEISURE;		//	Joint discretionary with parents	21
		default:
			LOG.error(new IllegalArgumentException("actTypeNo="+activityTypeNumber+" not allowed."));
			return null;
		}
	}

		
	private void cleanUp(Population population, int planNumber) {
		Set<Id<Person>> idsOfPersonsToRemove = new HashSet<>();
		for (Person person : population.getPersons().values()) {
			Activity firstActivity = (Activity)person.getPlans().get(planNumber).getPlanElements().get(0);
			if (firstActivity.getEndTime() < 0.0) {
				idsOfPersonsToRemove.add(person.getId());
				LOG.info("Person with ID " + person.getId() + ": first departure before 00:00:00. Will be removed from the population");
			}
		}
		for (Id<Person> personId : idsOfPersonsToRemove) {
			population.getPersons().remove(personId);
		}
		LOG.info("In total " + idsOfPersonsToRemove.size() + " removed from the population.");
	}
	
	
	public void setActivityDurationThreshold_s (int activityDurationThreshold_s) {
		this.activityDurationThreshold_s = activityDurationThreshold_s;
	}
}