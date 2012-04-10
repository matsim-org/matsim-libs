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

package playground.anhorni.surprice.preprocess.rwscenario;

import java.util.ArrayList;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;

public class PersonWeeks {	
	private Person person;
	private ArrayList<TreeMap<Integer, Plan>> days = new ArrayList<TreeMap<Integer, Plan>>();
	private int currentWeek = - 1;
	
	private final static Logger log = Logger.getLogger(PersonWeeks.class);
	
	public PersonWeeks() {
		for (int i = 0; i < 7; i++) {
			this.days.add(i, new TreeMap<Integer, Plan>());
		}
	}
	
	public void increaseWeek() {
		this.currentWeek++;
	}
	
	public PersonWeeks(Person person) {
		this.person = person;
	}
	
	public Plan getDay(int dow, int week) {
		return this.days.get(week).get(dow - 1);
	}
	
	public void addDay(int dow, Plan plan) {
		if (this.currentWeek < 7) this.days.get(this.currentWeek).put(dow - 1, plan);
	}

	public Person getPerson() {
		return this.person;
	}
}
