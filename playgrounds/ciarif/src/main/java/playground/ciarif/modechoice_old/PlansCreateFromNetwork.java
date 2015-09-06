/* *********************************************************************** *
 * project: org.matsim.*
 * PlansCreateFromNetwork.java
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

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.algorithms.NetworkSummary;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PersonUtils;

public class PlansCreateFromNetwork {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private final NetworkSummary network_summary;
	private final double p2c_prop;

	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public PlansCreateFromNetwork(final NetworkSummary network_summary, final double p2c_prop) {
		super();
		this.network_summary = network_summary;
		this.p2c_prop = p2c_prop;
	}

	//////////////////////////////////////////////////////////////////////
	// run methods
	//////////////////////////////////////////////////////////////////////

	public void run(final Population plans) {
		System.out.println("    running " + this.getClass().getName() + " algorithm...");

		if (plans.getName() == null) {
			plans.setName("created by '" + this.getClass().getName());
		}
		if (!plans.getPersons().isEmpty()) {
			throw new RuntimeException("[plans=" + plans + " is not empty]");
		}

		int network_capacity = this.network_summary.getNetworkCapacity();
		long nof_persons = Math.round(this.p2c_prop * network_capacity);
		System.out.println("      network_capacity = " + network_capacity + " cells");
		System.out.println("      p2c_proportion   = " + this.p2c_prop);
		System.out.println("      creating " + nof_persons + " persons");
		for (int i=1; i<=nof_persons; i++) {
			String sex = "m";
			if (MatsimRandom.getRandom().nextDouble() < 0.5) {
				sex = "f";
			}
			double rd = MatsimRandom.getRandom().nextDouble();
			String license = "no";
			String car_avail = "never";
			boolean employed = false;
			int age = -1;
			if (rd < 0.05) {
				age = MatsimRandom.getRandom().nextInt(7);
			} else if (rd < 0.2) {
				age = 7 + MatsimRandom.getRandom().nextInt(11);
			} else if (rd < 0.4) {
				age = 18 + MatsimRandom.getRandom().nextInt(10);
				if (MatsimRandom.getRandom().nextDouble() < 0.7) {
					employed = true;
				}
				if (MatsimRandom.getRandom().nextDouble() < 0.5) {
					license = "yes";
					double rd2 = MatsimRandom.getRandom().nextDouble();
					if (rd2 < 0.4) { car_avail = "sometimes"; }
					else if (rd2 < 0.5) { car_avail = "always"; }
				}
			} else if (rd < 0.9) {
				age = 28 + MatsimRandom.getRandom().nextInt(37);
				if (MatsimRandom.getRandom().nextDouble() < 0.6) {
					employed = true;
				}
				if (MatsimRandom.getRandom().nextDouble() < 0.7) {
					license = "yes";
					double rd2 = MatsimRandom.getRandom().nextDouble();
					if (rd2 < 0.2) { car_avail = "sometimes"; }
					else if (rd2 < 0.7) { car_avail = "always"; }
				}
			} else {
				age = 65 + MatsimRandom.getRandom().nextInt(35);
				if (MatsimRandom.getRandom().nextDouble() < 0.4) {
					license = "yes";
					double rd2 = MatsimRandom.getRandom().nextDouble();
					if (rd2 < 0.2) { car_avail = "sometimes"; }
					else if (rd2 < 0.4) { car_avail = "always"; }
				}
			}
			Person p = PersonImpl.createPerson(Id.create(i, Person.class));
			PersonUtils.setSex(p, sex);
			PersonUtils.setAge(p, age);
			PersonUtils.setLicence(p, license);
			PersonUtils.setCarAvail(p, car_avail);
			PersonUtils.setEmployed(p, employed);
			plans.addPerson(p);
		}
		System.out.println("    done.");
	}

}
