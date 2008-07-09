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

import org.apache.log4j.Logger;


/**
 * @author david
 *
 */
public class BasicPlanImpl implements /*Serializable, */BasicPlan {
	
	/* removed serialization because I think nobody uses it. If someone does,
	 * please complain, otherwise I'll soon delete these lines permanently.
	 * marcel/9jul2008
	 * TODO [MR] delete code
	 */
//	private static final long serialVersionUID = 1L;

	private final static Logger log = Logger.getLogger(BasicPlanImpl.class);

	protected ArrayList<Object> actsLegs = new ArrayList<Object>();

	private double score = UNDEF_SCORE;
	
	private Type type = null;
	
	private boolean isSelected;
	
	/* (non-Javadoc)
	 * @see org.matsim.basic.v01.BasicPlan#getScore()
	 */
	public final double getScore() {
		return this.score;
	}

	/* (non-Javadoc)
	 * @see org.matsim.basic.v01.BasicPlan#setScore(double)
	 */
	public final void setScore(final double score) {
		if (Double.isInfinite(score)) {
			log.warn("Infinite score is not supported! Score is not changed");
		} else {
			this.score = score;
		}
	}
	
	public boolean isSelected() {
		return isSelected;
	}
	
	public void setSelected(boolean selected) {
		this.isSelected = selected;
	}


	public Type getType() {
		return this.type;
	}
	

	public void setType(Type type) {
		this.type = type;
	}

	
	/* (non-Javadoc)
	 * @see org.matsim.basic.v01.BasicPlan#hasUndefinedScore()
	 */
	public final boolean hasUndefinedScore() {
		return isUndefinedScore(this.getScore());
	}

	/** @param score The score to test.
	 * @return true if <code>score</code> has the meaning of "undefined score". */
	public static final boolean isUndefinedScore(final double score) {
		return Double.isNaN(score);
	}

	/**
	 * Iterator that steps through all Activities ignoring the Legs
	 */
	public class ActIterator implements Iterator<BasicActImpl> {
		private int index = 0;

		public boolean hasNext() {
			return BasicPlanImpl.this.actsLegs.size() > this.index;
		}

		public BasicActImpl next() {
			this.index+=2;
			return (BasicActImpl)BasicPlanImpl.this.actsLegs.get(this.index-2);
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

	/**
	 * Iterator-like class, that steps through all Activities and Legs
	 * You have to call nextLeg(), nextAct alternating, otherwise
	 * an exception is thrown.
	 */
	public class ActLegIterator {
		private int index = 0;

		public boolean hasNextLeg() {
			return BasicPlanImpl.this.actsLegs.size() > this.index+1;
		}

		public BasicLeg nextLeg() {
			if (this.index % 2 == 0 ) throw new IndexOutOfBoundsException("Requested Leg on Act-Position");
			BasicLeg leg = (BasicLeg)BasicPlanImpl.this.actsLegs.get(this.index);
			this.index+=1;
			return leg;
		}

		public BasicAct nextAct() {
			if (this.index % 2 != 0 ) throw new IndexOutOfBoundsException("Requested Act on Leg-Position");
			BasicAct act = (BasicAct)BasicPlanImpl.this.actsLegs.get(this.index);
			this.index+=1;
			return act;
		}

		public void remove() {
			// not supported?
			throw new UnsupportedOperationException("Remove is not supported with this iterator");
		}
	}

	/* (non-Javadoc)
	 * @see org.matsim.basic.v01.BasicPlan#getIterator()
	 */
	public ActLegIterator getIterator() {
		return new ActLegIterator();
	}
	/* (non-Javadoc)
	 * @see org.matsim.basic.v01.BasicPlan#getIteratorLeg()
	 */
	public LegIterator getIteratorLeg () {
		return new LegIterator();
	}
	/* (non-Javadoc)
	 * @see org.matsim.basic.v01.BasicPlan#getIteratorAct()
	 */
	public ActIterator getIteratorAct () {
		return new ActIterator();
	}

	/* (non-Javadoc)
	 * @see org.matsim.basic.v01.BasicPlan#addLeg(org.matsim.basic.v01.BasicLeg)
	 */
	public void addLeg(final BasicLeg leg) {
		if (this.actsLegs.size() %2 == 0 ) throw (new IndexOutOfBoundsException("Error: Tried to insert leg at non-leg position"));
		this.actsLegs.add(leg);
	}

	/* (non-Javadoc)
	 * @see org.matsim.basic.v01.BasicPlan#addAct(org.matsim.basic.v01.BasicAct)
	 */
	public void addAct(final BasicAct act) {
		if (this.actsLegs.size() %2 != 0 ) throw (new IndexOutOfBoundsException("Error: Tried to insert act at non-act position"));
		this.actsLegs.add(act);
	}

}
