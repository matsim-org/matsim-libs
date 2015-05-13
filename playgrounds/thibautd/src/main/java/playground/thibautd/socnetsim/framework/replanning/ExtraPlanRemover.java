/* *********************************************************************** *
 * project: org.matsim.*
 * ExtraPlanRemover.java
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
package playground.thibautd.socnetsim.framework.replanning;

import playground.thibautd.socnetsim.framework.population.JointPlans;
import playground.thibautd.socnetsim.replanning.grouping.ReplanningGroup;

/**
 * @author thibautd
 */
public interface ExtraPlanRemover {
	/**
	 * @return true if at least one plan was removed for at least one agent,
	 * false otherwise
	 */
	public boolean removePlansInGroup(
			JointPlans jointPlans,
			ReplanningGroup group );
}

