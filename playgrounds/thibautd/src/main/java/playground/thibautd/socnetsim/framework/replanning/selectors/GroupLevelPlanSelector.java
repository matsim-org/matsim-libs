/* *********************************************************************** *
 * project: org.matsim.*
 * GroupLevelPlanSelector.java
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
package playground.thibautd.socnetsim.framework.replanning.selectors;

import playground.thibautd.socnetsim.framework.population.JointPlans;
import playground.thibautd.socnetsim.framework.replanning.grouping.GroupPlans;
import playground.thibautd.socnetsim.framework.replanning.grouping.ReplanningGroup;

/**
 * @author thibautd
 */
public interface GroupLevelPlanSelector {
	public GroupPlans selectPlans(
			JointPlans jointPlans,
			ReplanningGroup group);
}

