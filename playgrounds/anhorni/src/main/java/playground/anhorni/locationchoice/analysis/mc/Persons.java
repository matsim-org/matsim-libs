/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.anhorni.locationchoice.analysis.mc;

import java.util.TreeMap;

import org.matsim.api.core.v01.Id;

public class Persons {
	
	private TreeMap<Id, PersonTripActs> persons = new TreeMap<Id, PersonTripActs>();
	
	public void addTrip(Id id, MZTrip mzTrip) {
		if (!this.persons.containsKey(id)) {
			PersonTripActs personTripActs = new PersonTripActs(id);
			personTripActs.addTrip(mzTrip);
			this.persons.put(id, personTripActs);
		}
		else {
			this.persons.get(id).addTrip(mzTrip);
		}
	}
	
	public int getNumberOfPersons() {
		return this.persons.size();
	}

	public TreeMap<Id, PersonTripActs> getPersons() {
		return persons;
	}

	public void setPersons(TreeMap<Id, PersonTripActs> persons) {
		this.persons = persons;
	}
}
