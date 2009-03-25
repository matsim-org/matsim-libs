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

package org.matsim.population;

import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.basic.v01.Coord;
import org.matsim.api.basic.v01.population.BasicActivity;
import org.matsim.api.basic.v01.population.BasicLeg;
import org.matsim.basic.v01.BasicPlanImpl;
import org.matsim.basic.v01.BasicPlanImpl.ActIterator;
import org.matsim.basic.v01.BasicPlanImpl.LegIterator;
import org.matsim.core.api.facilities.Facility;
import org.matsim.core.api.network.Link;
import org.matsim.core.api.population.Activity;
import org.matsim.core.api.population.CarRoute;
import org.matsim.core.api.population.Leg;
import org.matsim.core.api.population.Person;
import org.matsim.core.api.population.Plan;
import org.matsim.core.api.population.PlanElement;
import org.matsim.gbl.Gbl;
import org.matsim.population.routes.NodeCarRoute;
import org.matsim.utils.misc.Time;

public class PlanImpl implements Plan {

	private final BasicPlanImpl delegate;
	
	private final static Logger log = Logger.getLogger(PlanImpl.class);

	private final static String ACT_ERROR = "The order of 'acts'/'legs' is wrong in some way while trying to create an 'act'.";

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public PlanImpl(final Person person) {
		this.delegate = new BasicPlanImpl(person);
	}

	public static Plan createPlan( final Person p) {
		return new PlanImpl(p) ;
	}

	public final Activity createAct(final String type, final Coord coord) throws IllegalStateException {
		verifyCreateAct(Time.UNDEFINED_TIME);
		Activity a = new ActivityImpl(type, coord);
		getPlanElements().add(a);
		return a;
	}

	public final Activity createAct(final String type, final Facility fac) throws IllegalStateException {
		verifyCreateAct(Time.UNDEFINED_TIME);
		Activity a = new ActivityImpl(type, fac);
		getPlanElements().add(a);
		return a;
	}


	public final Activity createAct(final String type, final Link link) throws IllegalStateException {
		verifyCreateAct(Time.UNDEFINED_TIME);
		Activity a = new ActivityImpl(type, link);
		getPlanElements().add(a);
		return a;
	}

	//////////////////////////////////////////////////////////////////////
	// create methods
	//////////////////////////////////////////////////////////////////////

	public Leg createLeg(final BasicLeg.Mode mode) throws IllegalStateException {
		verifyCreateLeg();
		Leg leg = new LegImpl(mode);
		// Override leg number with an appropriate value
		getPlanElements().add(leg);
		return leg;
	}

	private final void verifyCreateLeg() throws IllegalStateException {
		if (getPlanElements().size() % 2 == 0) {
			throw new IllegalStateException("The order of 'acts'/'legs' is wrong in some way while trying to create a 'leg'.");
		}
	}

	private static long noEndTimeCnt = 0 ;
	private final void verifyCreateAct(final double endTime) throws IllegalStateException {
		if (getPlanElements().size() % 2 != 0) {
			throw new IllegalStateException(ACT_ERROR);
		}
		if ((getPlanElements().size() == 0) && (endTime == Time.UNDEFINED_TIME)) {
			if ( noEndTimeCnt < 1 ) {
				noEndTimeCnt++ ;
			log.warn( "First 'act' has no end time.  Some people think that the first 'act' should have an end time.  This is, however, not true, since someone can stay at the first act all day.  Future occurences of this warning will be suppressed." ) ;
//			throw new IllegalStateException("The first 'act' has to have an end time.");
			}
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
	public final void removeAct(final int index) {
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
				Leg prev_leg = (Leg)getPlanElements().get(index-1); // prev leg;
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
				Leg next_leg = (Leg)getPlanElements().get(index+2);
				next_leg.setDepartureTime(Time.UNDEFINED_TIME);
				next_leg.setTravelTime(Time.UNDEFINED_TIME);
				next_leg.setArrivalTime(Time.UNDEFINED_TIME);
				next_leg.setRoute(null);
			}
			getPlanElements().remove(index+1); // following act
			getPlanElements().remove(index); // leg
		}
	}

	//////////////////////////////////////////////////////////////////////
	// get methods
	//////////////////////////////////////////////////////////////////////

	@SuppressWarnings("unchecked")
	public final List<PlanElement> getPlanElements() {
		return (List<PlanElement>) this.delegate.getPlanElements();
	}

	public final Person getPerson() {
		return (Person) this.delegate.getPerson();
	}

	public void setPerson(final Person person) {
		this.delegate.setPerson(person);// FIXME [MR]
	}

	public final boolean isSelected() {
		return getPerson().getSelectedPlan() == this;
	}

	public void setSelected(final boolean selected) {
		this.getPerson().setSelectedPlan(this);
		this.delegate.setSelected(selected);
	}

	@Override
	public final String toString() {
		return "[score=" + this.getScore().toString() + "]" +
				"[selected=" + this.isSelected() + "]" +
				"[nof_acts_legs=" + getPlanElements().size() + "]";
	}

	/** loads a copy of an existing plan
	 * @param in a plan who's data will be loaded into this plan
	 **/
	public void copyPlan(final Plan in) {
		setScore(in.getScore());
		this.setType(in.getType());
		setPerson(in.getPerson());
		List<?> actl = in.getPlanElements();
		for (int i= 0; i< actl.size() ; i++) {
			try {
				if (i % 2 == 0) {
					// activity
					Activity a = (Activity)actl.get(i);
					getPlanElements().add(new ActivityImpl(a));
				} else {
					// Leg
					Leg l = (Leg) actl.get(i);
					Leg l2 = createLeg(l.getMode());
					l2.setDepartureTime(l.getDepartureTime());
					l2.setTravelTime(l.getTravelTime());
					l2.setArrivalTime(l.getArrivalTime());
					if (l.getRoute() != null) {
						CarRoute r = new NodeCarRoute((CarRoute) l.getRoute());
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
	 * @throws IllegalArgumentException If the leg and act cannot be inserted at the specified position without retaining the correct order of legs and acts.
	 */
	public void insertLegAct(final int pos, final Leg leg, final Activity act) throws IllegalArgumentException {
		if (pos < getPlanElements().size()) {
			Object o = getPlanElements().get(pos);
			if (!(o instanceof Leg)) {
				throw new IllegalArgumentException("Position to insert leg and act is not valid (act instead of leg at position).");
			}
		} else if (pos > getPlanElements().size()) {
			throw new IllegalArgumentException("Position to insert leg and act is not valid.");
		}

		getPlanElements().add(pos, act);
		getPlanElements().add(pos, leg);
	}

	public Leg getPreviousLeg(final Activity act) {
		int index = this.getActLegIndex(act);
		if (index != -1) {
			return (Leg) getPlanElements().get(index-1);
		}
		return null;
	}

	public Activity getPreviousActivity(final Leg leg) {
		int index = this.getActLegIndex(leg);
		if (index != -1) {
			return (Activity) getPlanElements().get(index-1);
		}
		return null;
	}

	public Leg getNextLeg(final Activity act) {
		int index = this.getActLegIndex(act);
		if ((index < getPlanElements().size() - 1) && (index != -1)) {
			return (Leg) getPlanElements().get(index+1);
		}
		return null;
	}

	public Activity getNextActivity(final Leg leg) {
		int index = this.getActLegIndex(leg);
		if (index != -1) {
			return (Activity) getPlanElements().get(index+1);
		}
		return null;
	}

	private int getActLegIndex(final Object o) {
		if ((o instanceof Leg) || (o instanceof Activity)) {
			for (int i = 0; i < getPlanElements().size(); i++) {
				if (getPlanElements().get(i).equals(o)) {
					return i;
				}
			}
			return -1;
		}
		throw new IllegalArgumentException("Method call only valid with a Leg or Act instance as parameter!");
	}

	public Activity getFirstActivity() {
		return (Activity) getPlanElements().get(0);
	}

	public Activity getLastActivity() {
		return (Activity) getPlanElements().get(getPlanElements().size() - 1);
	}

	public final double getScoreAsPrimitiveType() {
		if (getScore() == null) {
			return Plan.UNDEF_SCORE;
		}
		return getScore().doubleValue();
	}

	public void addAct(BasicActivity act) {
		delegate.addAct(act);
	}

	public void addLeg(BasicLeg leg) {
		delegate.addLeg(leg);
	}

	public boolean containsActivity(String activityType) {
		return delegate.containsActivity(activityType);
	}

	public ActIterator getIteratorAct() {
		return delegate.getIteratorAct();
	}

	public LegIterator getIteratorLeg() {
		return delegate.getIteratorLeg();
	}

	public final Double getScore() {
		return delegate.getScore();
	}

	public Type getType() {
		return delegate.getType();
	}

	public void setScore(Double score) {
		delegate.setScore(score);
	}

	public void setType(Type type) {
		delegate.setType(type);
	}
	
}
