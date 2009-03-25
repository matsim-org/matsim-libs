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

package org.matsim.basic.v01;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.basic.v01.population.BasicActivity;
import org.matsim.api.basic.v01.population.BasicLeg;
import org.matsim.api.basic.v01.population.BasicPerson;
import org.matsim.api.basic.v01.population.BasicPlan;
import org.matsim.api.basic.v01.population.BasicPlanElement;
import org.matsim.core.api.population.Plan;

/**
 * @author david
 */
public class BasicPlanImpl implements BasicPlan {

	private final static Logger log = Logger.getLogger(BasicPlanImpl.class);

	protected ArrayList<BasicPlanElement> actsLegs = new ArrayList<BasicPlanElement>();

	private Double score = null;
	private BasicPerson person = null;

	private Plan.Type type = null;

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


	public Plan.Type getType() {
		return this.type;
	}


	public void setType(Plan.Type type) {
		this.type = type;
	}

	public List<? extends BasicPlanElement> getPlanElements() {
		return this.actsLegs;
	}

	/**
	 * Iterator that steps through all Activities ignoring the Legs
	 */
	public class ActIterator implements Iterator<BasicActivityImpl> {
		private int index = 0;

		public boolean hasNext() {
			return BasicPlanImpl.this.actsLegs.size() > this.index;
		}

		public BasicActivityImpl next() {
			this.index+=2;
			return (BasicActivityImpl)BasicPlanImpl.this.actsLegs.get(this.index-2);
		}

		public void remove() {
			// not supported?
			throw new UnsupportedOperationException("Remove is not supported with this iterator");
		}
	}

	/**
	 * Iterator that steps through all Legs ignoring Activities
	 */
	public class LegIterator implements Iterator {
		private int index = 1;

		public boolean hasNext() {
			return BasicPlanImpl.this.actsLegs.size() > this.index;
		}

		public BasicLeg next() {
			this.index+=2;
			return (BasicLeg)BasicPlanImpl.this.actsLegs.get(this.index-2);
		}

		public void remove() {
			// not supported?
			throw new UnsupportedOperationException("Remove is not supported with this iterator");
		}
	}

	public LegIterator getIteratorLeg () {
		return new LegIterator();
	}

	public ActIterator getIteratorAct () {
		return new ActIterator();
	}


	public void addLeg(final BasicLeg leg) {
		if (this.actsLegs.size() %2 == 0 ) throw (new IndexOutOfBoundsException("Error: Tried to insert leg at non-leg position"));
		this.actsLegs.add(leg);
	}

	public void addAct(final BasicActivity act) {
		if (this.actsLegs.size() %2 != 0 ) throw (new IndexOutOfBoundsException("Error: Tried to insert act at non-act position"));
		this.actsLegs.add(act);
	}

	public boolean containsActivity( String activityType ){
		ActIterator planit = getIteratorAct();		
		while( planit.hasNext())	
			if( planit.next().getType().equals(activityType) )
				return true;
		return false;
	}
}
