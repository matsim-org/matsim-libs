/* *********************************************************************** *
 * project: org.matsim.*
 * MutateAndOptimiseStrategy.java
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
package playground.thibautd.jointtrips.replanning.strategies;

import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.replanning.selectors.BestPlanSelector;
import org.matsim.core.replanning.selectors.ExpBetaPlanSelector;
import org.matsim.core.replanning.selectors.KeepSelected;
import org.matsim.core.replanning.selectors.PlanSelector;

import playground.thibautd.jointtrips.config.JointTripsMutatorConfigGroup;
import playground.thibautd.jointtrips.replanning.JointPlanStrategy;
import playground.thibautd.jointtrips.replanning.modules.jointtimemodechooser.JointTimeModeChooserModule;
import playground.thibautd.jointtrips.replanning.modules.jointtripsmutator.JointTripsMutatorModule;
import playground.thibautd.jointtrips.replanning.selectors.RandomPlanSelectorWithoutCasts;

/**
 * The strategy using the {@link playground.thibautd.jointtrips.replanning.modules.jointtripsmutator.JointTripsMutatorAlgorithm}.
 * What it does is:
 * <br>
 * <ul>
 * <li> select the plans to handle according to the selector specified in the config group
 * <li> execute the {@link playground.thibautd.jointtrips.replanning.modules.jointtripsmutator.JointTripsMutatorAlgorithm} on those plans
 * <li> execute the {@link playground.thibautd.jointtrips.replanning.modules.jointplanoptimizer.JointPlanOptimizer} on those plans. The
 * JointPlanOptimiser should be configured not to optimise toggle; nothing is done
 * to check nor enforce it.
 * </ul>
 *
 * @author thibautd
 */
public class MutateAndOptimiseStrategy extends JointPlanStrategy {
	public MutateAndOptimiseStrategy(
			final Controler controler) {
		super( extractSelector( controler.getConfig() ) );

		addStrategyModule( new JointTripsMutatorModule( controler ) );
		addStrategyModule( new JointTimeModeChooserModule( controler ) );
	}

	private static PlanSelector extractSelector(
			final Config config) {
		JointTripsMutatorConfigGroup group = (JointTripsMutatorConfigGroup)
			config.getModule( JointTripsMutatorConfigGroup.GROUP_NAME );

		switch (group.getSelector()) {
			case EXP_BETA:
				return new ExpBetaPlanSelector( config.planCalcScore() ); 
			case BEST_SCORE:
				return new BestPlanSelector();
			case RANDOM:
				return new RandomPlanSelectorWithoutCasts();
			case SELECTED:
				return new KeepSelected();
			default:
				throw new RuntimeException( "unexpected selector name "+group.getSelector() );
		}
	}
}

