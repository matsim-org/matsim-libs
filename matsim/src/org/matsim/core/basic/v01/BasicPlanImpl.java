/* *********************************************************************** *
 * project: org.matsim.*
 * BasicPlan.java
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

package org.matsim.core.basic.v01;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.basic.v01.population.BasicActivity;
import org.matsim.api.basic.v01.population.BasicLeg;
import org.matsim.api.basic.v01.population.BasicPerson;
import org.matsim.api.basic.v01.population.BasicPlan;
import org.matsim.api.basic.v01.population.BasicPlanElement;
import org.matsim.core.population.PlanImpl;

/**
 * @author david
 */
public class BasicPlanImpl implements BasicPlan {

	protected ArrayList<BasicPlanElement> actsLegs = new ArrayList<BasicPlanElement>();

	private Double score = null;
	private BasicPerson person = null;

	private PlanImpl.Type type = null;

	private boolean isSelected;

	public BasicPlanImpl(final BasicPerson person) {
		this.person = person;
	}
	
	public BasicPerson getPerson() {
		return this.person;
	}
	
	public void setPerson(final BasicPerson person) {
		this.person = person;
	}
	
	public final Double getScore() {
		return this.score;
	}
	
	public void setScore(final Double score) {
		this.score = score;
	}

	public boolean isSelected() {
		return isSelected;
	}

	public void setSelected(boolean selected) {
		this.isSelected = selected;
	}


	public PlanImpl.Type getType() {
		return this.type;
	}


	public void setType(PlanImpl.Type type) {
		this.type = type;
	}

	public List<? extends BasicPlanElement> getPlanElements() {
		return this.actsLegs;
	}

	public void addLeg(final BasicLeg leg) {
		if (this.actsLegs.size() %2 == 0 ) throw (new IndexOutOfBoundsException("Error: Tried to insert leg at non-leg position"));
		this.actsLegs.add(leg);
	}

	public void addActivity(final BasicActivity act) {
		if (this.actsLegs.size() %2 != 0 ) throw (new IndexOutOfBoundsException("Error: Tried to insert act at non-act position"));
		this.actsLegs.add(act);
	}

}
