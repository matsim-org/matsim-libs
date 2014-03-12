/* *********************************************************************** *
 * project: org.matsim.*
 * NoScoringFunctionFactory
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
package playground.vsptelematics.ub6;

import org.matsim.api.core.v01.population.Person;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;


/**
 * @author dgrether
 *
 */
public class NoScoringFunctionFactory implements ScoringFunctionFactory {

	/**
	 * @see org.matsim.core.scoring.ScoringFunctionFactory#createNewScoringFunction(Person)
	 */
	@Override
	public ScoringFunction createNewScoringFunction(Person person) {
		return new NoScoringFunction(person.getSelectedPlan());
	}

}
