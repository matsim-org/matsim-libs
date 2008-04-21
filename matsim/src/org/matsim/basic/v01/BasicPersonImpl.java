/* *********************************************************************** *
 * project: org.matsim.*
 * BasicPerson.java
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

import java.util.ArrayList;
import java.util.List;


public class BasicPersonImpl<T extends BasicPlan> implements BasicPerson<T> {

	protected List<T> plans = new ArrayList<T>(6);
	protected T selectedPlan = null;
	protected Id id;

	public BasicPersonImpl(final String id) {
		this(new IdImpl(id));
	}

	public BasicPersonImpl(final IdImpl id) {
		this.id = id;
	}

	/* (non-Javadoc)
	 * @see org.matsim.basic.v01.BasicPerson#addPlan(T)
	 */
	public void addPlan(final T plan) {
		this.plans.add(plan);
		// Make sure there is a selected plan if there is at least one plan
		if (this.selectedPlan == null) this.selectedPlan = plan;
	}

	/* (non-Javadoc)
	 * @see org.matsim.basic.v01.BasicPerson#getSelectedPlan()
	 */
	public T getSelectedPlan() {
		return this.selectedPlan;
	}

	/* (non-Javadoc)
	 * @see org.matsim.basic.v01.BasicPerson#setSelectedPlan(T)
	 */
	public void setSelectedPlan(final T selectedPlan) {
		if (this.plans.contains(selectedPlan)) {
			this.selectedPlan = selectedPlan;
		}
	}

	/* (non-Javadoc)
	 * @see org.matsim.basic.v01.BasicPerson#getPlans()
	 */
	public List<T> getPlans() {
		return this.plans;
	}

	/* (non-Javadoc)
	 * @see org.matsim.basic.v01.BasicPerson#getId()
	 */
	public Id getId() {
		return this.id;
	}

	/* (non-Javadoc)
	 * @see org.matsim.basic.v01.BasicPerson#setId(org.matsim.utils.identifiers.IdI)
	 */
	public void setId(final Id id) {
		this.id = id;
	}

	/* (non-Javadoc)
	 * @see org.matsim.basic.v01.BasicPerson#setId(java.lang.String)
	 */
	public void setId(final String idstring) {
		this.id = new IdImpl(idstring);
	}

}
