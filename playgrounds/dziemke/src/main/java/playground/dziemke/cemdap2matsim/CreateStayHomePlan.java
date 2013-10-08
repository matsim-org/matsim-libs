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

package playground.dziemke.cemdap2matsim;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.utils.objectattributes.ObjectAttributes;

/**
 * @author dziemke
 * 
 */
public class CreateStayHomePlan {

	private final static Logger log = Logger.getLogger(CreateStayHomePlan.class);

//	private final Random r = MatsimRandom.getRandom();
	private final Coord DEFAULT_COORD = new CoordImpl(-1.0,-1.0);

	private static final int HID = 0;
	private static final int PID = 1;
	private static final int ORIG_ID = 9;
	public static final String ZONE = "zone";


	public CreateStayHomePlan() {
	}
	

	
	// called at the very end
	private final void cleanUp(Population population, int planNumber) {
		Set<Id> pidsToRemove = new HashSet<Id>();
		for (Person person : population.getPersons().values()) {
			// Activity firstActivity = (Activity)person.getSelectedPlan().getPlanElements().get(0);
			if (person.getPlans().size() > planNumber) {
				Activity firstActivity = (Activity)person.getPlans().get(planNumber).getPlanElements().get(0);
				if (firstActivity.getEndTime() < 0.0) {
					pidsToRemove.add(person.getId());
					log.info("pid="+person.getId()+": first departure before 00:00:00. Will be removed from the population");
				}
			} else {
				log.warn("Person with ID=" + person.getId() + " has " + person.getPlans().size() + " plans.");
				pidsToRemove.add(person.getId());
			}
		}
		for (Id pid : pidsToRemove) {
			population.getPersons().remove(pid);
		}
		log.info("in total "+pidsToRemove.size()+" removed from the population.");
	}
	
	
	// create method
	public final void create(String cemdapStopsFile, int planNumber, Scenario scenario, ObjectAttributes personObjectAttributes, double fraction) {
		Population population = scenario.getPopulation();
		int lineCount = 0;

		try {
			BufferedReader br = IOUtils.getBufferedReader(cemdapStopsFile);
			String currentLine = br.readLine();
			

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
				
				
				// creates a person with a plan if a person with that pid does not already exist
				Person person = population.getPersons().get(pid);
				if (person == null) {
					person = population.getFactory().createPerson(pid);
					population.addPerson(person);
					person.addPlan(population.getFactory().createPlan());
				}

				
				// new
				if (planNumber != 0 && person.getPlans().size() <= planNumber ) {
					person.addPlan(population.getFactory().createPlan());
				}
				//
				
				
				Plan plan = person.getPlans().get(planNumber);
				
				
				if (plan.getPlanElements().isEmpty()) {
					String zoneId = entries[ORIG_ID].trim();
	
					personObjectAttributes.putAttribute(pid.toString(),ZONE+"0",zoneId);
					
					String actType = "home";
					
					Activity firstActivity = population.getFactory().createActivityFromCoord(actType,DEFAULT_COORD);
					plan.addActivity(firstActivity);
				}
				

			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		log.info(lineCount+" lines parsed.");
		log.info(population.getPersons().size()+" persons stored.");
		// cleanUp(population, planNumber);
		log.info(population.getPersons().size()+" persons remaining.");
	}
}
