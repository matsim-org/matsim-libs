/* *********************************************************************** *
 * project: org.matsim.*
 * JointLegImpl.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package playground.thibautd.jointtripsoptimizer.population;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Identifiable;

import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Route;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.routes.AbstractRoute;

/**
 * @author thibautd
 */
public class JointLeg extends LegImpl implements Leg, JointActing, Identifiable {
	private static final Logger log =
		Logger.getLogger(JointLeg.class);

	// must extend LegImpl, as there exist parts of the code (mobsim...) where
	// legs are casted to LegImpl.
	//private Leg legDelegate;
	
	/**
	 * Each joint leg created from a constructor without a joint leg as an argument
	 * is attributed a unique Id.
	 */
	private static long currentLegId = 0;
	private final IdLeg legId;

	/**
	 * to indicate when it is safe to copy the route of the driver leg from
	 * passenger legs.
	 */
	//TODO: find a more parcimonious way of achieving this.
	private boolean routeToCopy = false;
	
	private final List<IdLeg> linkedLegsIds = new ArrayList<IdLeg>();
	private JointPlan jointPlan = null;

	private Person person;
	//is set only if the trip is joint
	// private JointLeg associatedIndividualLeg = null;
	
	/*
	 * =========================================================================
	 * Constructors
	 * =========================================================================
	 */

	//LegImpl compatible
	public JointLeg(final String transportMode, final Person person) {
		super(transportMode);
		this.person = person;
		this.legId = createId();
	}

	//JointLeg specific
	public JointLeg(final JointLeg leg) {
		super((LegImpl) leg);
		this.legId = leg.getId();
		constructFromJointLeg(leg);
	}

	/**
	 * Converts a legImpl instance into an individual JointLeg instance.
	 */
	public JointLeg(final Leg leg, final Person pers) {
		super((LegImpl) leg);
		if (leg instanceof JointLeg) {
			this.legId = ((JointLeg) leg).getId();
			constructFromJointLeg((JointLeg) leg);
		} else {
			this.legId = createId();
			this.person = pers;
		}
	}

	/**
	 * "Almost" a copy cosntructor: takes all joint-trip related from a joint
	 * leg, and all other information (mode, route, etc.) from an individual
	 * leg. 
	 * Particularly, the new leg will have the same id as the old one.
	 */
	public JointLeg(final LegImpl leg, final JointLeg jointLeg) {
		super(leg);
		this.legId = jointLeg.getId();
		constructFromJointLeg(jointLeg);
	}

	private void constructFromJointLeg(final JointLeg leg) {
		this.linkedLegsIds.addAll(leg.getLinkedElementsIds());
		//this.isDriver = leg.getIsDriver();
		//this.associatedIndividualLeg = leg.getAssociatedIndividualLeg();
		this.person = leg.getPerson();
	}

	private IdLeg createId() {
		return new IdLeg(currentLegId++);
	}

	/*
	 * ========================================================================
	 * Overriden LegImpl methods (and related helper methods)
	 * ========================================================================
	 */
	/**
	 * Returns the associated route.
	 * In the case of a passenger leg, the route is created at the first call of
	 * this method, by copying the driver's route.
	 * <u>This requires the "routeToCopy" method to be called.</u>
	 * . If the route is not null or the method "routeToCopy" as not been called,
	 * the route will not be changed (this may be wanted: for example, the joint
	 * replanning module already creates consistent routes).
	 */
	@Override
	public Route getRoute() {
		if ( (this.getJoint()) && 
				(!this.getMode().equals(TransportMode.car)) &&
				//(super.getRoute()==null) &&
				this.routeToCopy) {
			//Passenger leg with no route: create it
			Route route=null;
			Leg driverLeg=null;

			for (JointLeg linkedLeg : this.getLinkedElements().values()) {
				if (linkedLeg.getIsDriver()) {
					route = linkedLeg.getRoute();
					driverLeg = linkedLeg;
					break;
				}
			}

			if (route==null) {
				super.setRoute(null);
				return null;
			}

			try {
				route = ((AbstractRoute) route).clone();
				//log.debug("Driver leg has been cloned by passenger leg");
			} catch (ClassCastException e) {
				throw new RuntimeException("JointLeg can currently handle only"+
						" AbstractRoute driver routes.");
			}

			this.routeToCopy = false;
			super.setTravelTime(driverLeg.getTravelTime());
			super.setRoute(route);
			return route;
		}
		return super.getRoute();
	}

	public void routeToCopy() {
		this.routeToCopy = true;
	}

	// impossible: super method is final!
	//@Override
	//public double getTravelTime() {
	//	if (super.getTravelTime() == Time.UNDEFINED_TIME) {
	//		try {
	//			this.setTravelTime(this.getRoute().getTravelTime());
	//		} catch (NullPointerException e) {
	//			// this is normal before simulation: don't worry.
	//		}
	//	}
	//	return super.getTravelTime();
	//}

	/*
	 * =========================================================================
	 * JointActing Methods
	 * =========================================================================
	 */

	/**
	 * Only <u>shared</u> rides are considered as joint (access to PU and return
	 * from DO are part of a Joint Episode, but are <u>not</u> joint).
	 */
	@Override
	public boolean getJoint() {
		return (this.linkedLegsIds.size() > 0);
	}

	@Override
	public void setLinkedElements(
			final Map<Id, ? extends JointActing> linkedElements) {
		try {
			this.linkedLegsIds.clear();
			for (JointActing currentElement : linkedElements.values()) {
				this.linkedLegsIds.add(((JointLeg) currentElement).getId());
			}
		} catch (ClassCastException e) {
			throw new IllegalArgumentException("Elements linked to a JointLeg must"+
					" be of type JointLeg!");
		}
	}

	@Override
	public void addLinkedElement(final Id id, final JointActing act) {
		try {
			this.addLinkedElementById(((JointLeg) act).getId());
		} catch (ClassCastException e) {
			throw new IllegalArgumentException("Elements linked to a JointLeg must"+
					" be of type JointLeg!");
		}
	}

	@Override
	public Map<Id, JointLeg> getLinkedElements() {
		Map<Id, JointLeg> output = new HashMap<Id, JointLeg>();
		JointLeg currentLeg;

		for (IdLeg currentIdLeg : this.linkedLegsIds) {
			currentLeg = this.jointPlan.getLegById(currentIdLeg);
			output.put(currentLeg.getPerson().getId(), currentLeg);
		}

		return output;
	}

	/**
	 * {@inheritDoc}
	 * @see JointActing#setLinkedElementsById(List) JointActing.setLinkedElementsById(List<Id>)
	 */
	@Override
	public void setLinkedElementsById(final List<? extends Id> linkedElements) {
		try {
			this.linkedLegsIds.clear();
			for (Id linkedId : linkedElements) {
				this.linkedLegsIds.add((IdLeg) linkedId);
			}
		} catch (ClassCastException e) {
			throw new IllegalArgumentException("ids linked to a joint leg must "+
					"be of type IdLeg!");
		}
	}

	/**
	 * {@inheritDoc}
	 * @see JointActing#addLinkedElementById(Id) JointActing.addLinkedElementById(Id)
	 */
	@Override
	public void addLinkedElementById(final Id linkedId) {
		try {
			this.linkedLegsIds.add((IdLeg) linkedId);
		} catch (ClassCastException e) {
			throw new IllegalArgumentException("ids linked to a joint leg must "+
					"be of type IdLeg!");
		}
	}

	/**
	 * {@inheritDoc}
	 * @see JointActing#getLinkedElementsIds() JointActing.getLinkedElementsIds()
	 */
	@Override
	public List<IdLeg> getLinkedElementsIds() {
		return this.linkedLegsIds;
	}

	/**
	 * for use when adding to a joint plan.
	 * Necessary to resolve the linked elements in a way robust with copy.
	 */
	/*package*/ void setJointPlan(final JointPlan plan) {
		this.jointPlan = plan;
	}

	@Override
	public Person getPerson() {
		return this.person;
	}
	
	@Override
	public void setPerson(final Person person) {
		this.person = person;
	}

	@Override
	public IdLeg getId() {
		return this.legId;
	}

	/*
	 * =========================================================================
	 * miscelaneous
	 * =========================================================================
	 */

	public boolean getIsDriver() {
		//return this.isDriver;
		return this.getJoint() && this.getMode().equals(TransportMode.car);
	}

	@Deprecated
	public void setIsDriver(final boolean isDriver) {
		//log.warn("deprecated method JointLeg.setIsDriver called. This method "+
		//		"does not do anything");
	}
}

