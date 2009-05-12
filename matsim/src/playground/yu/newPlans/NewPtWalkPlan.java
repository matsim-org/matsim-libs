/* *********************************************************************** *
 * project: org.matsim.*
 * NewAgentPtPlan.java
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

package playground.yu.newPlans;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.basic.v01.TransportMode;
import org.matsim.core.api.population.Activity;
import org.matsim.core.api.population.Leg;
import org.matsim.core.api.population.Person;
import org.matsim.core.api.population.Plan;
import org.matsim.core.api.population.Population;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationImpl;
import org.matsim.population.algorithms.PlanAlgorithm;

/**
 * writes new Plansfile, in which every person will has 2 plans, one with type
 * "iv" and the other with type "oev", whose leg mode will be "pt" and who will
 * have only a blank <Route></Rout>
 * 
 * @author ychen
 * 
 */
public class NewPtWalkPlan extends NewPopulation implements PlanAlgorithm {
	private Person person;
	private List<Plan> copyPlans = new ArrayList<Plan>();

	// copyPlans: the copy of the plans.
	/**
	 * Constructor, writes file-head
	 * 
	 * @param plans
	 *            - a Plans Object, which derives from MATSim plansfile
	 */
	public NewPtWalkPlan(final Population plans) {
		super(plans);
	}

	public NewPtWalkPlan(final Population population, final String filename) {
		super(population, filename);
	}

	@Override
	public void run(final Person person) {
		if (Integer.parseInt(person.getId().toString()) < 1000000000) {
			this.person = person;
			for (Plan pl : person.getPlans()) {
				run(pl);
			}
			for (Plan copyPlan : copyPlans) {
				person.addPlan(copyPlan);
			}
			copyPlans.clear();
		}
		this.pw.writePerson(person);
	}

	@SuppressWarnings("unchecked")
	public void run(Plan plan) {
		Plan ptPlan = new org.matsim.core.population.PlanImpl(person);
		Plan walkPlan = new org.matsim.core.population.PlanImpl(person);
		List actsLegs = plan.getPlanElements();
		for (int i = 0; i < actsLegs.size(); i++) {
			Object o = actsLegs.get(i);
			if (i % 2 == 0) {
				ptPlan.addActivity((Activity) o);
				walkPlan.addActivity((Activity) o);
			} else {
				Leg leg = (Leg) o;
				Leg ptLeg = new org.matsim.core.population.LegImpl(leg);
				ptLeg.setMode(TransportMode.pt);
				ptLeg.setRoute(null);
				// -----------------------------------------------
				// WITHOUT routeSetting!! traveltime of PT can be
				// calculated automaticly!!
				// -----------------------------------------------
				ptPlan.addLeg(ptLeg);

				Leg walkLeg = new org.matsim.core.population.LegImpl(leg);
				walkLeg.setMode(TransportMode.walk);
				walkLeg.setRoute(null);
				walkPlan.addLeg(walkLeg);
				if (!leg.getMode().equals(TransportMode.car)) {
					leg.setRoute(null);
					leg.setMode(TransportMode.car);
				}
			}
		}
		copyPlans.add(ptPlan);
		copyPlans.add(walkPlan);
	}

	public static void main(final String[] args) {
		Gbl.startMeasurement();

		final String netFilename = "../matsimTests/scoringTest/network.xml";
		final String plansFilename = "../matsimTests/scoringTest/plans100.xml";
		final String outputFilename = "../matsimTests/scoringTest/plans100_pt_walk.xml";

		NetworkLayer network = new NetworkLayer();
		new MatsimNetworkReader(network).readFile(netFilename);

		Population population = new PopulationImpl();

		NewPtWalkPlan npwp = new NewPtWalkPlan(population, outputFilename);

		new MatsimPopulationReader(population, network).readFile(plansFilename);

		npwp.run(population);

		npwp.writeEndPlans();

		System.out.println("--> Done!");
		Gbl.printElapsedTime();
		System.exit(0);
	}
}
