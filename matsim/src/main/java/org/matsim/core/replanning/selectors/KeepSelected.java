/* *********************************************************************** *
 * project: org.matsim.*
 * KeepSelected.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package org.matsim.core.replanning.selectors;

import org.matsim.api.core.v01.population.BasicPlan;
import org.matsim.api.core.v01.population.HasPlansAndId;

/**
 * Keeps the currently selected plan selected and returns it.
 * 
 * 
 * 
 * @author mrieser
 */
public class KeepSelected<T extends BasicPlan, I> implements PlanSelector<T, I> {

	/**
	 * returns the already selected plan for this person
	 * 
	 * One must be careful when using it, since the last executed plan might not be selected
	 * since it might have been removed at the start of the new iteration
	 * (because of the maxnumberofplans limit). balac aug '17
	 */
	@Override
	public T selectPlan(HasPlansAndId<T, I> person) {
		return person.getSelectedPlan();
	}

}
