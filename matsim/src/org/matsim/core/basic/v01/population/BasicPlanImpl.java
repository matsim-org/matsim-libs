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

package org.matsim.core.basic.v01.population;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.basic.v01.population.BasicActivity;
import org.matsim.api.basic.v01.population.BasicLeg;
import org.matsim.api.basic.v01.population.BasicPerson;
import org.matsim.api.basic.v01.population.BasicPlan;
import org.matsim.api.basic.v01.population.PlanElement;
import org.matsim.core.population.PlanImpl;

/**
 * @author david
 */
public class BasicPlanImpl implements BasicPlan<PlanElement> {

	protected ArrayList<PlanElement> actsLegs = new ArrayList<PlanElement>();

	private Double score = null;
	private BasicPerson person = null;

	private PlanImpl.Type type = null;

	private boolean isSelected;

	public BasicPlanImpl(final BasicPerson person) {
		// yyyyyy this should be protected
		this.person = person;
	}
	
	protected BasicPlanImpl() {
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
		throw new UnsupportedOperationException("Not supported at this level");
	}

	public void setSelected(boolean selected) {
		throw new UnsupportedOperationException("Not supported at this level");
//		this.isSelected = selected;
	}


	public PlanImpl.Type getType() {
		return this.type;
	}


	public void setType(PlanImpl.Type type) {
		this.type = type;
	}

	public final List<PlanElement> getPlanElements() {
		return this.actsLegs;
	}

	public final void addLeg(final BasicLeg leg) {
		if (this.actsLegs.size() %2 == 0 ) throw (new IllegalStateException("Error: Tried to insert leg at non-leg position"));
		this.actsLegs.add(leg);
	}

	public final void addActivity(final BasicActivity act) {
		if (this.actsLegs.size() %2 != 0 ) throw (new IllegalStateException("Error: Tried to insert act at non-act position"));
		this.actsLegs.add(act);
	}

}
