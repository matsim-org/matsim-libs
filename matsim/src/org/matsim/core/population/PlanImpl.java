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

import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.basic.v01.Coord;
import org.matsim.api.basic.v01.TransportMode;
import org.matsim.api.basic.v01.population.BasicActivity;
import org.matsim.api.basic.v01.population.BasicLeg;
import org.matsim.core.api.experimental.population.Plan;
import org.matsim.core.api.experimental.population.PlanElement;
import org.matsim.core.api.population.Route;
import org.matsim.core.basic.v01.BasicPlanImpl;
import org.matsim.core.facilities.ActivityFacility;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.routes.GenericRoute;
import org.matsim.core.population.routes.GenericRouteImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.utils.misc.Time;

public class PlanImpl implements Plan { //zzzz would be better with inheritance because of protected c'tor of BasicPlanImpl, but BasicPlanImpl.getPlanElements() is read only
	/**
	 * @deprecated use Leg.Mode instead
	 */
	@Deprecated
	public enum Type { CAR, PT, RIDE, BIKE, WALK, UNDEFINED}
	
	/**
	 * Constant describing the score of an unscored plan. <b>Do not use this constant in
	 * comparisons</b>, but use <code>getScore() == null</code>
	 * instead to test if a plan has an undefined score.
	 */
	@Deprecated
	public static final double UNDEF_SCORE = Double.NaN;

	private final BasicPlanImpl delegate;
	
	private final static Logger log = Logger.getLogger(PlanImpl.class);

	private final static String ACT_ERROR = "The order of 'acts'/'legs' is wrong in some way while trying to create an 'act'.";

	public PlanImpl(final PersonImpl person) {
		this.delegate = new BasicPlanImpl(person);
	}

	public final ActivityImpl createActivity(final String type, final Coord coord) {
		verifyCreateAct();
		ActivityImpl a = new ActivityImpl(type, coord);
		getPlanElements().add(a);
		return a;
	}

	public final ActivityImpl createActivity(final String type, final ActivityFacility fac) {
		verifyCreateAct();
		ActivityImpl a = new ActivityImpl(type, fac);
		getPlanElements().add(a);
		return a;
	}


	public final ActivityImpl createActivity(final String type, final LinkImpl link) {
		verifyCreateAct();
		ActivityImpl a = new ActivityImpl(type, link);
		getPlanElements().add(a);
		return a;
	}

	//////////////////////////////////////////////////////////////////////
	// create methods
	//////////////////////////////////////////////////////////////////////

	public LegImpl createLeg(final TransportMode mode) {
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

	//////////////////////////////////////////////////////////////////////
	// get methods
	//////////////////////////////////////////////////////////////////////

	@SuppressWarnings("unchecked")
	public final List<PlanElement> getPlanElements() {
		return (List<PlanElement>) this.delegate.getPlanElements();
	}

	public final PersonImpl getPerson() {
		return (PersonImpl) this.delegate.getPerson();
	}

	public void setPerson(final PersonImpl person) {
		this.delegate.setPerson(person);
	}

	public final boolean isSelected() {
		return this.getPerson().getSelectedPlan() == this;
	}
	

	public void setSelected(final boolean selected) {
		this.getPerson().setSelectedPlan(this); 
//		this.delegate.setSelected(selected);
	}

	@Override
	public final String toString() {
		return "[score=" + this.getScore().toString() + "]" +
				"[selected=" + this.isSelected() + "]" +
				"[nof_acts_legs=" + getPlanElements().size() + "]";
	}

	/** loads a copy of an existing plan, but keeps the person reference
	 * @param in a plan who's data will be loaded into this plan
	 **/
	public void copyPlan(final PlanImpl in) {
		// TODO should be re-implemented making use of Cloneable
		setScore(in.getScore());
		this.setType(in.getType());
//		setPerson(in.getPerson()); // do not copy person, but keep the person we're assigned to
		for (PlanElement pe : in.getPlanElements()) {
			if (pe instanceof ActivityImpl) {
				ActivityImpl a = (ActivityImpl) pe;
				getPlanElements().add(new ActivityImpl(a));
			} else if (pe instanceof LegImpl) {
				LegImpl l = (LegImpl) pe;
				LegImpl l2 = createLeg(l.getMode());
				l2.setDepartureTime(l.getDepartureTime());
				l2.setTravelTime(l.getTravelTime());
				l2.setArrivalTime(l.getArrivalTime());
				Route route = l.getRoute();
				if (route != null) {
					if (route instanceof NetworkRoute) {
						NetworkLayer net = (NetworkLayer) route.getStartLink().getLayer();
						NetworkRoute r2 = (NetworkRoute) net.getFactory().createRoute(TransportMode.car, route.getStartLink(), route.getEndLink());
						r2.setLinks(route.getStartLink(), ((NetworkRoute) route).getLinks(), route.getEndLink());
						r2.setDistance(route.getDistance());
						r2.setTravelTime(route.getTravelTime());
						l2.setRoute(r2);
					} else if (route instanceof GenericRoute) {
						GenericRoute r = new GenericRouteImpl(route.getStartLink(), route.getEndLink());
						r.setRouteDescription(route.getStartLink(), ((GenericRoute) route).getRouteDescription(), route.getEndLink());
						r.setDistance(route.getDistance());
						r.setTravelTime(route.getTravelTime());
						l2.setRoute(r);
					} else {
						log.warn("could not fully copy plan to agent " + this.getPerson().getId() + " because of unknown Route-type.");
					}
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

	public LegImpl getPreviousLeg(final ActivityImpl act) {
		int index = this.getActLegIndex(act);
		if (index != -1) {
			return (LegImpl) getPlanElements().get(index-1);
		}
		return null;
	}

	public ActivityImpl getPreviousActivity(final LegImpl leg) {
		int index = this.getActLegIndex(leg);
		if (index != -1) {
			return (ActivityImpl) getPlanElements().get(index-1);
		}
		return null;
	}

	public LegImpl getNextLeg(final ActivityImpl act) {
		int index = this.getActLegIndex(act);
		if ((index < getPlanElements().size() - 1) && (index != -1)) {
			return (LegImpl) getPlanElements().get(index+1);
		}
		return null;
	}

	public ActivityImpl getNextActivity(final LegImpl leg) {
		int index = this.getActLegIndex(leg);
		if (index != -1) {
			return (ActivityImpl) getPlanElements().get(index+1);
		}
		return null;
	}

	private int getActLegIndex(final Object o) {
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

	public void addActivity(BasicActivity act) {
		delegate.addActivity(act);
	}

	public void addLeg(BasicLeg leg) {
		delegate.addLeg(leg);
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
