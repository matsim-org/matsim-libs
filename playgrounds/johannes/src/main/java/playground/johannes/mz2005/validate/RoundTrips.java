/* *********************************************************************** *
 * project: org.matsim.*
 * RoundTrips.java
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
package playground.johannes.mz2005.validate;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Plan;

/**
 * @author illenberger
 *
 */
public class RoundTrips implements PlanValidator {

	@Override
	public boolean validate(Plan plan) {
		if(plan.getPlanElements().size() == 3) {
			Activity home = (Activity) plan.getPlanElements().get(2);
			home.setType("roundTrip");
		}
		
		return true;
	}

}
