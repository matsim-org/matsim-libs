/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package org.matsim.api.core.v01.population;

import org.matsim.api.core.v01.Id;
import org.matsim.utils.customize.Customizable;

/**
 * @author dgrether
 */
public interface Person extends Customizable, HasPlansAndId<Plan, Person>{

////	public List<? extends Plan> getPlans();
//	public List<Plan> getPlans();
//	// ("? extends Plan" is necessary when classes that implement persons want to use something that extends Plan.  This is not the
//	// case so far.  Could be changed if it becomes the case. kai, nov'13)

	public void setId(final Id id);
//	/**
//	 * adds the plan to the Person's List of plans and
//	 * sets the reference to this person in the Plan instance.
//	 */
//	public boolean addPlan(final Plan p);
//
//	public Plan getSelectedPlan();
	
//	public void setSelectedPlan(final Plan selectedPlan);

}
