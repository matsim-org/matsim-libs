/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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
package playground.jbischoff.csberlin.replanning;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.population.algorithms.PermissibleModesCalculator;

import com.google.inject.Inject;

import playground.jbischoff.ffcs.FFCSUtils;

/**
 * @author  jbischoff
 *
 */
/**
 *
 */
public class BerlinCSPermissibleModesCalculator implements PermissibleModesCalculator {

	@Inject Scenario scenario;
	/* (non-Javadoc)
	 * @see org.matsim.core.population.algorithms.PermissibleModesCalculator#getPermissibleModes(org.matsim.api.core.v01.population.Plan)
	 */
	@Override
	public Collection<String> getPermissibleModes(Plan plan) {
		List<String> modes = Arrays.asList(scenario.getConfig().subtourModeChoice().getModes());
		int age = (int) scenario.getPopulation().getPersonAttributes().getAttribute(plan.getPerson().toString(), "age");
		String carAv = (String) scenario.getPopulation().getPersonAttributes().getAttribute(plan.getPerson().toString(), "carAvail");
		boolean member = (boolean) scenario.getPopulation().getPersonAttributes().getAttribute(plan.getPerson().toString(), "member");
		
		if (age<18) {
			modes.remove(TransportMode.car);
			modes.remove(FFCSUtils.FREEFLOATINGMODE);
		}
		if (carAv.equals("never")){
			modes.remove(TransportMode.car);
		}
		if (!member){
			modes.remove(FFCSUtils.FREEFLOATINGMODE);
		}
		
		return modes;
	}

}
