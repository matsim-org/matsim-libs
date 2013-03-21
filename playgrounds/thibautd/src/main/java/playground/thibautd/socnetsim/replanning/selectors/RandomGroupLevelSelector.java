/* *********************************************************************** *
 * project: org.matsim.*
 * RandomGroupLevelSelector.java
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
package playground.thibautd.socnetsim.replanning.selectors;

import java.util.Random;

import org.matsim.api.core.v01.population.Plan;

import playground.thibautd.socnetsim.replanning.grouping.ReplanningGroup;
import playground.thibautd.socnetsim.replanning.selectors.highestweightselection.AbstractHighestWeightSelector;

/**
 * @author thibautd
 */
public class RandomGroupLevelSelector extends AbstractHighestWeightSelector {
	private final Random random;

	public RandomGroupLevelSelector(final Random random) {
		this.random = random;
	}

	@Override
	public double getWeight(
			final Plan indivPlan,
			final ReplanningGroup group) {
		return random.nextDouble();
	}
}

