/* *********************************************************************** *
 * project: org.matsim.*
 * PlanFilterEngine.java
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
package playground.johannes.socialnetworks.sim.locationChoice;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.sna.util.ProgressLogger;

/**
 * @author illenberger
 *
 */
public class PlanFilterEngine {

	private static final Logger logger = Logger.getLogger(PlanFilterEngine.class);
	
	public static void apply(Population population, PlanFilter filter) {
		int cnt = 0;
		int total = 0;
		ProgressLogger.init(population.getPersons().size(), 1, 5);
		
		for(Person person : population.getPersons().values()) {
			for(Plan plan : person.getPlans()) {
				if(filter.apply(plan))
					cnt++;
				
				total++;
			}
			
			ProgressLogger.step();
		}
		
		ProgressLogger.termiante();
		
		logger.info(String.format("Modified %1$s of %2$s plans.", cnt, total));
	}
}
