/* *********************************************************************** *
 * project: org.matsim.*
 * CharyparNagelOpenTimesScoringFunctionFactory.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package playground.artemc.scoring;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;
import playground.artemc.scoring.functions.*;

import java.util.HashMap;


/**
 * A factory to create scoring functions as described by D. Charypar and K. Nagel.
 * 
 * <blockquote>
 *  <p>Charypar, D. und K. Nagel (2005) <br>
 *  Generating complete all-day activity plans with genetic algorithms,<br>
 *  Transportation, 32 (4) 369-397.</p>
 * </blockquote>
 * 
 * @author rashid_waraich
 */
public class CharyparNagelScoringFunctionForAnalysisFactory implements ScoringFunctionFactory, PersonalScoringFunctionFactory {


	private static final Logger log = Logger.getLogger(CharyparNagelScoringFunctionForAnalysisFactory.class);

	protected Network network;
	private final PlanCalcScoreConfigGroup config;
	private HashMap<Id, ScoringFunction> personScoringFunctions;

	public CharyparNagelScoringFunctionForAnalysisFactory(final PlanCalcScoreConfigGroup config, Network network) {
		this.config = config;
		this.network = network;
		this.personScoringFunctions = new HashMap<Id, ScoringFunction>();
	}

	@Override
	public ScoringFunction createNewScoringFunction(Person person){
		
		PersonalScoringParameters params = new PersonalScoringParameters(this.config);

		DisaggregatedSumScoringFunction sumScoringFunction = new DisaggregatedSumScoringFunction();
		sumScoringFunction.setParams(params);
		
		sumScoringFunction.addScoringFunction(new CharyparNagelActivityScoring(params));
		for(String mode:config.getModes().keySet()){
			sumScoringFunction.addLegScoringFunction(mode, new CharyparNagelLegScoring(params, this.network));
		}
		sumScoringFunction.addLegScoringFunction("transit_walk", new CharyparNagelLegScoring(params, this.network));
		
		sumScoringFunction.addScoringFunction(new CharyparNagelMoneyScoring(params));
		sumScoringFunction.addScoringFunction(new CharyparNagelAgentStuckScoring(params));

		//		ScoringFunctionAccumulator sumScoringFunction = new ScoringFunctionAccumulator();
		//		sumScoringFunction.addScoringFunction(new CharyparNagelActivityScoring(this.params));
		//		sumScoringFunction.addScoringFunction(new CharyparNagelLegScoring(this.params, this.network));
		//		sumScoringFunction.addScoringFunction(new CharyparNagelMoneyScoring(this.params));
		//		sumScoringFunction.addScoringFunction(new CharyparNagelAgentStuckScoring(this.params));

		personScoringFunctions.put(person.getId(), sumScoringFunction);
		return sumScoringFunction;
	}

	@Override
	public HashMap<Id, ScoringFunction> getPersonScoringFunctions() {
		return personScoringFunctions;
	}
}
