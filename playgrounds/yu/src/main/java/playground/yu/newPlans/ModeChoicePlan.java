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

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.scenario.ScenarioLoaderImpl;

import playground.yu.analysis.PlanModeJudger;

/**
 * @author yu
 * 
 */
public class ModeChoicePlan extends NewPopulation {
	private boolean addNewPlan = false;

	/**
	 * @param plans
	 */
	public ModeChoicePlan(final Population plans) {
		super(plans);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void run(final Person person) {
		Plan sp = person.getSelectedPlan();
		person.getPlans().clear();
		person.addPlan(sp);

		// Plan.Type t = sp.getType();
		PlanImpl cp = new org.matsim.core.population.PlanImpl(person);
		List actsLegs = sp.getPlanElements();

		if (
		// t.equals(Plan.Type.CAR)
		PlanModeJudger.useCar(sp)) {
			this.addNewPlan = true;
			// cp.setType(Plan.Type.PT);//?????????????????????????????????????
			for (int i = 0; i < actsLegs.size(); i++) {
				Object o = actsLegs.get(i);
				if (i % 2 == 0) {
					cp.addActivity((ActivityImpl) o);
				} else {
					LegImpl cl = new org.matsim.core.population.LegImpl((LegImpl) o);
					cl.setMode(
					// "pt"
							TransportMode.pt);
					cl.setRoute(null);
					cp.addLeg(cl);
				}
			}
		} else if (
		// t.equals(Plan.Type.PT)
		PlanModeJudger.usePt(sp)) {
			this.addNewPlan = true;
			// cp.setType(Plan.Type.CAR);
			for (int i = 0; i < actsLegs.size(); i++) {
				Object o = actsLegs.get(i);
				if (i % 2 == 0) {
					cp.addActivity((ActivityImpl) o);
				} else {
					LegImpl cl = new org.matsim.core.population.LegImpl((LegImpl) o);
					cl.setMode(
					// "car"
							TransportMode.car);
					cl.setRoute(null);
					cp.addLeg(cl);
				}
			}
		}
		if (this.addNewPlan) {
			person.addPlan(cp);
			this.addNewPlan = false;
		}

		this.pw.writePerson(person);
	}

	/**
	 * @param args
	 */
	public static void main(final String[] args) {
		Scenario scenario = new ScenarioLoaderImpl(args[0]).loadScenario();
		
		ModeChoicePlan mcp = new ModeChoicePlan(scenario.getPopulation());
		mcp.run(scenario.getPopulation());
		mcp.writeEndPlans();
	}

}
