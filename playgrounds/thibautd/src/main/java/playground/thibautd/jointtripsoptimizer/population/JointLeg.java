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

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Identifiable;

import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.population.LegImpl;

/**
 * @author thibautd
 */
public class JointLeg extends LegImpl implements Leg, JointActing, Identifiable {
	// must extend LegImpl, as there exist parts of the code (mobsim...) where
	// legs are casted to LegImpl.
	//private Leg legDelegate;
	
	/**
	 * Each joint leg created from a constructor without a joint leg as an argument
	 * is attributed a unique Id.
	 */
	private static long currentLegId = 0;
	private final IdLeg legId;

	private boolean isDriver = false;
	
	private final List<IdLeg> linkedLegsIds = new ArrayList<IdLeg>();
	private JointPlan jointPlan = null;

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
	public JointLeg(LegImpl leg, Person pers) {
		super(leg);
		if (leg instanceof JointLeg) {
			this.legId = ((JointLeg) leg).getId();
			constructFromJointLeg((JointLeg) leg);
		} else {
			this.legId = createId();
			this.person = pers;
		}
	}

	public JointLeg(LegImpl leg, JointLeg jointLeg) {
		super(leg);
		this.legId = jointLeg.getId();
		constructFromJointLeg(jointLeg);
	}
	//TODO
	/**
	 * Creates a shared JointLeg and sets the "replacement" individual leg.
	 */
	//public JointLeg() {
	//}

	private void constructFromJointLeg(JointLeg leg) {
		this.linkedLegsIds.addAll(leg.getLinkedElementsIds());
		this.isDriver = leg.getIsDriver();
		this.associatedIndividualLeg = leg.getAssociatedIndividualLeg();
		this.person = leg.getPerson();
	}

	private IdLeg createId() {
		currentLegId++;
		return new IdLeg(currentLegId);
	}

	/*
	 * =========================================================================
	 * JointActing Methods
	 * =========================================================================
	 */

	/**
	 * Only <u>shared</u> rides are considered as joint (access to PU and return
	 * from DO are part of a Joint Episode, but are not joint).
	 */
	@Override
	public boolean getJoint() {
		return (this.linkedLegsIds.size() > 0);
	}

	@Override
	public void setLinkedElements(Map<Id, ? extends JointActing> linkedElements) {
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
	public void addLinkedElement(Id id, JointActing act) {
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
	 * @see JointActing#setLinkedElementsById(List<Id>)
	 */
	@Override
	public void setLinkedElementsById(List<? extends Id> linkedElements) {
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
	 * @see JointActing#addLinkedElementsById(List<Id>)
	 */
	@Override
	public void addLinkedElementById(Id linkedId) {
		try {
			this.linkedLegsIds.add((IdLeg) linkedId);
		} catch (ClassCastException e) {
			throw new IllegalArgumentException("ids linked to a joint leg must "+
					"be of type IdLeg!");
		}
	}

	/**
	 * {@inheritDoc}
	 * @see JointActing#getLinkedElementsIds()
	 */
	@Override
	public List<IdLeg> getLinkedElementsIds() {
		return this.linkedLegsIds;
	}

	/**
	 * for use when adding to a joint plan.
	 * Necessary to resolve the linked elements in a way robust with copy.
	 */
	/*package*/ void setJointPlan(JointPlan plan) {
		this.jointPlan = plan;
	}

	@Override
	public Person getPerson() {
		return this.person;
	}
	
	@Override
	public void setPerson(Person person) {
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

