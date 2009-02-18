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
package org.matsim.interfaces.basic.v01;

import java.util.List;
import java.util.TreeSet;

import org.matsim.basic.v01.BasicKnowledge;
import org.matsim.population.Desires;

/**
 *
* @author dgrether
*
*/

public interface BasicPerson<T extends BasicPlan, K extends BasicKnowledge> {

//	TODO [kai]: Would make more sense to be to have something like "getAttributes" and "getPlans".  Current version seems a bit
//	over-specified to me.  kai, feb09

	public void addPlan(final T plan);

	public List<T> getPlans();

	public Id getId();

	public void setId(final Id id);

	public String getSex();

	public int getAge();

	public String getLicense();

	public boolean hasLicense();

	public String getCarAvail();

	/**
	 * @return "yes" if the person has a job
	 * @deprecated use {@link #isEmployed()}
	 */
	@Deprecated
	public String getEmployed();

	public boolean isEmployed();

	public void setAge(final int age);

	public void setSex(final String sex);

	public void setLicence(final String licence);

	public void setCarAvail(final String carAvail);

	public void setEmployed(final String employed);

//	public Knowledge createKnowledge(final String desc);

	public Desires createDesires(final String desc);

	public void addTravelcard(final String type);

	public TreeSet<String> getTravelcards();

	public K getKnowledge();

	public Desires getDesires();

	public Id getFiscalHouseholdId();

}