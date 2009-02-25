/* *********************************************************************** *
 * project: org.matsim.*
 * PlansActChainReduction.java
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

package org.matsim.population.algorithms;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.TreeMap;

import org.matsim.gbl.Gbl;
import org.matsim.interfaces.basic.v01.Id;
import org.matsim.interfaces.core.v01.Act;
import org.matsim.interfaces.core.v01.Person;
import org.matsim.interfaces.core.v01.Plan;
import org.matsim.population.Population;

public class PlansActChainReduction {

	private final double cut;

	public PlansActChainReduction(final double cut) {
		super();
		this.cut = cut;
		if ((this.cut < 0.0) || (this.cut > 1.0)) {
			throw new IllegalArgumentException("cut=" + this.cut + " is not allowed.");
		}
	}

	//////////////////////////////////////////////////////////////////////
	// calc methods
	//////////////////////////////////////////////////////////////////////

	private final TreeMap<String, ArrayList<Person>> calcChainFrequencies(final Population plans) {
		// TreeMap(String chain, ArrayList(Person person))
		TreeMap<String, ArrayList<Person>> chains = new TreeMap<String, ArrayList<Person>>();
		// fill up the chains TreeMap
		Iterator<Person> p_it = plans.getPersons().values().iterator();
		while (p_it.hasNext()) {
			Person p = p_it.next();
			if (p.getPlans().size() != 1) {
				Gbl.errorMsg("[person_id=" + p.getId() + " does not have exactly one plan. not allowed.]");
			}
			String chain = "";
			Plan plan = p.getPlans().get(0);
			for (int i=0; i<plan.getActsLegs().size(); i+=2) {
				Act act = (Act)plan.getActsLegs().get(i);
				chain = chain.concat(act.getType());
			}
			if (!chains.containsKey(chain)) {
				chains.put(chain, new ArrayList<Person>());
			}
			ArrayList<Person> persons = chains.get(chain);
			persons.add(p);
		}
		return chains;
	}

	//////////////////////////////////////////////////////////////////////

	private final TreeMap<Double, ArrayList<String>> calcFractionTreeMap(final TreeMap<String, ArrayList<Person>> chains) {
		// calculate the sum of all chain occurrences
		int sum = 0;
		Iterator<ArrayList<Person>> al_it = chains.values().iterator();
		while (al_it.hasNext()) {
			ArrayList<Person> al = al_it.next();
			sum += al.size();
		}
		// TreeMap(Double fraction, ArrayList(String chains))
		TreeMap<Double, ArrayList<String>> fractions = new TreeMap<Double, ArrayList<String>>();
		// create the fractions TreeMap
		System.out.println("        chain\tfreq\tpercentage");
		for (java.util.Map.Entry<String, ArrayList<Person>> entry : chains.entrySet()) {
			String c = entry.getKey();
			ArrayList<Person> persons = entry.getValue();
			Double frac = Double.valueOf(((double)persons.size())/((double)sum));
			if (!fractions.containsKey(frac)) {
				fractions.put(frac, new ArrayList<String>());
			}
			fractions.get(frac).add(c);
			System.out.println("        " + c + "\t" + persons.size() + "\t" + (frac.doubleValue()*100.0));
		}
		return fractions;
	}

	//////////////////////////////////////////////////////////////////////
	// run methods
	//////////////////////////////////////////////////////////////////////

	public void run(Population plans) {
		System.out.println("    running " + this.getClass().getName() + " algorithm...");

		// TreeMap(String chain, ArrayList(Person person))
		TreeMap<String, ArrayList<Person>> chains = this.calcChainFrequencies(plans);

		// TreeMap(Double fraction, ArrayList(String chains))
		System.out.println("      chain distribution before cut:");
		TreeMap<Double, ArrayList<String>> fractions = this.calcFractionTreeMap(chains);

		// remove all persons from the plans data structure, which are below the
		// cumulative fraction cut
		double cumulate_fraq = 0.0;
		for (java.util.Map.Entry<Double, ArrayList<String>> entry : fractions.entrySet()) {
			Double f = entry.getKey();
			ArrayList<String> cs = entry.getValue();

			// for each chain of the given fraction f, remove those persons from the
			// plans data structure
			for (int i=0; i<cs.size(); i++) {
				String chain = cs.get(i);
				ArrayList<Person> persons = chains.get(chain);

				for (int j=0, n=persons.size(); j<n; j++) {
					Id pid = (persons.get(j)).getId();
					plans.getPersons().remove(pid);
				}
			}
			cumulate_fraq += f.doubleValue()*cs.size();
			if (cumulate_fraq > 1-this.cut) {
				break;
			}
		}

		// TreeMap(String chain, ArrayList(Person person))
		chains = this.calcChainFrequencies(plans);

		// TreeMap(Double fraction, ArrayList(String chains))
		System.out.println("      chain distribution after cut:");
		this.calcFractionTreeMap(chains);

		System.out.println("    done.");
	}
}
