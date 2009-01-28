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

import org.matsim.basic.v01.BasicLeg;
import org.matsim.config.Config;
import org.matsim.gbl.Gbl;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;
import org.matsim.population.Act;
import org.matsim.population.Leg;
import org.matsim.population.MatsimPopulationReader;
import org.matsim.population.Person;
import org.matsim.population.Plan;
import org.matsim.population.Population;
import org.matsim.world.World;

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

	@Override
	public void run(Person person) {
		Plan sp = person.getSelectedPlan();
		person.getPlans().clear();
		person.addPlan(sp);

		// Plan.Type t = sp.getType();
		Plan cp = new Plan(person);
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
					Leg cl = new Leg((Leg) o);
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
					Leg cl = new Leg((Leg) o);
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
		World world = Gbl.getWorld();
		Config config = Gbl.createConfig(args);

		NetworkLayer network = new NetworkLayer();
		new MatsimNetworkReader(network).readFile(config.network()
				.getInputFile());
		world.setNetworkLayer(network);
		world.complete();

		Population population = new Population();
		ModeChoicePlan mcp = new ModeChoicePlan(population);
		population.addAlgorithm(mcp);
		new MatsimPopulationReader(population).readFile(config.plans()
				.getInputFile());
		population.runAlgorithms();
		mcp.writeEndPlans();
	}

}
