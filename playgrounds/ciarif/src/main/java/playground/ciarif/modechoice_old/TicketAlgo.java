/* *********************************************************************** *
 * project: org.matsim.*
 * TicketAlgo.java
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

package playground.ciarif.modechoice_old;



import org.matsim.api.core.v01.population.Person;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.PersonUtils;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;

public class TicketAlgo extends AbstractPersonAlgorithm {

	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public TicketAlgo() {
		super();
	}

	//////////////////////////////////////////////////////////////////////
	// run methods
	//////////////////////////////////////////////////////////////////////


	@Override
	public void run(Person pp) {
		Person person = pp;
		double rd3 = MatsimRandom.getRandom().nextDouble();
		if (PersonUtils.isEmployed(person) == null) {
			; // what happens if it is "null"?
		} else if (!PersonUtils.isEmployed(person)) {
			if (PersonUtils.getCarAvail(person) == "always") {
				if (rd3 < 0.1) {
					PersonUtils.addTravelcard(person, "HT");
				}
			}
			else if (PersonUtils.getCarAvail(person) == "sometimes") {
				if (rd3 < 0.6) {
					PersonUtils.addTravelcard(person, "HT");
				}
				else if (rd3 < 0.7) {
					PersonUtils.addTravelcard(person, "GA");
				}
				else {
					; // no travel card will be assigned
				}
			}
			else if (PersonUtils.getCarAvail(person) == "never") {
				if (rd3 < 0.2) {
					PersonUtils.addTravelcard(person, "HT");
				}
				else if (rd3 < 0.5) {
					PersonUtils.addTravelcard(person, "GA");
				}
				else {
					; // no travel card will be assigned
				}
			}
			else {
				throw new RuntimeException("do not know car avail = " + PersonUtils.getCarAvail(person));
			}
		}
		else if (PersonUtils.isEmployed(person)) {
			if (PersonUtils.getCarAvail(person) == "always") {
				if (rd3 < 0.1) {
					PersonUtils.addTravelcard(person, "HT");
				}
			}
			else if (PersonUtils.getCarAvail(person) == "sometimes") {
				if (rd3 < 0.7) {
					PersonUtils.addTravelcard(person, "HT");
				}
				else {
					PersonUtils.addTravelcard(person, "GA");
				}
			}
			else if (PersonUtils.getCarAvail(person) == "never") {
				if (rd3 < 0.5) {
					PersonUtils.addTravelcard(person, "HT");
				}
				else {
					PersonUtils.addTravelcard(person, "GA");
				}
			}
			else {
				throw new RuntimeException("do not know car avail = " + PersonUtils.getCarAvail(person));
			}
		}
	}
}