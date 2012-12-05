/* *********************************************************************** *
 * project: org.matsim.*
 * ControlerBuilder.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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
package playground.thibautd.socnetsim.run;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.scoring.functions.CharyparNagelScoringFunctionFactory;
import org.matsim.core.scoring.ScoringFunctionFactory;

import playground.thibautd.socnetsim.replanning.GroupIdentifier;
import playground.thibautd.socnetsim.replanning.GroupPlanStrategy;
import playground.thibautd.socnetsim.replanning.GroupReplanningListenner;
import playground.thibautd.socnetsim.replanning.GroupStrategyManager;
import playground.thibautd.socnetsim.replanning.GroupStrategyRegistry;
import playground.thibautd.socnetsim.replanning.ReplanningGroup;

/**
 * @author thibautd
 */
public class ControlerBuilder {
	private final Scenario scenario;
	private ScoringFunctionFactory scoringFunctionFactory;
	private GroupIdentifier groupIdentifier;
	private final GroupStrategyRegistry registry = new GroupStrategyRegistry();

	public ControlerBuilder( final Scenario scenario ) {
		this.scenario = scenario;

		this.scoringFunctionFactory =
			new CharyparNagelScoringFunctionFactory(
					scenario.getConfig().planCalcScore(),
					scenario.getNetwork());

		// default: individuals (ie no groups)
		this.groupIdentifier = new GroupIdentifier() {
			@Override
			public Collection<ReplanningGroup> identifyGroups(
					final Population population) {
				final List<ReplanningGroup> groups = new ArrayList<ReplanningGroup>();
	
				for (Person p : population.getPersons().values()) {
					ReplanningGroup g = new ReplanningGroup();
					g.addPerson( p );
					groups.add( g );
				}

				return groups;
			}
		};
	}

	public ControlerBuilder withScoring( final ScoringFunctionFactory scoringFunctionFactory ) {
		this.scoringFunctionFactory = scoringFunctionFactory;
		return this;
	}

	public ControlerBuilder withGroupIdentifier( final GroupIdentifier identifier ) {
		this.groupIdentifier = identifier;
		return this;
	}

	public ControlerBuilder withStrategy(
			final GroupPlanStrategy strategy,
			final double weight) {
		registry.addStrategy( strategy , weight );
		return this;
	}

	public ImmutableJointControler build() {
		return new ImmutableJointControler(
				scenario,
				new GroupReplanningListenner(
					scenario.getPopulation(),
					new GroupStrategyManager(
						groupIdentifier,
						registry,
						scenario.getConfig().strategy().getMaxAgentPlanMemorySize())),
				scoringFunctionFactory);
	}
}
