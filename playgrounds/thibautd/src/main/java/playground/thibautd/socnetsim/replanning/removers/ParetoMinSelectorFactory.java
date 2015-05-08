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

package playground.thibautd.socnetsim.replanning.removers;

import org.matsim.api.core.v01.Scenario;

import com.google.inject.Inject;

import playground.thibautd.socnetsim.replanning.selectors.GroupLevelPlanSelector;
import playground.thibautd.socnetsim.replanning.selectors.IncompatiblePlansIdentifierFactory;
import playground.thibautd.socnetsim.replanning.selectors.InverseScoreWeight;
import playground.thibautd.socnetsim.replanning.selectors.ParetoWeight;
import playground.thibautd.socnetsim.replanning.selectors.highestweightselection.HighestWeightSelector;

public class ParetoMinSelectorFactory extends AbstractDumbRemoverFactory {
	private final IncompatiblePlansIdentifierFactory incompatiblePlansIdentifierFactory;

	@Inject
	public ParetoMinSelectorFactory( Scenario sc, IncompatiblePlansIdentifierFactory incompatiblePlansIdentifierFactory ) {
		super( sc );
		this.incompatiblePlansIdentifierFactory = incompatiblePlansIdentifierFactory;
	}

	@Override
	public GroupLevelPlanSelector createSelector() {
		return new HighestWeightSelector(
				true ,
				incompatiblePlansIdentifierFactory,
				new ParetoWeight(
					new InverseScoreWeight() ) );
	}
}
