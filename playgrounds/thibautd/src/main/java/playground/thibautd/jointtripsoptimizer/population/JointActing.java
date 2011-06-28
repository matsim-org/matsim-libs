/* *********************************************************************** *
 * project: org.matsim.*
 * JointAct.java
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

import org.matsim.api.core.v01.Id;

import org.matsim.api.core.v01.population.Person;

/**
 * @author thibautd
 */
public interface JointActing {

	public boolean getJoint();

	// better to define in terms of linked activities than in terms of
	// participants
	// public void setParticipants(List<? extends Person> participants);
	// public void addParticipant(Person participant);
	// public void removeParticipant(Person participant);
	// public List<? extends Person> getParticipants();
	
	// first API. Linking with objects can lead to wrong references when creating
	// a plan copy, and using co-participants Id is less useful than expected.
	// Modified so as to work, but prefer the "Id" API.
	public void setLinkedElements(Map<Id, ? extends JointActing> linkedElements);
	public void addLinkedElement(Id id, JointActing act);
	// confusing when act/legs are also identified
	//public void removeLinkedElement(Id id);
	public Map<Id, ? extends JointActing> getLinkedElements();

	public void setLinkedElementsById(List<? extends Id> linkedElements);
	public void addLinkedElementById(Id linkedElement);
	public List<? extends Id> getLinkedElementsIds();

	// in the context of a joint plan, it is useful to associate activities with
	// a person as well.
	public Person getPerson();
	public void setPerson(Person person);
}

