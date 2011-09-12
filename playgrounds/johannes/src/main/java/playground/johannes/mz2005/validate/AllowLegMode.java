/* *********************************************************************** *
 * project: org.matsim.*
 * RemoveModeTrips.java
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

import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;



/**
 * @author illenberger
 *
 */
public class AllowLegMode implements PlanValidator {

	private final String mode;
	
	public AllowLegMode(String mode) {
		this.mode = mode;
	}
	
	@Override
	public boolean validate(Plan plan) {
		for(int i = 1; i < plan.getPlanElements().size(); i += 2) {
			Leg leg = (Leg) plan.getPlanElements().get(i);
			if(!leg.getMode().equals(mode)) {
				return false;
			}
		}
		
		return true;
	}

}
