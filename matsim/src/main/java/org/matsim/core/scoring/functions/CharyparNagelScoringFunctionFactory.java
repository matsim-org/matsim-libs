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

package org.matsim.core.scoring.functions;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.ScenarioConfigGroup;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.SumScoringFunction;
import org.matsim.utils.objectattributes.ObjectAttributes;

import java.util.HashMap;
import java.util.Map;

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
public class CharyparNagelScoringFunctionFactory implements ScoringFunctionFactory {
	// yy should be final.

	protected Network network;

	private final ScoringParametersForPerson params;

	public interface ScoringParametersForPerson {
		CharyparNagelScoringParameters getScoringParameters( Person person );
	}

	public static class UniformScoringParameters implements ScoringParametersForPerson {
		private final PlanCalcScoreConfigGroup config;
		private final ScenarioConfigGroup scConfig;
		private CharyparNagelScoringParameters params = null;

		public UniformScoringParameters(PlanCalcScoreConfigGroup config, ScenarioConfigGroup scConfig) {
			this.config = config;
			this.scConfig = scConfig;
		}

		@Override
		public CharyparNagelScoringParameters getScoringParameters(Person person) {
            if (this.params == null) {
                /* lazy initialization of params. not strictly thread safe, as different threads could
                 * end up with different params-object, although all objects will have the same
                 * values in them due to using the same config. Still much better from a memory performance
                 * point of view than giving each ScoringFunction its own copy of the params.
                 */
                this.params = CharyparNagelScoringParameters.getBuilder(
						this.config,
						this.config.getScoringParameters( null ),
						scConfig).create();
            }

			return this.params;
		}
	}

	public static class SubpopulationScoringParameters implements ScoringParametersForPerson {
		private final PlanCalcScoreConfigGroup config;
		private final ScenarioConfigGroup scConfig;
		private final ObjectAttributes personAttributes;
		private final String subpopulationAttributeName;
		private final Map<String, CharyparNagelScoringParameters> params = new HashMap<>();

		public SubpopulationScoringParameters(Scenario scenario) {
			this.config = scenario.getConfig().planCalcScore();
			this.scConfig = scenario.getConfig().scenario();
			this.personAttributes = scenario.getPopulation().getPersonAttributes();
			this.subpopulationAttributeName = scenario.getConfig().plans().getSubpopulationAttributeName();
		}

		@Override
		public CharyparNagelScoringParameters getScoringParameters(Person person) {
			final String subpopulation = (String) personAttributes.getAttribute(
					person.getId().toString(),
					subpopulationAttributeName );

            if ( !this.params.containsKey( subpopulation ) ) {
                /* lazy initialization of params. not strictly thread safe, as different threads could
                 * end up with different params-object, although all objects will have the same
                 * values in them due to using the same config. Still much better from a memory performance
                 * point of view than giving each ScoringFunction its own copy of the params.
                 */
                this.params.put(
						subpopulation,
						CharyparNagelScoringParameters.getBuilder(
								this.config,
								this.config.getScoringParameters( subpopulation ),
								scConfig).create() );
            }

			return this.params.get( subpopulation );
		}
	}

	public CharyparNagelScoringFunctionFactory( final Scenario sc ) {
		this( new SubpopulationScoringParameters( sc ) , sc.getNetwork() );
	}

	//public CharyparNagelScoringFunctionFactory(final PlanCalcScoreConfigGroup config, ScenarioConfigGroup scConfig, Network network) {
	//	this(
	//		new UniformScoringParameters( config, scConfig),
    //        network );
	//}

    public CharyparNagelScoringFunctionFactory(final ScoringParametersForPerson params, Network network) {
		this.params = params;
		this.network = network;
	}

	/**
     *
     * In every iteration, the framework creates a new ScoringFunction for each Person.
     * A ScoringFunction is much like an EventHandler: It reacts to scoring-relevant events
     * by accumulating them. After the iteration, it is asked for a score value.
     *
     * Since the factory method gets the Person, it can create a ScoringFunction
     * which depends on Person attributes. This implementation does not.
     *
	 * <li>The fact that you have a person-specific scoring function does not mean that the "creative" modules
	 * (such as route choice) are person-specific.  This is not a bug but a deliberate design concept in order 
	 * to reduce the consistency burden.  Instead, the creative modules should generate a diversity of possible
	 * solutions.  In order to do a better job, they may (or may not) use person-specific info.  kai, apr'11
	 * </ul>
	 * 
	 * @param person
	 * @return new ScoringFunction
	 */
	@Override
	public ScoringFunction createNewScoringFunction(Person person) {

		final CharyparNagelScoringParameters parameters = params.getScoringParameters( person );

		SumScoringFunction sumScoringFunction = new SumScoringFunction();
		sumScoringFunction.addScoringFunction(new CharyparNagelActivityScoring( parameters ));
		sumScoringFunction.addScoringFunction(new CharyparNagelLegScoring( parameters , this.network));
		sumScoringFunction.addScoringFunction(new CharyparNagelMoneyScoring( parameters ));
		sumScoringFunction.addScoringFunction(new CharyparNagelAgentStuckScoring( parameters ));
		return sumScoringFunction;
	}
}
