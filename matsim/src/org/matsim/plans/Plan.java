/* *********************************************************************** *
 * project: org.matsim.*
 * Plan.java
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

package org.matsim.plans;

import java.util.ArrayList;
import java.util.List;

import org.matsim.basic.v01.BasicPlan;
import org.matsim.gbl.Gbl;
import org.matsim.network.Link;
import org.matsim.stats.algorithms.PlanStatsI;


public class Plan extends BasicPlan {

	private static final long serialVersionUID = 1L;

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private String type = null;

	private transient Person person = null;

	public PlanStatsI firstPlanStatsAlgorithm = null;

	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public Plan(final Person person) {
		this.person = person;
	}

	public Plan(final String score, final String age, final Person person) {
		this.person = person;
		if (score != null) { this.setScore(Double.parseDouble(score)); }
	}

	//////////////////////////////////////////////////////////////////////
	// create methods
	//////////////////////////////////////////////////////////////////////

	public final Act createAct(final String type, final Double x, final Double y, final String link, final String startTime,
														 final String endTime, final String dur, final String isPrimary) throws Exception {
		verifyCreateAct(endTime);
		Act a = new Act(type, x, y, link, startTime, endTime, dur, isPrimary);
		this.actsLegs.add(a);
		return a;
	}

	public final Act createAct(final String type, final String x, final String y, final String link, final String startTime,
			 final String endTime, final String dur, final String isPrimary) throws Exception {
		Double xx = (x == null) ? null : Double.valueOf(x);
		Double yy = (y == null) ? null : Double.valueOf(y);
		return createAct(type, xx, yy, link, startTime, endTime, dur, isPrimary);
	}

	public final Act createAct(final String type, final double x, final double y, final Link link, final double startTime,
			final double endTime, final double dur, final boolean isPrimary) throws Exception {
		if (endTime == Gbl.UNDEFINED_TIME) {
			verifyCreateAct(null);
		} else {
			verifyCreateAct(Double.toString(endTime));
		}
		Act a = new Act(type, x, y, link, startTime, endTime, dur, isPrimary);
		this.actsLegs.add(a);
		return a;
	}

	public final Leg createLeg(final String num, final String mode, final String depTime,
														 final String travTime, final String arrTime) throws Exception {
		verifyCreateLeg();
		String legnum = num;
		if (num == null) {
			// Override leg number with an appropriate value if no number was give
			int legnumber = (this.actsLegs.size()-1) /2;
			legnum = Integer.toString(legnumber);
		}
		Leg l = new Leg(legnum, mode, depTime, travTime, arrTime);
		this.actsLegs.add(l);
		return l;
	}

	public final Leg createLeg(final int num, final String mode, final double depTime,
			 final double travTime, final double arrTime) throws Exception {
		verifyCreateLeg();
		// Override leg number with an appropriate value
		int legnum = (this.actsLegs.size()-1) /2;
		Leg l = new Leg(legnum, mode, depTime, travTime, arrTime);
		this.actsLegs.add(l);
		return l;
	}

	private final void verifyCreateLeg() throws Exception {
		if (this.actsLegs.size() % 2 == 0) {
			throw new Exception("The order of 'acts'/'legs' is wrong in some way while trying to create a 'leg'.");
		}
		if ((this.actsLegs.size() > 1) && (((Act)this.actsLegs.get(this.actsLegs.size()-1)).getDur() == Integer.MIN_VALUE)) {
			throw new Exception("All but the first and last 'act' must have a duration.");
		}
	}

	private final void verifyCreateAct(final String end_time) throws Exception {
		if (this.actsLegs.size() % 2 != 0) {
			throw new Exception("The order of 'acts'/'legs' is wrong in some way while trying to create an 'act'.");
		}
		if ((this.actsLegs.size() == 0) && (end_time == null)) {
			throw new Exception("The first 'act' has to have an end time.");
		}
	}

	//////////////////////////////////////////////////////////////////////
	// remove methods
	//////////////////////////////////////////////////////////////////////

	// for in-between acts: removes the specified act, emptying the previous leg
	//                      and removes the following leg.
	// for first act: removes the act and the following leg
	// for last act: removes the act and the previous leg
	public final void removeAct(final int index) {
		if ((index % 2 != 0) || (index < 0) || (index > this.actsLegs.size()-1)) {
			Gbl.warningMsg(this.getClass(),"removeAct(...)",this + "[index=" + index +" is wrong. nothing removed]");
		}
		else if (this.actsLegs.size() == 1) {
			Gbl.warningMsg(this.getClass(),"removeAct(...)",this + "[index=" + index +" only one act. nothing removed]");
		}
		else {
			if (index == 0) {
				// remove first act and first leg
				this.actsLegs.remove(index+1); // following leg
				this.actsLegs.remove(index); // act
			}
			else if (index == this.actsLegs.size()-1) {
				// remove last act and last leg
				this.actsLegs.remove(index); // act
				this.actsLegs.remove(index-1); // previous leg
			}
			else {
				// remove an in-between act
				Leg prev_leg = (Leg)this.actsLegs.get(index-1); // prev leg
				prev_leg.setNum(Integer.MIN_VALUE);
				prev_leg.setDepTime(Gbl.UNDEFINED_TIME);
				prev_leg.setTravTime(Gbl.UNDEFINED_TIME);
				prev_leg.setArrTime(Gbl.UNDEFINED_TIME);
				prev_leg.removeRoute();

				this.actsLegs.remove(index+1); // following leg
				this.actsLegs.remove(index); // act
			}
		}
	}

	// removes the specified leg AND the following act, too!
	// if the following act is not the last one, the following leg will be emptied
	// to keep consistency (i.e. for the route)
	public final void removeLeg(final int index) {
		if ((index % 2 == 0) || (index < 1) || (index >= this.actsLegs.size()-1)) {
			Gbl.warningMsg(this.getClass(),"removeLeg(...)",this + "[index=" + index +" is wrong. nothing removed]");
		}
		else {
			if (index != this.actsLegs.size()-2) {
				// not the last leg
				Leg next_leg = (Leg)this.actsLegs.get(index+2);
				next_leg.setNum(Integer.MIN_VALUE);
				next_leg.setDepTime(Gbl.UNDEFINED_TIME);
				next_leg.setTravTime(Gbl.UNDEFINED_TIME);
				next_leg.setArrTime(Gbl.UNDEFINED_TIME);
				next_leg.removeRoute();
			}
			this.actsLegs.remove(index+1); // following act
			this.actsLegs.remove(index); // leg
		}
	}

	//////////////////////////////////////////////////////////////////////
	// get methods
	//////////////////////////////////////////////////////////////////////

	public final ArrayList<Object> getActsLegs() {
		return this.actsLegs;
	}

	public final String getType() {
		return this.type;
	}

	public final Person getPerson() {
		return this.person;
	}

	//////////////////////////////////////////////////////////////////////
	// set methods
	//////////////////////////////////////////////////////////////////////

	public final void setType(final String type) {
		this.type = (type == null) ? null : type.intern();
	}

	/**
	 * Loops through all acts and updates their start-, endtime and duration according to the
	 * departure and arrival times in the legs between the acts
	 */
	public final void setActTimesFromLegTimes() {
		Act act1 = (Act)this.actsLegs.get(0);
		Act act2 = null;

		// the end time of act1 must be defined by definition, so take it as given
		if (act1.getStartTime() == Gbl.UNDEFINED_TIME) {
			act1.setStartTime(0);
			act1.setDur(act1.getEndTime());
		} else {
			act1.setDur(act1.getEndTime() - act1.getStartTime());
		}

		for (int i = 2; i < this.actsLegs.size(); i = i+2) {
			act2 = (Act)this.actsLegs.get(i);
			Leg leg = (Leg)this.actsLegs.get(i-1);
			act1.setEndTime(leg.getDepTime());
			act2.setStartTime(leg.getArrTime());
			act1.setDur(act1.getEndTime() - act1.getStartTime());
			act1 = act2;
		}

		double endTime = act1.getEndTime();
		if ((endTime == Gbl.UNDEFINED_TIME) || (endTime < act1.getStartTime())) {
			double duration = act1.getDur();
			if (duration == Gbl.UNDEFINED_TIME) {
				// there was no planned duration
				if (act1.getStartTime() < 24*3600) {	// TODO replace `24' with something like sim-duration
					duration = 24*3600 - act1.getStartTime();	// the last act lasts until midnight
				} else {
					duration = 3600;	// midnight is already over, just make the duration one hour long
				}
			}
			act1.setEndTime(act1.getStartTime() + duration);
			act1.setDur(duration);
		}
	}

	public void setPerson(final Person person) {
		this.person = person;
	}

	//////////////////////////////////////////////////////////////////////
	// query methods
	//////////////////////////////////////////////////////////////////////

	public final boolean isSelected() {
		return this.person.getSelectedPlan() == this;
	}

	public final boolean hasUndefinedScore() {
		return Plan.isUndefinedScore(this.getScore());
	}

	public static final boolean isUndefinedScore(final double score) {
		return Double.isNaN(score);
	}

	//////////////////////////////////////////////////////////////////////
	// print methods
	//////////////////////////////////////////////////////////////////////

	@Override
	public final String toString() {
		return "[score=" + this.getScore() + "]" +
				"[selected=" + this.isSelected() + "]" +
				"[nof_acts_legs=" + this.actsLegs.size() + "]";
	}

	// loads a copy of an existing plan
	public void copyPlan(final Plan in) {
		setScore(in.getScore());
		this.firstPlanStatsAlgorithm = null;
		this.type = in.type;
		this.person = in.person;
		List<?> actl = in.getActsLegs();
		for (int i= 0; i< actl.size() ; i++) {
			try {
				if (i % 2 == 0) {
					// activity
					Act a = (Act)actl.get(i);
					this.actsLegs.add(new Act(a));
				} else {
					// Leg
					Leg l = (Leg) actl.get(i);
					Leg l2 = createLeg(l.getNum(), l.getMode(), l.getDepTime(), l.getTravTime(),l.getArrTime());
					if (l.getRoute() != null) {
						Route r = new Route(l.getRoute());
						l2.setRoute(r);
					}
				}
			} catch (Exception e) {
				// copying a plan is fairly basic. if an exception occurs here, something
				// must be definitively wrong -- exit with an error
				Gbl.errorMsg(e);
			}
		}
	}

	/**
	 * Inserts a leg and a following act at position <code>pos</code> into the plan.
	 *
	 * @param pos the position where to insert the leg-act-combo. acts and legs are both counted from the beginning starting at 0.
	 * @param leg the leg to insert
	 * @param act the act to insert, following the leg
	 * @throws Exception If the leg and act cannot be inserted at the specified position without retaining the correct order of legs and acts.
	 */
	public void insertLegAct(final int pos, final Leg leg, final Act act) throws Exception {
		if (pos < this.actsLegs.size()) {
			Object o = this.actsLegs.get(pos);
			if ((o == null) || !(o instanceof Leg)) {
				throw new Exception("Position to insert leg and act is not valid (act instead of leg at position).");
			}
		} else if (pos > this.actsLegs.size()) {
			throw new Exception("Position to insert leg and act is not valid.");
		}

		this.actsLegs.add(pos, act);
		this.actsLegs.add(pos, leg);
	}

	public Leg getPreviousLeg(final Act act) {
		int index = this.getActLegIndex(act);
		if (index != -1) {
			return (Leg) this.actsLegs.get(index-1);
		}
		return null;
	}

	public Act getPreviousActivity(final Leg leg) {
		int index = this.getActLegIndex(leg);
		if (index != -1) {
			return (Act) this.actsLegs.get(index-1);
		}
		return null;
	}

	public Leg getNextLeg(final Act act) {
		int index = this.getActLegIndex(act);
		if ((index < this.actsLegs.size() - 1) && (index != -1)) {
			return (Leg) this.actsLegs.get(index+1);
		}
		return null;
	}

	public Act getNextActivity(final Leg leg) {
		int index = this.getActLegIndex(leg);
		if (index != -1) {
			return (Act) this.actsLegs.get(index+1);
		}
		return null;
	}

	private int getActLegIndex(final Object o) {
		if ((o instanceof Leg) || (o instanceof Act)) {
			for (int i = 0; i < this.actsLegs.size(); i++) {
				if (this.actsLegs.get(i).equals(o)) {
					return i;
				}
			}
			return -1;
		}
		throw new IllegalArgumentException("Method calls only valid with a Leg or Act instance as parameter!");
	}

	public Act getFirstActivity() {
		return (Act) this.actsLegs.get(0);
	}

}
