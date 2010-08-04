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
import org.matsim.core.gbl.Gbl;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.PersonImpl;
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
		PersonImpl person = (PersonImpl) pp;
		double rd3 = MatsimRandom.getRandom().nextDouble();
		if (person.isEmployed() == null) {
			; // what happens if it is "null"?
		} else if (!person.isEmployed()) {
			if (person.getCarAvail() == "always") {
				if (rd3 < 0.1) {
					person.addTravelcard("HT");
				}
			}
			else if (person.getCarAvail() == "sometimes") {
				if (rd3 < 0.6) {
					person.addTravelcard("HT");
				}
				else if (rd3 < 0.7) {
					person.addTravelcard("GA");
				}
				else {
					; // no travel card will be assigned
				}
			}
			else if (person.getCarAvail() == "never") {
				if (rd3 < 0.2) {
					person.addTravelcard("HT");
				}
				else if (rd3 < 0.5) {
					person.addTravelcard("GA");
				}
				else {
					; // no travel card will be assigned
				}
			}
			else {
				Gbl.errorMsg("do not know car avail = " + person.getCarAvail());
			}
		}
		else if (person.isEmployed()) {
			if (person.getCarAvail() == "always") {
				if (rd3 < 0.1) {
					person.addTravelcard("HT");
				}
			}
			else if (person.getCarAvail() == "sometimes") {
				if (rd3 < 0.7) {
					person.addTravelcard("HT");
				}
				else {
					person.addTravelcard("GA");
				}
			}
			else if (person.getCarAvail() == "never") {
				if (rd3 < 0.5) {
					person.addTravelcard("HT");
				}
				else {
					person.addTravelcard("GA");
				}
			}
			else {
				Gbl.errorMsg("do not know car avail = " + person.getCarAvail());
			}
		}
	}
}