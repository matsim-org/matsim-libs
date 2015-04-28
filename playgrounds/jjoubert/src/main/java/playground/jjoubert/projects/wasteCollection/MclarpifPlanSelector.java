/* *********************************************************************** *
 * project: org.matsim.*
 * ConvertOsmToMatsim.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

/**
 * 
 */
package playground.jjoubert.projects.wasteCollection;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.HasPlansAndId;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.replanning.selectors.PlanSelector;

/**
 * Shell for solving the MCLARPIF waste problems.
 * 
 * @author jwjoubert
 */
public class MclarpifPlanSelector implements PlanSelector {
	final private Logger log = Logger.getLogger(MclarpifPlanSelector.class);

	/* (non-Javadoc)
	 * @see org.matsim.core.replanning.selectors.GenericPlanSelector#selectPlan(org.matsim.api.core.v01.population.HasPlansAndId)
	 */
	@Override
	public Plan selectPlan(HasPlansAndId<Plan, Person> member) {
		// TODO Auto-generated method stub
		log.warn("returning the 'same' plan as last selected.");
		return member.createCopyOfSelectedPlanAndMakeSelected();
	}

}
