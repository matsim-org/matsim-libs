/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package playground.julia.distribution.scoring;

import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;

import playground.julia.distribution.ResponsibilityCostModule;
import playground.julia.distribution.ResponsibilityScoringFunction;

public class ResponisibilityScoringFunctionFactory implements
		ScoringFunctionFactory {

	private ResponsibilityCostModule rcm;

	public ResponisibilityScoringFunctionFactory(ResponsibilityCostModule rcm) {
		// TODO Auto-generated constructor stub
		this.rcm=rcm;
	}

	@Override
	public ScoringFunction createNewScoringFunction(Plan plan) {
		// TODO Auto-generated method stub
		//TODO hier den alten wert + wert fuer verursachte exposure....
		// mal bei daniel gucken
		//ScoringFunction sf = new ResponsibilityScoringFunction(rcm);
		return null;
	}

}
