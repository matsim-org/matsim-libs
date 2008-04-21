/* *********************************************************************** *
 * project: org.matsim.*
 * BasicAct.java
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
package org.matsim.basic.v01;

import java.util.List;

/**
*
* @author dgrether
*
*/

public interface BasicPerson<T extends BasicPlan> {

	public void addPlan(final T plan);

	public T getSelectedPlan();

	/**
	 * Sets the selected plan of a person. If the plan is not part of the person,
	 * nothing is changed.
	 *
	 * @param selectedPlan the plan to be the selected one of the person
	 */
	public void setSelectedPlan(final T selectedPlan);

	public List<T> getPlans();

	public Id getId();

	public void setId(final Id id);

	public void setId(final String idstring);

}