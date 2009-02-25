/* *********************************************************************** *
 * project: org.matsim.*
 * ModeChoicePlan.java
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

import java.util.List;

import org.matsim.config.Config;
import org.matsim.gbl.Gbl;
import org.matsim.interfaces.basic.v01.BasicLeg;
import org.matsim.interfaces.core.v01.Act;
import org.matsim.interfaces.core.v01.Leg;
import org.matsim.interfaces.core.v01.Person;
import org.matsim.interfaces.core.v01.Plan;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;
import org.matsim.population.MatsimPopulationReader;
import org.matsim.population.Population;

import playground.yu.analysis.PlanModeJudger;

/**
 * @author yu
 * 
 */
public class ModeChoicePlan extends NewPlan {
	private boolean addNewPlan = false;

	/**
	 * @param plans
	 */
	public ModeChoicePlan(Population plans) {
		super(plans);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void run(Person person) {
		Plan sp = person.getSelectedPlan();
		person.getPlans().clear();
		person.addPlan(sp);

		// Plan.Type t = sp.getType();
		Plan cp = new org.matsim.population.PlanImpl(person);
		List actsLegs = sp.getActsLegs();

		if (
		// t.equals(Plan.Type.CAR)
		PlanModeJudger.useCar(sp)) {
			addNewPlan = true;
			// cp.setType(Plan.Type.PT);//?????????????????????????????????????
			for (int i = 0; i < actsLegs.size(); i++) {
				Object o = actsLegs.get(i);
				if (i % 2 == 0) {
					cp.addAct((Act) o);
				} else {
					Leg cl = new org.matsim.population.LegImpl((Leg) o);
					cl.setMode(
					// "pt"
							BasicLeg.Mode.pt);
					cl.setRoute(null);
					cp.addLeg(cl);
				}
			}
		} else if (
		// t.equals(Plan.Type.PT)
		PlanModeJudger.usePt(sp)) {
			addNewPlan = true;
			// cp.setType(Plan.Type.CAR);
			for (int i = 0; i < actsLegs.size(); i++) {
				Object o = actsLegs.get(i);
				if (i % 2 == 0) {
					cp.addAct((Act) o);
				} else {
					Leg cl = new org.matsim.population.LegImpl((Leg) o);
					cl.setMode(
					// "car"
							BasicLeg.Mode.car);
					cl.setRoute(null);
					cp.addLeg(cl);
				}
			}
		}
		if (addNewPlan) {
			person.addPlan(cp);
			addNewPlan = false;
		}

		pw.writePerson(person);
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Config config = Gbl.createConfig(args);

		NetworkLayer network = new NetworkLayer();
		new MatsimNetworkReader(network).readFile(config.network()
				.getInputFile());

		Population population = new Population();
		ModeChoicePlan mcp = new ModeChoicePlan(population);
		population.addAlgorithm(mcp);
		new MatsimPopulationReader(population, network).readFile(config.plans()
				.getInputFile());
		population.runAlgorithms();
		mcp.writeEndPlans();
	}

}
