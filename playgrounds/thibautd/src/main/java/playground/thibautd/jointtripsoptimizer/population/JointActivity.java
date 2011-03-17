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

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

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
	private static final Logger log =
		Logger.getLogger(JointActivity.class);



	private boolean isJoint = false;
	//private List<Person> participants = null;
	private Map<Id, JointActivity> linkedActivities = new HashMap<Id, JointActivity>();
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

	@SuppressWarnings("unchecked")
	private void constructFromJointActivity(JointActivity act) {
		this.isJoint = act.getJoint();
		// cast leaved unchecked as elements linked to a joint activity must be
		// a joint activity
		// FIXME: when creating a new joint plan from an old one, linked
		// elements still reference the ones of the old plan!
		this.linkedActivities = (Map<Id,JointActivity>) act.getLinkedElements();
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

	@SuppressWarnings("unchecked")
	@Override
	public void setLinkedElements(Map<Id, ? extends JointActing> linkedElements) {
		//unchecked cast, as elements linked to a joint activity must be a joint activity.
		this.linkedActivities = (Map<Id, JointActivity>) linkedElements;
		if (this.linkedActivities.size()>1) {
			this.isJoint = true;
		}
	}

	@Override
	public void addLinkedElement(Id id, JointActing act) {
		//TODO: check cast
		this.linkedActivities.put(id, (JointActivity) act);
		if ((this.isJoint==false)&&(this.linkedActivities.size()>1)) {
			this.isJoint = true;
		}
	}

	/**
	 * based on the assumption that there is max one linked elements per other
	 * member.
	 */
	@Override
	public void removeLinkedElement(Id id) {
		//TODO: check cast and success
		this.linkedActivities.remove(id);
		if (this.linkedActivities.size() <= 1) {
			this.isJoint = false;
		}
	}

	@Override
	public Map<Id, ? extends JointActing> getLinkedElements() {
		return this.linkedActivities;
	}

	@Override
	public Person getPerson() {
		return this.person;
	}
	
	@Override
	public void setPerson(Person person) {
		this.person = person;
	}

	@Deprecated
	protected Activity getDelegate() {
		return (ActivityImpl) this;
	}

}
