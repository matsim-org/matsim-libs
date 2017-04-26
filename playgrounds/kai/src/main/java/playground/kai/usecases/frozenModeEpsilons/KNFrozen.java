/* *********************************************************************** *
 * project: org.matsim.*												   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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
package playground.kai.usecases.frozenModeEpsilons;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.math3.distribution.GumbelDistribution;
import org.apache.commons.math3.util.FastMath;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.SumScoringFunction;
import org.matsim.core.scoring.SumScoringFunction.LegScoring;
import org.matsim.core.scoring.functions.CharyparNagelActivityScoring;
import org.matsim.core.scoring.functions.CharyparNagelAgentStuckScoring;
import org.matsim.core.scoring.functions.CharyparNagelLegScoring;
import org.matsim.core.scoring.functions.CharyparNagelMoneyScoring;
import org.matsim.core.scoring.functions.ScoringParametersForPerson;
import org.matsim.core.scoring.functions.SubpopulationScoringParameters;
import org.matsim.core.scoring.functions.ScoringParameters;

/**
 * @author nagel
 *
 */
public class KNFrozen {
	private static final double EULER = - FastMath.PI / (2 * FastMath.E);

	/**
	 * @author nagel
	 *
	 */
	static class MyAdditionalLegScoring implements LegScoring {

		private double score = 0 ;

		private Map< String, Double> epsilons;
		
		MyAdditionalLegScoring( Map<String,Double> epsilons ) {
			this.epsilons = epsilons ;
		}

		@Override
		public void finish() {
			
		}

		@Override
		public double getScore() {
			return score ;
		}

		@Override
		public void handleLeg(Leg leg) {
			score += epsilons.get( leg.getMode() ) ;
		}

	}

	public static void main( String[] str ) {
		
		final Config config = ConfigUtils.createConfig() ;
		
		final Scenario scenario = ScenarioUtils.loadScenario(config) ;
		
		final Controler controler = new Controler(scenario) ;
		
		final Map<Id<Person>,Map<String, Double>> epsilonss = new HashMap<>() ;
		double beta = 1 ;
		GumbelDistribution gmb = new GumbelDistribution( EULER, beta ) ;
		long seed = 1 ;
		
		for ( Person person : scenario.getPopulation().getPersons().values() ) {
			// person.getId().hashCode() ;
			gmb.reseedRandomGenerator(seed++); // auch nicht toll.  Wie hat ahorni das gelöst?
			Map<String,Double> epsilons = new HashMap<>() ;
			epsilons.put( TransportMode.car, gmb.sample() ) ;
			epsilons.put( TransportMode.pt, gmb.sample() ) ;
			epsilonss.put( person.getId(), epsilons ) ;
		}
		// alternative: write the epsilons as person attributes to file.
		
		
		ScoringFunctionFactory scoringFunctionFactory = new ScoringFunctionFactory() {
			final ScoringParametersForPerson parametersForPerson = new SubpopulationScoringParameters( scenario );

			@Override
			public ScoringFunction createNewScoringFunction(Person person) {
				SumScoringFunction sum = new SumScoringFunction() ;
				
				ScoringParameters params = parametersForPerson.getScoringParameters( person );
				
				sum.addScoringFunction( new CharyparNagelActivityScoring(params));
				sum.addScoringFunction( new CharyparNagelMoneyScoring(params) );
				sum.addScoringFunction( new CharyparNagelAgentStuckScoring(params));
				sum.addScoringFunction( new CharyparNagelLegScoring(params, scenario.getNetwork() ) );
				// check if this is the default.
				
				final LegScoring scoringFunction = new MyAdditionalLegScoring( epsilonss.get( person ) );
				sum.addScoringFunction( scoringFunction);

				return sum ;
			}
		} ;
		controler.setScoringFunctionFactory(scoringFunctionFactory);
		
		
		controler.run();
	}
	
}
