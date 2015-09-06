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

package playground.juliakern.distribution.withScoringFast;

import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.Config;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.functions.CharyparNagelScoringFunctionFactory;

public class ResponsibilityScoringFunctionFactory implements
		ScoringFunctionFactory {

	private CharyparNagelScoringFunctionFactory delegate;
	private EmissionControlerListener ecl;
	
	public ResponsibilityScoringFunctionFactory(Config config, Network network, EmissionControlerListener ecl) {
		this.delegate = new CharyparNagelScoringFunctionFactory(config.planCalcScore(), scConfig, network);
		this.ecl = ecl;
		// TODO Auto-generated constructor stub
	}

	@Override
	public ScoringFunction createNewScoringFunction(Person person) {
		// TODO Auto-generated method stub
		return new ResponsiblityScoringFunction(person.getSelectedPlan(), delegate.createNewScoringFunction(person), ecl);
	}

}
