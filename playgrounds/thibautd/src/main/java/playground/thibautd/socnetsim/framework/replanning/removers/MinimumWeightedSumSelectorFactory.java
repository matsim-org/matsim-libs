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

package playground.thibautd.socnetsim.framework.replanning.removers;

import org.matsim.api.core.v01.Scenario;

import playground.thibautd.socnetsim.replanning.GroupReplanningConfigGroup;
import playground.thibautd.socnetsim.framework.replanning.selectors.GroupLevelPlanSelector;
import playground.thibautd.socnetsim.framework.replanning.selectors.IncompatiblePlansIdentifierFactory;
import playground.thibautd.socnetsim.framework.replanning.selectors.InverseScoreWeight;
import playground.thibautd.socnetsim.framework.replanning.selectors.WeightedWeight;
import playground.thibautd.socnetsim.framework.replanning.selectors.highestweightselection.HighestWeightSelector;

import com.google.inject.Inject;

public class MinimumWeightedSumSelectorFactory extends AbstractDumbRemoverFactory {
	private final IncompatiblePlansIdentifierFactory incompatiblePlansIdentifierFactory;
	private final Scenario sc;

	@Inject
	public MinimumWeightedSumSelectorFactory( Scenario sc, IncompatiblePlansIdentifierFactory incompatiblePlansIdentifierFactory ) {
		super( sc );
		this.sc = sc;
		this.incompatiblePlansIdentifierFactory = incompatiblePlansIdentifierFactory;
	}

	@Override
	public GroupLevelPlanSelector createSelector() {
		final GroupReplanningConfigGroup weights = (GroupReplanningConfigGroup)
				sc.getConfig().getModule(
					GroupReplanningConfigGroup.GROUP_NAME );
		return new HighestWeightSelector(
				true ,
				incompatiblePlansIdentifierFactory,
				new WeightedWeight(
					new InverseScoreWeight(),
					weights.getWeightAttributeName(),
					sc.getPopulation().getPersonAttributes()  ));
	}
}
