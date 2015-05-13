/* *********************************************************************** *
 * project: org.matsim.*
 * IncompatiblePlansIdentifierFactory.java
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

import playground.thibautd.socnetsim.framework.population.JointPlans;
import playground.thibautd.socnetsim.replanning.grouping.ReplanningGroup;

/**
 * Create instances of {@link IncompatiblePlansIdentifierFactory}
 * for specific replanning groups.
 * <br>
 * It is safe to assume that the instances returned by this factory
 * will always be called on plans from the group used at construction.
 *
 * @author thibautd
 */
public interface IncompatiblePlansIdentifierFactory {
	public IncompatiblePlansIdentifier createIdentifier(JointPlans jointPlans, ReplanningGroup group);
}

