/* *********************************************************************** *
 * project: org.matsim.*
 * EmptyIncompatiblePlansIdentifierFactory.java
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
package playground.thibautd.socnetsim.replanning.selectors;

import java.util.Collections;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Plan;

import playground.thibautd.socnetsim.framework.population.JointPlans;
import playground.thibautd.socnetsim.replanning.grouping.ReplanningGroup;

/**
 * The default factory: no incomaptibility relationships
 * @author thibautd
 */
public class EmptyIncompatiblePlansIdentifierFactory implements IncompatiblePlansIdentifierFactory {

	@Override
	public IncompatiblePlansIdentifier createIdentifier(
			final JointPlans jointPlans,
			final ReplanningGroup group) {
		return new IncompatiblePlansIdentifier() {
			@Override
			public Set<Id> identifyIncompatibilityGroups(final Plan plan) {
				return Collections.<Id>emptySet();
			}
		};
	}
}

