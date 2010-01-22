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

import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.PlanImpl.Type;
import org.matsim.population.algorithms.PlanAlgorithm;

import playground.yu.analysis.PlanModeJudger;

/**
 * writes new Plansfile, in which every person will has 2 plans, one with type
 * "car" and the other with type "pt", whose leg mode will be "pt" and who will
 * have only a blank <Route></Rout>
 *
 * @author ychen
 *
 */
public class NewPtPlans extends NewPopulation implements PlanAlgorithm {
	private Person person;
	private List<PlanImpl> copyPlans = new ArrayList<PlanImpl>();

	// copyPlans: the copy of the plans.
	/**
	 * Constructor, writes file-head
	 *
	 * @param plans
	 *            - a Plans Object, which derives from MATSim plansfile
	 */
	public NewPtPlans(final Network network, final Population plans) {
		super(network, plans);
	}

	public NewPtPlans(final Network network, final Population population, final String filename) {
		super(network, population, filename);
	}

	@Override
	public void run(final Person person) {
		if (Integer.parseInt(person.getId().toString()) < 1000000000) {
			this.person = person;
			for (Plan pl : person.getPlans()) {
				run(pl);
			}
			for (PlanImpl copyPlan : copyPlans) {
				person.addPlan(copyPlan);
			}
			copyPlans.clear();
		}
		this.pw.writePerson(person);
	}

	public void run(Plan plan) {
		if (PlanModeJudger.useCar(plan))
			((PlanImpl) plan).setType(Type.CAR);
		PlanImpl ptPlan = new PlanImpl(person);
		ptPlan.setType(Type.PT);
		// Plan walkPlan = new PlanImpl(person);
		List<PlanElement> actsLegs = plan.getPlanElements();
		for (int i = 0; i < actsLegs.size(); i++) {
			Object o = actsLegs.get(i);
			if (i % 2 == 0) {
				ptPlan.addActivity((ActivityImpl) o);
				// walkPlan.addActivity((Activity) o);
			} else {
				LegImpl leg = (LegImpl) o;
				LegImpl ptLeg = new LegImpl(leg);
				ptLeg.setMode(TransportMode.pt);
				ptLeg.setRoute(null);
				// -----------------------------------------------
				// WITHOUT routeSetting!! traveltime of PT can be
				// calculated automaticly!!
				// -----------------------------------------------
				ptPlan.addLeg(ptLeg);

				// Leg walkLeg = new org.matsim.core.population.LegImpl(leg);
				// walkLeg.setMode(TransportMode.walk);
				// walkLeg.setRoute(null);
				// walkPlan.addLeg(walkLeg);
				if (!leg.getMode().equals(TransportMode.car)) {
					leg.setRoute(null);
					leg.setMode(TransportMode.car);
				}
			}
		}
		copyPlans.add(ptPlan);
		// copyPlans.add(walkPlan);
	}

	public static void main(final String[] args) {
		Gbl.startMeasurement();

		final String netFilename = "examples/equil/network.xml";
		final String plansFilename = "../matsimTests/Calibration/test/plans100.xml";
		final String outputFilename = "../matsimTests/Calibration/test/plans100withPt.xml";

		ScenarioImpl scenario = new ScenarioImpl();
		NetworkLayer network = scenario.getNetwork();
		new MatsimNetworkReader(scenario).readFile(netFilename);

		Population population = scenario.getPopulation();
		new MatsimPopulationReader(scenario).readFile(plansFilename);

		NewPtPlans npwp = new NewPtPlans(network, population, outputFilename);
		npwp.run(population);
		npwp.writeEndPlans();

		System.out.println("--> Done!");
		Gbl.printElapsedTime();
		System.exit(0);
	}
}
