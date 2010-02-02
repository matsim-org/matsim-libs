/* *********************************************************************** *
 * project: org.matsim.*
 * Plan.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007, 2008 by the members listed in the COPYING,  *
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

package org.matsim.core.population;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.utils.misc.Time;

public class PlanImpl implements Plan {
	/**
	 * @deprecated use Leg.Mode instead
	 */
	@Deprecated
	public enum Type { CAR, PT, RIDE, BIKE, WALK, UNDEFINED}

	protected ArrayList<PlanElement> actsLegs = new ArrayList<PlanElement>();

	private Double score = null;
	private Person person = null;

	private PlanImpl.Type type = null;

	private final static Logger log = Logger.getLogger(PlanImpl.class);

	private final static String ACT_ERROR = "The order of 'acts'/'legs' is wrong in some way while trying to create an 'act'.";

	public PlanImpl(final Person person) {
		this.person = person;
	}

	public PlanImpl() {
	}

	public final ActivityImpl createAndAddActivity(final String type, final Coord coord) {
		verifyCreateAct();
		ActivityImpl a = new ActivityImpl(type, coord);
		getPlanElements().add(a);
		return a;
	}

	public final ActivityImpl createAndAddActivity(final String type, final ActivityFacility fac) {
		verifyCreateAct();
		ActivityImpl a = new ActivityImpl(type, fac);
		getPlanElements().add(a);
		return a;
	}


	public final ActivityImpl createAndAddActivity(final String type, final Id linkId) {
		verifyCreateAct();
		ActivityImpl a = new ActivityImpl(type, linkId);
		getPlanElements().add(a);
		return a;
	}

	//////////////////////////////////////////////////////////////////////
	// create methods
	//////////////////////////////////////////////////////////////////////

	public LegImpl createAndAddLeg(final TransportMode mode) {
		verifyCreateLeg();
		LegImpl leg = new LegImpl(mode);
		// Override leg number with an appropriate value
		getPlanElements().add(leg);
		return leg;
	}

	private final void verifyCreateLeg() throws IllegalStateException {
		if (getPlanElements().size() % 2 == 0) {
			throw new IllegalStateException("The order of 'acts'/'legs' is wrong in some way while trying to create a 'leg'.");
		}
	}

	private final void verifyCreateAct() throws IllegalStateException {
		if (getPlanElements().size() % 2 != 0) {
			throw new IllegalStateException(ACT_ERROR);
		}
	}

	//////////////////////////////////////////////////////////////////////
	// remove methods
	//////////////////////////////////////////////////////////////////////

	/**
	 * Removes the specified act from the plan as well as a leg according to the following rule:
	 * <ul>
	 * <li>first act: removes the act and the following leg</li>
	 * <li>last act: removes the act and the previous leg</li>
	 * <li>in-between act: removes the act, removes the previous leg's route, and removes the following leg.
	 * </ul>
	 *
	 * @param index
	 */
	public final void removeActivity(final int index) {
		if ((index % 2 != 0) || (index < 0) || (index > getPlanElements().size()-1)) {
			log.warn(this + "[index=" + index +" is wrong. nothing removed]");
		}
		else if (getPlanElements().size() == 1) {
			log.warn(this + "[index=" + index +" only one act. nothing removed]");
		}
		else {
			if (index == 0) {
				// remove first act and first leg
				getPlanElements().remove(index+1); // following leg
				getPlanElements().remove(index); // act
			}
			else if (index == getPlanElements().size()-1) {
				// remove last act and last leg
				getPlanElements().remove(index); // act
				getPlanElements().remove(index-1); // previous leg
			}
			else {
				// remove an in-between act
				LegImpl prev_leg = (LegImpl)getPlanElements().get(index-1); // prev leg;
				prev_leg.setDepartureTime(Time.UNDEFINED_TIME);
				prev_leg.setTravelTime(Time.UNDEFINED_TIME);
				prev_leg.setArrivalTime(Time.UNDEFINED_TIME);
				prev_leg.setRoute(null);

				getPlanElements().remove(index+1); // following leg
				getPlanElements().remove(index); // act
			}
		}
	}

	/**
	 * Removes the specified leg <b>and</b> the following act, too! If the following act is not the last one,
	 * the following leg will be emptied to keep consistency (i.e. for the route)
	 *
	 * @param index
	 */
	public final void removeLeg(final int index) {
		if ((index % 2 == 0) || (index < 1) || (index >= getPlanElements().size()-1)) {
			log.warn(this + "[index=" + index +" is wrong. nothing removed]");
		}
		else {
			if (index != getPlanElements().size()-2) {
				// not the last leg
				LegImpl next_leg = (LegImpl)getPlanElements().get(index+2);
				next_leg.setDepartureTime(Time.UNDEFINED_TIME);
				next_leg.setTravelTime(Time.UNDEFINED_TIME);
				next_leg.setArrivalTime(Time.UNDEFINED_TIME);
				next_leg.setRoute(null);
			}
			getPlanElements().remove(index+1); // following act
			getPlanElements().remove(index); // leg
		}
	}

	public final PersonImpl getPerson() {
		return (PersonImpl) this.person;
	}

	public void setPerson(final Person person) {
		this.person = person;
	}

	public final Double getScore() {
		return this.score;
	}

	public void setScore(final Double score) {
		this.score = score;
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

	public final void addLeg(final Leg leg) {
		if (this.actsLegs.size() %2 == 0 ) throw (new IllegalStateException("Error: Tried to insert leg at non-leg position"));
		this.actsLegs.add(leg);
	}

	public final void addActivity(final Activity act) {
		if (this.actsLegs.size() %2 != 0 ) throw (new IllegalStateException("Error: Tried to insert act at non-act position"));
		this.actsLegs.add(act);
	}

	public final boolean isSelected() {
		return this.getPerson().getSelectedPlan() == this;
	}

	public void setSelected(final boolean selected) {
		this.getPerson().setSelectedPlan(this);
	}

	@Override
	public final String toString() {

		String scoreString = "undefined";
		if (this.getScore() != null) {
			scoreString = this.getScore().toString();
		}

		return "[score=" + scoreString + "]" +
				"[selected=" + this.isSelected() + "]" +
				"[nof_acts_legs=" + getPlanElements().size() + "]";
	}

	/** loads a copy of an existing plan, but keeps the person reference
	 * @param in a plan who's data will be loaded into this plan
	 **/
	public void copyPlan(final Plan in) {
		// TODO should be re-implemented making use of Cloneable
		setScore(in.getScore());
		if (in instanceof PlanImpl) {
			this.setType(((PlanImpl) in).getType());
		}
//		setPerson(in.getPerson()); // do not copy person, but keep the person we're assigned to
		for (PlanElement pe : in.getPlanElements()) {
			if (pe instanceof ActivityImpl) {
				ActivityImpl a = (ActivityImpl) pe;
				getPlanElements().add(new ActivityImpl(a));
			} else if (pe instanceof LegImpl) {
				LegImpl l = (LegImpl) pe;
				LegImpl l2 = createAndAddLeg(l.getMode());
				l2.setDepartureTime(l.getDepartureTime());
				l2.setTravelTime(l.getTravelTime());
				l2.setArrivalTime(l.getArrivalTime());
				if (l.getRoute() != null) {
					l2.setRoute(l.getRoute().clone());
				}
			}
		}
	}

	/**
	 * Inserts a leg and a following act at position <code>pos</code> into the plan.
	 *
	 * @param pos the position where to insert the leg-act-combo. acts and legs are both counted from the beginning starting at 0.
	 * @param leg the leg to insert
	 * @param act the act to insert, following the leg
	 * @throws IllegalArgumentException If the leg and act cannot be inserted at the specified position without retaining the correct order of legs and acts.
	 */
	public void insertLegAct(final int pos, final LegImpl leg, final ActivityImpl act) throws IllegalArgumentException {
		if (pos < getPlanElements().size()) {
			Object o = getPlanElements().get(pos);
			if (!(o instanceof LegImpl)) {
				throw new IllegalArgumentException("Position to insert leg and act is not valid (act instead of leg at position).");
			}
		} else if (pos > getPlanElements().size()) {
			throw new IllegalArgumentException("Position to insert leg and act is not valid.");
		}

		getPlanElements().add(pos, act);
		getPlanElements().add(pos, leg);
	}

	public LegImpl getPreviousLeg(final Activity act) {
		int index = this.getActLegIndex(act);
		if (index != -1) {
			return (LegImpl) getPlanElements().get(index-1);
		}
		return null;
	}

	public ActivityImpl getPreviousActivity(final Leg leg) {
		int index = this.getActLegIndex(leg);
		if (index != -1) {
			return (ActivityImpl) getPlanElements().get(index-1);
		}
		return null;
	}

	public LegImpl getNextLeg(final Activity act) {
		int index = this.getActLegIndex(act);
		if ((index < getPlanElements().size() - 1) && (index != -1)) {
			return (LegImpl) getPlanElements().get(index+1);
		}
		return null;
	}

	public ActivityImpl getNextActivity(final Leg leg) {
		int index = this.getActLegIndex(leg);
		if (index != -1) {
			return (ActivityImpl) getPlanElements().get(index+1);
		}
		return null;
	}

	public int getActLegIndex(final Object o) {
		if ((o instanceof LegImpl) || (o instanceof ActivityImpl)) {
			for (int i = 0; i < getPlanElements().size(); i++) {
				if (getPlanElements().get(i).equals(o)) {
					return i;
				}
			}
			return -1;
		}
		throw new IllegalArgumentException("Method call only valid with a Leg or Act instance as parameter!");
	}

	public ActivityImpl getFirstActivity() {
		return (ActivityImpl) getPlanElements().get(0);
	}

	public ActivityImpl getLastActivity() {
		return (ActivityImpl) getPlanElements().get(getPlanElements().size() - 1);
	}

}
