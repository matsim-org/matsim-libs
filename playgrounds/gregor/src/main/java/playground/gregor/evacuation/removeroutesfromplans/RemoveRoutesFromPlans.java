/* *********************************************************************** *
 * project: org.matsim.*
 * RemoveRoutesFromPlans.java
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
package playground.gregor.evacuation.removeroutesfromplans;

import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PopulationReaderMatsimV4;
import org.matsim.core.population.PopulationWriter;

/**
 * @author laemmel
 * 
 */
public class RemoveRoutesFromPlans {

	public static void main(String[] args) {
		String in = "/home/laemmel/arbeit/svn/shared-svn/studies/countries/id/padang/plans/padang_plans_v20100707_EAF_flooding.xml.gz";
		String out = "/home/laemmel/arbeit/svn/shared-svn/studies/countries/id/padang/plans/padang_plans_v20100707_EMPTY_ROUTES.xml.gz";
		String net = "/home/laemmel/arbeit/svn/shared-svn/studies/countries/id/padang/network/padang_net_evac_v20100317.xml.gz";
		ScenarioImpl sc1 = new ScenarioImpl();
		new MatsimNetworkReader(sc1).readFile(net);
		new PopulationReaderMatsimV4(sc1).readFile(in);

		PopulationFactory fac = sc1.getPopulation().getFactory();
		for (Person p : sc1.getPopulation().getPersons().values()) {
			Activity a1 = (Activity) p.getSelectedPlan().getPlanElements().get(0);
			Activity a2 = (Activity) p.getSelectedPlan().getPlanElements().get(2);
			Leg leg = fac.createLeg("car");
			Plan plan = fac.createPlan();
			plan.addActivity(a1);
			plan.addLeg(leg);
			plan.addActivity(a2);
			p.addPlan(plan);
			((PersonImpl) p).setSelectedPlan(plan);
			((PersonImpl) p).removeUnselectedPlans();
		}
		new PopulationWriter(sc1.getPopulation(), sc1.getNetwork()).write(out);
	}

}
