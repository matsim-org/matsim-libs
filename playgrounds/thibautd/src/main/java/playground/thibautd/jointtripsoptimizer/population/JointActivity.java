/* *********************************************************************** *
 * project: org.matsim.*
 * JointActImpl.java
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

import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;

import org.matsim.core.population.ActivityImpl;

/**
 *
 * @author thibautd
 */
public class JointActivity extends ActivityImpl implements Activity, JointActing {
	// must extend ActivityImpl, as there exist parts of the code (mobsim...) where
	// Activities are casted to ActivityImpl.

	// joint activity currently unsupported
	private final boolean isJoint = false;
	//private List<Person> participants = null;
	//private Map<Id, JointActivity> linkedActivities = new HashMap<Id, JointActivity>();
	private Person person;

	/*
	 * =========================================================================
	 * Constructors
	 * =========================================================================
	 */
	public JointActivity(final String type, final Id linkId, final Person person) {
		super(type, linkId);
		this.person = person;
	}

	public JointActivity(final String type, final Coord coord, final Person person) {
		super(type, coord);
		this.person = person;
	}

	public JointActivity(final String type, final Coord coord, final Id linkId, final Person person) {
		super(type, coord, linkId);
		this.person = person;
	}

	public JointActivity(final ActivityImpl act, final Person person) {
		super(act);
		this.person = person;
	}

	public JointActivity(final JointActivity act) {
		super((ActivityImpl) act);
		constructFromJointActivity(act);
	}

	public JointActivity(Activity act, Person pers) {
		super((ActivityImpl) act);
		if (act instanceof JointActivity) {
			constructFromJointActivity((JointActivity) act);
		} else if (act instanceof ActivityImpl) {
			this.person = pers;
		} 
	}

	private void constructFromJointActivity(final JointActivity act) {
		this.person = act.getPerson();
	}

	/*
	 * =========================================================================
	 * JointActing-specific methods
	 * =========================================================================
	 */

	@Override
	public boolean getJoint() {
		return this.isJoint;
	}

	@Override
	public void setLinkedElements(final Map<Id, ? extends JointActing> linkedElements) {
		this.linkageError();
	}

	@Override
	public void addLinkedElement(
			final Id id,
			final JointActing act) {
		this.linkageError();
	}

	@Override
	public Map<Id, ? extends JointActing> getLinkedElements() {
		this.linkageError();
		return null;
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
	public void setLinkedElementsById(final List<? extends Id> linkedElements) {
		this.linkageError();
	}

	@Override
	public void addLinkedElementById(final Id linkedElement) {
		this.linkageError();
	}

	@Override
	public List<? extends Id> getLinkedElementsIds() {
		return null;
	}

	private void linkageError() {
		throw new UnsupportedOperationException("linkage of activities not supported yet");
	}

	@Deprecated
	protected Activity getDelegate() {
		return (ActivityImpl) this;
	}

}
