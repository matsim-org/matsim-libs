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

package playground.anhorni.surprice.preprocess;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;

public class PersonHomeWork {
	
	Person person; 
	Id homeFacilityId;
	Id workFaciliyId;
	
	public PersonHomeWork(Person person, Id homeFacilityId, Id workFaciliyId) {
		this.person = person;
		this.homeFacilityId = homeFacilityId;
		this.workFaciliyId = workFaciliyId;
	}

	public Person getPerson() {
		return person;
	}

	public void setPerson(Person person) {
		this.person = person;
	}

	public Id getHomeFacilityId() {
		return homeFacilityId;
	}

	public void setHomeFacilityId(Id homeFacilityId) {
		this.homeFacilityId = homeFacilityId;
	}

	public Id getWorkFaciliyId() {
		return workFaciliyId;
	}

	public void setWorkFaciliyId(Id workFaciliyId) {
		this.workFaciliyId = workFaciliyId;
	}
}
