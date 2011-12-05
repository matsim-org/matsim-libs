/* *********************************************************************** *
 * project: org.matsim.*
 * NoFreightPlanAcceptor.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package playground.thibautd.agentsmating.logitbasedmating.spbasedmodel;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;

import playground.thibautd.agentsmating.logitbasedmating.framework.PlanAcceptor;

/**
 * Excludes plans with at least one activity with type "freight"
 * @author thibautd
 */
public class NoFreightPlanAcceptor implements PlanAcceptor {
	/**
	 * the string used to identify freight activities
	 */
	public static final String FREIGHT = "freight";

	@Override
	public boolean accept(final Plan plan) {
		for (PlanElement pe : plan.getPlanElements()) {
			if (pe instanceof Activity &&
					((Activity) pe).getType().equals( FREIGHT )) {
				return false;
			}
		}

		return true;
	}
}

