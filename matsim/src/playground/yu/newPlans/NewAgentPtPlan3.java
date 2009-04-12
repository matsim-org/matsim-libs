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
import org.matsim.core.api.population.PersonAlgorithm;
import org.matsim.core.api.population.Plan;
import org.matsim.core.api.population.Population;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.PopulationReader;

import playground.yu.analysis.PlanModeJudger;

/**
 * writes new Plansfile, in which every person will has 2 plans, one with type
 * "iv" and the other with type "oev", whose leg mode will be "pt" and who will
 * have only a blank <Route></Rout>
 * 
 * @author ychen
 * 
 */
public class NewAgentPtPlan3 extends NewPopulation implements PersonAlgorithm {

	private final List<Plan> copyPlans = new ArrayList<Plan>();

	/**
	 * Constructor, writes file-head
	 * 
	 * @param plans
	 *            - a Plans Object, which derives from MATSim plansfile
	 */
	public NewAgentPtPlan3(final Population plans) {
		super(plans);
		copyPlans.clear();
	}

	@SuppressWarnings("unchecked")
	@Override
	public void run(final Person person) {
		// if (person.getLicense().equals("yes")) {
		// copyPlans: the copy of the plans.
		for (Plan pl : person.getPlans()) {
			Leg firstLeg = (Leg) pl.getPlanElements().get(1);
			TransportMode legMode = firstLeg.getMode();
			//pl.setType(NewAgentPtPlan2.getPlanType(legMode));//???????????????

			if (!legMode.equals(TransportMode.car)) {
				if (person.getLicense().equals("yes")) {
					Plan copyPlan = new org.matsim.core.population.PlanImpl(person);
					//copyPlan.setType(Plan.Type.CAR);//????????????????????????
					// ??
					copyPlans.add(copyPlan);
				}
			} else if (!legMode.equals(TransportMode.pt)) {
				Plan copyPlan = new org.matsim.core.population.PlanImpl(person);
				// copyPlan.setType(Plan.Type.PT);//?????????????????????
				copyPlans.add(copyPlan);
			}

			List actsLegs = pl.getPlanElements();
			int actsLegsSize = actsLegs.size();
			for (Plan copyPlan : copyPlans)
				for (int i = 0; i < actsLegsSize; i++) {
					Object o = actsLegs.get(i);
					if (i % 2 == 0)
						copyPlan.addAct((Activity) o);
					else {
						Leg leg = (Leg) o;
						Leg copyLeg = new org.matsim.core.population.LegImpl(leg);
						copyLeg.setRoute(null);
						copyLeg.setMode(
						// copyPlan.getType().toString()
								PlanModeJudger.getMode(copyPlan));
						// -----------------------------------------------
						// WITHOUT routeSetting!! traveltime of "pt" or
						// "car"can be calculated automaticly!!
						// -----------------------------------------------
						copyPlan.addLeg(copyLeg);
					}
				}
		}

		for (Plan copyPlan : copyPlans)
			person.addPlan(copyPlan);
		copyPlans.clear();

		// }
		if (person.getLicense().equals("no")) {
			List<Plan> plans = person.getPlans();
			for (Plan pl : plans)
				if (
				// pl.getType().equals(BasicLeg.CARMODE)
				PlanModeJudger.getMode(pl).equals(TransportMode.car))
					plans.remove(pl);
		}

		pw.writePerson(person);
	}
	public static void main(final String[] args) {
		final String netFilename = "../data/ivtch/input/network.xml";
		final String plansFilename = "../data/ivtch/input/10pctZrhPlans.xml.gz";
		Gbl
				.createConfig(new String[] { "../data/ivtch/newAllPlansWithLicense.xml" });

		NetworkLayer network = new NetworkLayer();
		new MatsimNetworkReader(network).readFile(netFilename);

		Population population = new PopulationImpl();
		NewAgentPtPlan3 nap3 = new NewAgentPtPlan3(population);

		PopulationReader plansReader = new MatsimPopulationReader(population,
				network);
		plansReader.readFile(plansFilename);

		nap3.run(population);

		nap3.writeEndPlans();
	}
}
