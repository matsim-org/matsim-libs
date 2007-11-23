/* *********************************************************************** *
 * project: org.matsim.*
 * PlansCreatePopulation.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package playground.balmermi.census2000.modules;

import java.util.Iterator;

import org.matsim.basic.v01.Id;
import org.matsim.gbl.Gbl;
import org.matsim.plans.Person;
import org.matsim.plans.Plans;
import org.matsim.plans.algorithms.PlansAlgorithm;

import playground.balmermi.census2000.data.Persons;

public class PlansCreatePopulation extends PlansAlgorithm {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private final Persons persons;
	
	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public PlansCreatePopulation(Persons persons) {
		super();
		System.out.println("    init " + this.getClass().getName() + " module...");
		this.persons = persons;
		System.out.println("    done.");
	}

	//////////////////////////////////////////////////////////////////////
	// run methods
	//////////////////////////////////////////////////////////////////////

	@Override
	public void run(final Plans plans) {
		System.out.println("    running " + this.getClass().getName() + " algorithm...");

		if (plans.getName() == null) {
			plans.setName("created by '" + this.getClass().getName() + "'");
		}
		if (!plans.getPersons().isEmpty()) {
			Gbl.errorMsg("[plans=" + plans + " is not empty]");
		}

		Iterator<Integer> pid_it = this.persons.getPersons().keySet().iterator();
		while (pid_it.hasNext()) {
			Integer pid = pid_it.next();
			playground.balmermi.census2000.data.Person p = this.persons.getPersons().get(pid);
			Id id = new Id(pid);
			Person person = new Person(id,p.getSex(),p.getAge(),p.getLicense(),p.getCarAvail(),p.getEmployed());
			try {
				plans.addPerson(person);
			} catch (Exception e) {
				e.printStackTrace();
				System.exit(-1);
			}
		}
		System.out.println("    done.");
	}

	//////////////////////////////////////////////////////////////////////
	// print methods
	//////////////////////////////////////////////////////////////////////
}
