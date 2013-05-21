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

package playground.dziemke.cemdap2matsim.single;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Random;
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
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.utils.objectattributes.ObjectAttributes;

/**
 * @author dziemke
 * @see balmermi: UCSBStopsParser
 *
 */
public class CemdapStopsParser {

	private final static Logger log = Logger.getLogger(CemdapStopsParser.class);

	private final Random r = MatsimRandom.getRandom();
	private final Coord DEFAULT_COORD = new CoordImpl(-1.0,-1.0);

	private static final int HID = 0;
	private static final int PID = 1;
	private static final int ACT_TYPE = 4;
	private static final int START_TT = 5;
	// private static final int TT = 6;
	private static final int STOP_DUR = 7;
	private static final int STOP_ID = 8;
	private static final int ORIG_ID = 9;
	private static final int ACT_TYPE_PREV = 11;
		
	private static final int TIME_OFFSET = 3*3600;
	public static final String ZONE = "zone";


	public CemdapStopsParser() {
	}
	

	private final String transformActType(int actTypeNo) {
		switch (actTypeNo) {
		case 0: return "shop";
		case 1: return "other";
		case 2: return "other";
		case 3: return "leis";
		case 4: return "other";
		case 5: return "leis";
		case 6: return "leis";
		case 7: return "other";
		case 8: return "work";
		case 9: return "other";
		case 10: return "other";
		case 11: return "other";
		case 12: return "home";
		case 13: return "work";
		case 14: return "home";
		case 15: return "educ";
		case 16: return "leis";
		case 17: return "work";
		case 18: return "home";
		case 19: return "educ";
		case 20: return "leis";
		case 21: return "leis";
		default:
			Gbl.errorMsg(new IllegalArgumentException("actTypeNo="+actTypeNo+" not allowed."));
			return null;
		}
	}
	
	
	// not used so far
	private final void cleanUp(Population population) {
		Set<Id> pidsToRemove = new HashSet<Id>();
		for (Person person : population.getPersons().values()) {
			Activity firstActivity = (Activity)person.getSelectedPlan().getPlanElements().get(0);
			if (firstActivity.getEndTime() < 0.0) {
				pidsToRemove.add(person.getId());
				log.info("pid="+person.getId()+": first departure before 00:00:00. Will be removed from the population");
			}
		}
		for (Id pid : pidsToRemove) {
			population.getPersons().remove(pid);
		}
		log.info("in totoal "+pidsToRemove.size()+" removed from the population.");
	}
	
	
	// parse method
	public final void parse(String cemdapStopsFile, int planNumber, Scenario scenario, ObjectAttributes personObjectAttributes, double fraction) {
//	public final void parse(String cemdapStopsFile, int planNumber, Scenario scenario, Map<String,SimpleFeature> features, double fraction) {	
		Population population = scenario.getPopulation();
		int lineCount = 0;

		try {
			BufferedReader br = IOUtils.getBufferedReader(cemdapStopsFile);
			String currentLine = br.readLine();
			
			Id currPid = null;
			boolean storePerson = false;

			// data
			while ((currentLine = br.readLine()) != null) {
				String[] entries = currentLine.split("\t", -1);
				lineCount++;
				
				if (lineCount % 1000000 == 0) {
					log.info("line "+lineCount+": "+population.getPersons().size()+" persons stored so far.");
					Gbl.printMemoryUsage();
				}
				
				// household id / person id
				Integer hid = Integer.parseInt(entries[HID]);
				Integer id = Integer.parseInt(entries[PID]);
				Id pid = scenario.createId(hid+"_"+id);
				
				
				if (!pid.equals(currPid)) {
					currPid = pid;
					if (r.nextDouble() < fraction) {
						storePerson = true;
					} else {
						storePerson = false;
					}
				}
				if (!storePerson) {
					continue;
				}
				
				
				// creates a person with a plan if a person with id pid does not already exist
				Person person = population.getPersons().get(pid);
				if (person == null) {
					person = population.getFactory().createPerson(pid);
					population.addPerson(person);
					person.addPlan(population.getFactory().createPlan());
				}

				
				// Plan plan = person.getSelectedPlan();
				// get SELECTED plan??
				if (planNumber != 0) {
					person.addPlan(population.getFactory().createPlan());
				}
				Plan plan = person.getPlans().get(planNumber);
				
				int depTime = Integer.parseInt(entries[START_TT])*60 + TIME_OFFSET;
				// int arrTime = Integer.parseInt(entries[TT])*60 + TIME_OFFSET;
				

				if (plan.getPlanElements().isEmpty()) {
					String zoneId = entries[ORIG_ID].trim();
					// String zoneId = entries[STOP_ID].trim();
					
					// new
					// SimpleFeature zone = features.get(zoneId);
					// Coord coord = UCSBUtils.getRandomCoordinate(zone);
					//
					
					personObjectAttributes.putAttribute(pid.toString(),ZONE+"0",zoneId);
					
					String actType = transformActType(Integer.parseInt(entries[ACT_TYPE_PREV]));
					
					Activity firstActivity = population.getFactory().createActivityFromCoord(actType,DEFAULT_COORD);
					// Activity firstActivity = population.getFactory().createActivityFromCoord(actType,coord);
					
					// kai: only end time // firstActivity.setStartTime(0);
					firstActivity.setEndTime(depTime);
					
					// kai: only end time // firstActivity.setMaximumDuration(depTime);
					plan.addActivity(firstActivity);
				}
				
				// if needed apply here a method transformMode
				Leg leg = population.getFactory().createLeg(TransportMode.car);
				leg.setDepartureTime(depTime);
				plan.addLeg(leg);
				
				String zoneId = entries[STOP_ID];
				
				int actIndex = plan.getPlanElements().size()/2;
				personObjectAttributes.putAttribute(pid.toString(),ZONE+actIndex,zoneId);
				
				String actType = transformActType(Integer.parseInt(entries[ACT_TYPE]));
				Activity activity = population.getFactory().createActivityFromCoord(actType,DEFAULT_COORD);
				int actDur = Integer.parseInt(entries[STOP_DUR])*60;
				
				// kai: only end time // activity.setStartTime(depTime);
				activity.setEndTime(depTime+actDur);
				
				// kai: only end time // activity.setMaximumDuration(actDur);
				plan.addActivity(activity);

			}
		} catch (IOException e) {
			Gbl.errorMsg(e);
		}
		log.info(lineCount+" lines parsed.");
		log.info(population.getPersons().size()+" persons stored.");
		cleanUp(population);
		log.info(population.getPersons().size()+" persons remaining.");
	}
}
