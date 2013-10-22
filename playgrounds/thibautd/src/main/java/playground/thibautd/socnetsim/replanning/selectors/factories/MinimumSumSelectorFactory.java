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

package playground.thibautd.socnetsim.replanning.selectors.factories;

import playground.thibautd.socnetsim.controller.ControllerRegistry;
import playground.thibautd.socnetsim.replanning.selectors.GroupLevelPlanSelector;
import playground.thibautd.socnetsim.replanning.selectors.GroupLevelSelectorFactory;
import playground.thibautd.socnetsim.replanning.selectors.LowestScoreSumSelectorForRemoval;

public class MinimumSumSelectorFactory implements
		GroupLevelSelectorFactory {
	@Override
	public GroupLevelPlanSelector createSelector(
			final ControllerRegistry controllerRegistry) {
		return new LowestScoreSumSelectorForRemoval(
				controllerRegistry.getIncompatiblePlansIdentifierFactory());
	}
}
