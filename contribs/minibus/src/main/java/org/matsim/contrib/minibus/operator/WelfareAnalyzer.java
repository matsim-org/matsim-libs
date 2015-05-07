/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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
package org.matsim.contrib.minibus.operator;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.pt.transitSchedule.api.TransitLine;

/**
 * @author ikaddoura
 *
 */
public class WelfareAnalyzer {
	
	Map<Id<TransitLine>, Double> lineId2welfareCorreciton = new HashMap<>();
	
	public void computeWelfare(Scenario scenario) {
		for (Person person : scenario.getPopulation().getPersons().values()){
			System.out.println("Person " + person.getId() + " - Score: " + person.getSelectedPlan().getScore());
		}
		
		// ...
	}
	
	public double getLineId2welfareCorrection(Id<TransitLine> id) {
		return this.lineId2welfareCorreciton.get(id);
	}

}
