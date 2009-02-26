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

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.basic.v01.BasicPlanImpl;
import org.matsim.facilities.Facility;
import org.matsim.gbl.Gbl;
import org.matsim.interfaces.basic.v01.BasicLeg;
import org.matsim.interfaces.basic.v01.Coord;
import org.matsim.interfaces.core.v01.Act;
import org.matsim.interfaces.core.v01.CarRoute;
import org.matsim.interfaces.core.v01.Leg;
import org.matsim.interfaces.core.v01.Link;
import org.matsim.interfaces.core.v01.Person;
import org.matsim.interfaces.core.v01.Plan;
import org.matsim.population.routes.NodeCarRoute;
import org.matsim.utils.misc.Time;

public class PlanImpl extends BasicPlanImpl implements Plan {

	private final static Logger log = Logger.getLogger(PlanImpl.class);

	private final static String ACT_ERROR = "The order of 'acts'/'legs' is wrong in some way while trying to create an 'act'.";

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private Person person = null;

	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public PlanImpl(final Person person) {
		this.person = person;
	}

	public static Plan createPlan( final Person p) {
		return new PlanImpl(p) ;
	}

	public final Act createAct(final String type, final Coord coord) throws IllegalStateException {
		verifyCreateAct(Time.UNDEFINED_TIME);
		Act a = new ActImpl(type, coord);
		this.actsLegs.add(a);
		return a;
	}

	public final Act createAct(final String type, final Facility fac) throws IllegalStateException {
		verifyCreateAct(Time.UNDEFINED_TIME);
		Act a = new ActImpl(type, fac);
		this.actsLegs.add(a);
		return a;
	}


	public final Act createAct(final String type, final Link link) throws IllegalStateException {
		verifyCreateAct(Time.UNDEFINED_TIME);
		Act a = new ActImpl(type, link);
		this.actsLegs.add(a);
		return a;
	}

	//////////////////////////////////////////////////////////////////////
	// create methods
	//////////////////////////////////////////////////////////////////////

	public Leg createLeg(final BasicLeg.Mode mode) throws IllegalStateException {
		verifyCreateLeg();
		Leg leg = new LegImpl(mode);
		// Override leg number with an appropriate value
		leg.setNum((this.actsLegs.size()-1) /2);
		this.actsLegs.add(leg);
		return leg;
	}

	private final void verifyCreateLeg() throws IllegalStateException {
		if (this.actsLegs.size() % 2 == 0) {
			throw new IllegalStateException("The order of 'acts'/'legs' is wrong in some way while trying to create a 'leg'.");
		}
	}

	private static long noEndTimeCnt = 0 ;
	private final void verifyCreateAct(final double endTime) throws IllegalStateException {
		if (this.actsLegs.size() % 2 != 0) {
			throw new IllegalStateException(ACT_ERROR);
		}
		if ((this.actsLegs.size() == 0) && (endTime == Time.UNDEFINED_TIME)) {
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
		if ((index % 2 != 0) || (index < 0) || (index > this.actsLegs.size()-1)) {
			log.warn(this + "[index=" + index +" is wrong. nothing removed]");
		}
		else if (this.actsLegs.size() == 1) {
			log.warn(this + "[index=" + index +" only one act. nothing removed]");
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
				Leg prev_leg = (Leg)this.actsLegs.get(index-1); // prev leg;
				prev_leg.setDepartureTime(Time.UNDEFINED_TIME);
				prev_leg.setTravelTime(Time.UNDEFINED_TIME);
				prev_leg.setArrivalTime(Time.UNDEFINED_TIME);
				prev_leg.setRoute(null);

				this.actsLegs.remove(index+1); // following leg
				this.actsLegs.remove(index); // act
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
		if ((index % 2 == 0) || (index < 1) || (index >= this.actsLegs.size()-1)) {
			log.warn(this + "[index=" + index +" is wrong. nothing removed]");
		}
		else {
			if (index != this.actsLegs.size()-2) {
				// not the last leg
				Leg next_leg = (Leg)this.actsLegs.get(index+2);
				next_leg.setDepartureTime(Time.UNDEFINED_TIME);
				next_leg.setTravelTime(Time.UNDEFINED_TIME);
				next_leg.setArrivalTime(Time.UNDEFINED_TIME);
				next_leg.setRoute(null);
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

	public final Person getPerson() {
		return this.person;
	}

	//////////////////////////////////////////////////////////////////////
	// set methods
	//////////////////////////////////////////////////////////////////////

	public void setPerson(final Person person) {
		this.person = person;
	}

	//////////////////////////////////////////////////////////////////////
	// query methods
	//////////////////////////////////////////////////////////////////////
	@Override
	public final boolean isSelected() {
		return this.person.getSelectedPlan() == this;
	}


	@Override
	public void setSelected(final boolean selected) {
		this.getPerson().setSelectedPlan(this);
		super.setSelected(selected);
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

	/** loads a copy of an existing plan
	 * @param in a plan who's data will be loaded into this plan
	 **/
	public void copyPlan(final Plan in) {
		setScore(in.getScore());
		this.setType(in.getType());
		this.person = in.getPerson();
		List<?> actl = in.getActsLegs();
		for (int i= 0; i< actl.size() ; i++) {
			try {
				if (i % 2 == 0) {
					// activity
					Act a = (Act)actl.get(i);
					this.actsLegs.add(new ActImpl(a));
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
	public void insertLegAct(final int pos, final Leg leg, final Act act) throws IllegalArgumentException {
		if (pos < this.actsLegs.size()) {
			Object o = this.actsLegs.get(pos);
			if (!(o instanceof Leg)) {
				throw new IllegalArgumentException("Position to insert leg and act is not valid (act instead of leg at position).");
			}
		} else if (pos > this.actsLegs.size()) {
			throw new IllegalArgumentException("Position to insert leg and act is not valid.");
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

	/**
	 * Returns the leg following the specified act. <b>Important Note: </b> This method (together with
	 * {@link #getNextActivity(Leg)}) has a very bad performance if it is used to iterate over all Acts and
	 * Legs of a plan. In that case, it is advised to use one of the special iterators.
	 *
	 * @param act
	 * @return The Leg following <tt>act</tt> in the plan, null if <tt>act</tt> is the last Act in the plan.
	 *
	 * @see #getIterator()
	 * @see #getIteratorAct()
	 * @see #getIteratorLeg()
	 */
	public Leg getNextLeg(final Act act) {
		int index = this.getActLegIndex(act);
		if ((index < this.actsLegs.size() - 1) && (index != -1)) {
			return (Leg) this.actsLegs.get(index+1);
		}
		return null;
	}

	/**
	 * Returns the activity following the specified leg. <b>Important Note: </b> This method (together with
	 * {@link #getNextLeg(Act)}) has a very bad performance if it is used to iterate over all Acts and Legs of
	 * a plan. In that case, it is advised to use one of the special iterators.
	 *
	 * @param leg
	 * @return The Act following <tt>leg</tt> in the plan.
	 *
	 * @see #getIterator()
	 * @see #getIteratorAct()
	 * @see #getIteratorLeg()
	 */
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
		throw new IllegalArgumentException("Method call only valid with a Leg or Act instance as parameter!");
	}

	public Act getFirstActivity() {
		return (Act) this.actsLegs.get(0);
	}

	public Act getLastActivity() {
		return (Act) this.actsLegs.get(this.actsLegs.size() - 1);
	}

}
