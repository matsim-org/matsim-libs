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
	// must extend LegImpl, as there exist parts of the code (mobsim...) where
	// legs are casted to LegImpl.
	//private Leg legDelegate;

	private boolean isJoint = false;
	private boolean isDriver = false;
	//private List<PersonInClique> participants = null;
	//private List<PersonImpl> participants = null;
	private Map<Id, JointLeg> linkedLegs = new HashMap<Id, JointLeg>();
	private Person person;
	//is set only if the trip is joint
	private JointLeg associatedIndividualLeg = null;
	
	/*
	 * =========================================================================
	 * Constructors
	 * =========================================================================
	 */

	//LegImpl compatible
	public JointLeg(final String transportMode, final Person person) {
		super(transportMode);
		this.person = person;
	}

	//JointLeg specific
	public JointLeg(final JointLeg leg) {
		super((LegImpl) leg);
		constructFromJointLeg(leg);
	}

	/**
	 * Converts a legImpl instance into an individual JointLeg instance.
	 */
	public JointLeg(LegImpl leg, Person pers) {
		super(leg);
		if (leg instanceof JointLeg) {
			constructFromJointLeg((JointLeg) leg);
		} else {
			this.person = pers;
		}
	}

	public JointLeg(LegImpl leg, JointLeg jointLeg) {
		super(leg);
		constructFromJointLeg((JointLeg) leg);
	}
	//TODO
	/**
	 * Creates a shared JointLeg and sets the "replacement" individual leg.
	 */
	//public JointLeg() {
	//}

	@SuppressWarnings("unchecked")
	private void constructFromJointLeg(JointLeg leg) {
		this.isJoint = leg.getJoint();
		//cast unchecked, as it is the only possible output from getLinkedElements
		this.linkedLegs = new HashMap<Id, JointLeg>(
				(Map<Id, JointLeg>) leg.getLinkedElements());
		this.isDriver = leg.getIsDriver();
		this.associatedIndividualLeg = leg.getAssociatedIndividualLeg();
		this.person = leg.getPerson();
	}

	/*
	 * =========================================================================
	 * JointActing Methods
	 * =========================================================================
	 */

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

	/*
	 * =========================================================================
	 * miscelaneous
	 * =========================================================================
	 */

	/**
	 * For shared rides, returns a default individual leg.
	 * Used in the optimisation, to allow quick affectation/desaffectation of
	 * shared rides.
	 * All trips associated to a shared ride (Act-PU, PU-DO, DO-Act) should return
	 * the same leg.
	 * @todo: make compatible with sequence optimisation
	 */
	public JointLeg getAssociatedIndividualLeg() {
		return this.associatedIndividualLeg;
	}

	public boolean getIsDriver() {
		return this.isDriver;
	}

	public void setIsDriver(boolean isDriver) {
		this.isDriver = isDriver;
	}
}

