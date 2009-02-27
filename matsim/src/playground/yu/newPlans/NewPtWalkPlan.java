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

import org.matsim.gbl.Gbl;
import org.matsim.interfaces.basic.v01.BasicLeg.Mode;
import org.matsim.interfaces.basic.v01.BasicPlan.Type;
import org.matsim.interfaces.core.v01.Act;
import org.matsim.interfaces.core.v01.Leg;
import org.matsim.interfaces.core.v01.Person;
import org.matsim.interfaces.core.v01.PersonAlgorithm;
import org.matsim.interfaces.core.v01.Plan;
import org.matsim.interfaces.core.v01.Population;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;
import org.matsim.population.MatsimPopulationReader;
import org.matsim.population.PopulationImpl;
import org.matsim.population.algorithms.PlanAlgorithm;

/**
 * writes new Plansfile, in which every person will has 2 plans, one with type
 * "iv" and the other with type "oev", whose leg mode will be "pt" and who will
 * have only a blank <Route></Rout>
 * 
 * @author ychen
 * 
 */
public class NewPtWalkPlan extends NewPlan implements PersonAlgorithm,
		PlanAlgorithm {
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
		plan.setType(Type.CAR);
		Plan ptPlan = new org.matsim.population.PlanImpl(person);
		ptPlan.setType(Type.PT);
		Plan walkPlan = new org.matsim.population.PlanImpl(person);
		walkPlan.setType(Type.WALK);
		List actsLegs = plan.getActsLegs();
		for (int i = 0; i < actsLegs.size(); i++) {
			Object o = actsLegs.get(i);
			if (i % 2 == 0) {
				ptPlan.addAct((Act) o);
				walkPlan.addAct((Act) o);
			} else {
				Leg leg = (Leg) o;
				Leg ptLeg = new org.matsim.population.LegImpl(leg);
				ptLeg.setMode(Mode.pt);
				ptLeg.setRoute(null);
				// -----------------------------------------------
				// WITHOUT routeSetting!! traveltime of PT can be
				// calculated automaticly!!
				// -----------------------------------------------
				ptPlan.addLeg(ptLeg);

				Leg walkLeg = new org.matsim.population.LegImpl(leg);
				walkLeg.setMode(Mode.walk);
				walkLeg.setRoute(null);
				walkPlan.addLeg(walkLeg);
				if (!leg.getMode().equals(Mode.car)) {
					leg.setRoute(null);
					leg.setMode(Mode.car);
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

		Gbl.createConfig(null);

		NetworkLayer network = new NetworkLayer();
		new MatsimNetworkReader(network).readFile(netFilename);

		Population population = new PopulationImpl();

		NewPtWalkPlan npwp = new NewPtWalkPlan(population, outputFilename);
		population.addAlgorithm(npwp);

		new MatsimPopulationReader(population, network).readFile(plansFilename);

		population.runAlgorithms();

		npwp.writeEndPlans();

		System.out.println("--> Done!");
		Gbl.printElapsedTime();
		System.exit(0);
	}
}
