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

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;

import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.population.LegImpl;

/**
 * @author thibautd
 */
public class JointLeg extends LegImpl implements Leg, JointActing {
	//private Leg legDelegate;

	private boolean isJoint = false;
	//private List<PersonInClique> participants = null;
	//private List<PersonImpl> participants = null;
	private Map<Id, JointLeg> linkedLegs = new HashMap<Id, JointLeg>();
	private Person person;
	
	/*
	 * =========================================================================
	 * Constructors
	 * =========================================================================
	 */

	//LegImpl compatible
	public JointLeg(final String transportMode, final Person person) {
		//legDelegate = new LegImpl(transportMode);
		super(transportMode);
		this.person = person;
	}

	public JointLeg(final LegImpl leg, final Person person) {
		//legDelegate = new LegImpl(leg);
		super(leg);
		this.person = person;
	}

	//JointLeg specific
	public JointLeg(final JointLeg leg) {
		super((LegImpl) leg);
		constructFromJointLeg(leg);
	}

	public JointLeg(Leg leg, Person pers) {
		super((LegImpl) leg);
		if (leg instanceof JointLeg) {
			constructFromJointLeg((JointLeg) leg);
		} else if (leg instanceof LegImpl) {
			//legDelegate = new LegImpl((LegImpl) leg);
			this.person = pers;
		}// else {
	//		throw new IllegalArgumentException("unrecognized leg type");
	//	}
	}

	@SuppressWarnings("unchecked")
	private void constructFromJointLeg(JointLeg leg) {
		//legDelegate = new LegImpl(leg.getMode());

		//legDelegate.setRoute(leg.getRoute());
		//legDelegate.setDepartureTime(leg.getDepartureTime());
		//legDelegate.setTravelTime(leg.getTravelTime());

		this.isJoint = leg.getJoint();
		//cast unchecked, as it is the only possible output from getLinkedElements
		this.linkedLegs = new HashMap<Id, JointLeg>(
				(Map<Id, JointLeg>) leg.getLinkedElements());

	}

	/*
	 * =========================================================================
	 * Leg Delegate methods
	 * =========================================================================
	 */

	//public String getMode() {
	//	return legDelegate.getMode();
	//}

	//public void setMode(String mode) {
	//	legDelegate.setMode(mode);
	//}

	//public Route getRoute() {
	//	return legDelegate.getRoute();
	//}

	//public void setRoute(Route route) {
	//	legDelegate.setRoute(route);
	//}

	//public double getDepartureTime() {
	//	return legDelegate.getDepartureTime();
	//}

	//public void setDepartureTime(double seconds) {
	//	legDelegate.setDepartureTime(seconds);
	//}

	//public double getTravelTime() {
	//	return legDelegate.getTravelTime();
	//}

	//public void setTravelTime(double seconds) {
	//	legDelegate.setTravelTime(seconds);
	//}

	/*
	 * =========================================================================
	 * JointActing Methods
	 * =========================================================================
	 */

	//@Override
	//public void setJoint(boolean isJoint) {
	//	this.isJoint = isJoint;
	//}

	@Override
	public boolean getJoint() {
		return this.isJoint;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void setLinkedElements(Map<Id, ? extends JointActing> linkedElements) {
		//FIXME: unchecked cast is't safe here
		this.linkedLegs = (Map<Id, JointLeg>) linkedElements;
		if (this.linkedLegs.size()>1) {
			this.isJoint = true;
		}
	}

	@Override
	public void addLinkedElement(Id id, JointActing act) {
		//TODO: check cast
		this.linkedLegs.put(id, (JointLeg) act);
		if ((this.isJoint==false)&&(this.linkedLegs.size()>1)) {
			this.isJoint = true;
		}
	}

	@Override
	public void removeLinkedElement(Id id) {
		//TODO: check cast and success
		this.linkedLegs.remove(id);
		if (this.linkedLegs.size() <= 1) {
			this.isJoint = false;
		}
	}

	@Override
	public Map<Id, ? extends JointActing> getLinkedElements() {
		return this.linkedLegs;
	}

	@Override
	public Person getPerson() {
		return this.person;
	}
	
	@Override
	public void setPerson(Person person) {
		this.person = person;
	}
}

