/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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
package playground.agarwalamit.analysis.activity;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.utils.collections.Tuple;

/**
 * @author amit
 */
public class PersonActivityInfo  {

	private Id<Person> personId;
	private final List<Tuple<String,Double>> actType2StartTimes; 
	private final List<Tuple<String,Double>> actType2EndTimes; 
	
	public PersonActivityInfo(final Id<Person> personId) {
		this.personId = personId;
		actType2EndTimes = new ArrayList<>();
		actType2StartTimes = new ArrayList<>();
	}

	public Id<Person> getPersonId() {
		return personId;
	}

	public void setPersonId(final Id<Person> personId) {
		this.personId = personId;
	}

	public List<Tuple<String, Double>> getActType2StartTimes() {
		return actType2StartTimes;
	}

	public List<Tuple<String, Double>> getActType2EndTimes() {
		return actType2EndTimes;
	}
}