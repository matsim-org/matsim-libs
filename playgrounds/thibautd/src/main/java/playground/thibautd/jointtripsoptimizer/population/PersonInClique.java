/* *********************************************************************** *
 * project: org.matsim.*
 * PersonInClique.java
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
package playground.thibautd.jointtripsoptimizer.population;

import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;

import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;

/**
 * @author thibautd
 */
public class PersonInClique implements Person {
	Person personDelegate;

	/*
	 * =========================================================================
	 * Delegate methods
	 * =========================================================================
	 */

	/**
	 * @see Person#getPlans()
	 */
	public List<? extends Plan> getPlans()
	{
		return personDelegate.getPlans();
	}

	/**
	 * @see Person#setId(Id)
	 */
	public void setId(Id id)
	{
		personDelegate.setId(id);
	}

	/**
	 * @see Person#addPlan(Plan)
	 */
	public boolean addPlan(Plan p)
	{
		return personDelegate.addPlan(p);
	}

	/**
	 * @see Person#getSelectedPlan()
	 */
	public Plan getSelectedPlan()
	{
		return personDelegate.getSelectedPlan();
	}

	/**
	 * @see org.matsim.api.core.v01.Identifiable#getId()
	 */
	public Id getId()
	{
		return personDelegate.getId();
	}

	/**
	 * @see org.matsim.utils.customize.Customizable#getCustomAttributes()
	 */
	public Map<String,Object> getCustomAttributes()
	{
		return personDelegate.getCustomAttributes();
	}
}

