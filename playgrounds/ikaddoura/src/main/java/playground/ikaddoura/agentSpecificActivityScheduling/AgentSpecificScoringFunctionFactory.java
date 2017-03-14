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

package playground.ikaddoura.agentSpecificActivityScheduling;

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
import org.matsim.core.scoring.functions.CharyparNagelScoringFunctionFactory;
import org.matsim.core.scoring.functions.ScoringParameters;
import org.matsim.core.scoring.functions.ScoringParametersForPerson;
import org.matsim.core.scoring.functions.SubpopulationScoringParameters;

/**
* The default {@link CharyparNagelScoringFunctionFactory} except that activities are scored for each agent individually.
* 
* @author ikaddoura
*/

public class AgentSpecificScoringFunctionFactory implements ScoringFunctionFactory {
	
	protected Network network;
	private final ScoringParametersForPerson params;
	private final CountActEventHandler actCount;
	private double tolerance;
		
	public AgentSpecificScoringFunctionFactory( final Scenario sc, final CountActEventHandler actCount, double tolerance) {
		this( new SubpopulationScoringParameters( sc ) , sc.getNetwork() , actCount , tolerance);
	}
	
	@Inject
	AgentSpecificScoringFunctionFactory(final ScoringParametersForPerson params, Network network, CountActEventHandler actCount, double tolerance) {
		this.params = params;
		this.network = network;
		this.actCount = actCount;
		this.tolerance = tolerance;
	}
	
	@Override
	public ScoringFunction createNewScoringFunction(Person person) {
				
		final ScoringParameters parameters = params.getScoringParameters( person );
				
		SumScoringFunction sumScoringFunction = new SumScoringFunction();
		sumScoringFunction.addScoringFunction(new AgentSpecificActivityScoring(parameters, person, actCount, tolerance));
		sumScoringFunction.addScoringFunction(new CharyparNagelLegScoring( parameters , this.network));
		sumScoringFunction.addScoringFunction(new CharyparNagelMoneyScoring( parameters ));
		sumScoringFunction.addScoringFunction(new CharyparNagelAgentStuckScoring( parameters ));
		return sumScoringFunction;		
	}

}

