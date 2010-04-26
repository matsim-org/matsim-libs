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
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PersonImpl;
import org.matsim.population.algorithms.PlanAlgorithm;

import playground.yu.analysis.PlanModeJudger;

/**
 * writes new Plansfile, in which every person will has 2 plans, one with type
 * "iv" and the other with type "oev", whose leg mode will be "pt" and who will
 * have only a blank <Route></Rout>
 *
 * @author ychen
 *
 */
public class NewAgentCarPlan extends NewPopulation implements PlanAlgorithm {
	private Person person = null;
	private final List<Plan> plans = new ArrayList<Plan>();

	/**
	 * Constructor, writes file-head
	 *
	 * @param network
	 * @param plans
	 *            - a Plans Object, which derives from MATSim plansfile
	 */
	public NewAgentCarPlan(final Network network, final Population plans, final String filename) {
		super(network, plans, filename);
	}

	@Override
	public void run(final Person person) {
		this.person = person;
		this.plans.clear();
		this.plans.addAll(person.getPlans());
		for (Plan p : this.plans) {
			run(p);
		}
		this.pw.writePerson(person);
	}

	public void run(final Plan plan) {
		if (!PlanModeJudger.useCar(plan))
			((PersonImpl) this.person).removePlan(plan);
	}

	public static void main(final String[] args) {
		final String netFilename = "../schweiz-ivtch-SVN/baseCase/network/ivtch-osm.xml";
		final String plansFilename = "../schweiz-ivtch-SVN/baseCase/plans/plans_all_zrh30km_transitincl_10pct.xml.gz";
		final String outputPlansFilename = "../schweiz-ivtch-SVN/baseCase/plans/plans_all_with_car_zrh30km_transitincl_10pct.xml.gz";

		ScenarioImpl scenario = new ScenarioImpl();
		NetworkLayer network = scenario.getNetwork();
		new MatsimNetworkReader(scenario).readFile(netFilename);

		Population population = scenario.getPopulation();
		new MatsimPopulationReader(scenario).readFile(plansFilename);

		NewAgentCarPlan nac = new NewAgentCarPlan(network, population,
				outputPlansFilename);
		nac.run(population);
		nac.writeEndPlans();
	}

}
