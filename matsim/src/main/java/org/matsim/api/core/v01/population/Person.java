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

import java.io.Serializable;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Identifiable;
import org.matsim.utils.customize.Customizable;

/**
 * @author dgrether
 */
public interface Person extends Identifiable, Serializable, Customizable{
	
	public List<? extends Plan> getPlans();

	public void setId(final Id id);
	/**
	 * adds the plan to the Person's List of plans and
	 * sets the reference to this person in the Plan instance.
	 */
	public boolean addPlan(final Plan p);

	public Plan getSelectedPlan();

}
