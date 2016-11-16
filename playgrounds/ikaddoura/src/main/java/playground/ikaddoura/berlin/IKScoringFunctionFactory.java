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

package playground.ikaddoura.berlin;

import javax.inject.Inject;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.SumScoringFunction;
import org.matsim.core.scoring.functions.CharyparNagelAgentStuckScoring;
import org.matsim.core.scoring.functions.CharyparNagelLegScoring;
import org.matsim.core.scoring.functions.CharyparNagelMoneyScoring;
import org.matsim.core.scoring.functions.CharyparNagelScoringParameters;
import org.matsim.core.scoring.functions.CharyparNagelScoringParametersForPerson;
import org.matsim.core.scoring.functions.SubpopulationCharyparNagelScoringParameters;

/**
* @author ikaddoura
*/

public class IKScoringFunctionFactory implements ScoringFunctionFactory {
	
	protected Network network;
	private final CharyparNagelScoringParametersForPerson params;
	private final CountActEventHandler actCount;
		
	@Inject
	public IKScoringFunctionFactory( final Scenario sc, final CountActEventHandler actCount ) {
		this( new SubpopulationCharyparNagelScoringParameters( sc ) , sc.getNetwork() , actCount );
	}
	
	IKScoringFunctionFactory(final CharyparNagelScoringParametersForPerson params, Network network, CountActEventHandler actCount) {
		this.params = params;
		this.network = network;
		this.actCount = actCount;
	}
	
	@Override
	public ScoringFunction createNewScoringFunction(Person person) {
				
		final CharyparNagelScoringParameters parameters = params.getScoringParameters( person );
				
		SumScoringFunction sumScoringFunction = new SumScoringFunction();
		sumScoringFunction.addScoringFunction(new IKActivityScoring(parameters, person, actCount));
		sumScoringFunction.addScoringFunction(new CharyparNagelLegScoring( parameters , this.network));
		sumScoringFunction.addScoringFunction(new CharyparNagelMoneyScoring( parameters ));
		sumScoringFunction.addScoringFunction(new CharyparNagelAgentStuckScoring( parameters ));
		return sumScoringFunction;		
	}

}

