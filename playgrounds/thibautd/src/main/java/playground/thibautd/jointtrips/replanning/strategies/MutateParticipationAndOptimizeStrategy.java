/* *********************************************************************** *
 * project: org.matsim.*
 * MutateParticipationAndOptimizeStrategy.java
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

import org.matsim.core.controler.Controler;

import playground.thibautd.jointtrips.replanning.JointPlanStrategy;
import playground.thibautd.jointtrips.replanning.modules.jointtimemodechooser.JointTimeModeChooserModule;
import playground.thibautd.jointtrips.replanning.modules.jointtripmutator.JointTripMutatorModule;
import playground.thibautd.jointtrips.replanning.modules.reroute.JointReRouteModule;
import playground.thibautd.jointtrips.replanning.selectors.RandomPlanSelectorWithoutCasts;

/**
 * @author thibautd
 */
public class MutateParticipationAndOptimizeStrategy extends JointPlanStrategy {
	public MutateParticipationAndOptimizeStrategy(final Controler controler) {
		super( new RandomPlanSelectorWithoutCasts() );
		addStrategyModule( new JointTripMutatorModule( controler ) );
		addStrategyModule( new JointReRouteModule( controler ) );
		addStrategyModule( new JointTimeModeChooserModule( controler ) );
	}
}

