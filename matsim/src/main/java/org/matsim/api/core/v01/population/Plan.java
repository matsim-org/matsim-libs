/* *********************************************************************** *
 * project: org.matsim.*
 * Plan.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007, 2009 by the members listed in the COPYING,  *
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

import org.matsim.api.core.v01.Customizable;
import org.matsim.core.api.internal.MatsimPopulationObject;

import java.util.List;

/**
 * A plan contains the intention of an agent.  In consequence, all information is <i>expected</i>.  For example,
 * travel times and travel distances in routes are expected.  Even something like mode could be expected, if the
 * plan is fed into a mobsim that is within-day replanning capable at the mode level.
 * <p/>
 * The only thing which is not "expected" in the same sense is the score.
 *
 */
public interface Plan extends MatsimPopulationObject, Customizable, BasicPlan {
	
	public abstract boolean isSelected();

	public List<PlanElement> getPlanElements();

	public void addLeg(final Leg leg);

	public void addActivity(final Activity act);

    public String getType();

    public void setType(final String type);

	public Person getPerson();

	/**
	 * Sets the reference to the person.
	 * This is done automatically if using Person.addPlan(). Make
	 * sure that the bidirectional reference is set correctly if
	 * you are using this method!.
	 */
	public void setPerson(Person person);

}
