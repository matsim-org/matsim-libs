/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package playground.gregor.demandmodeling;

import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioLoaderImpl;

public class RemoveRoutes {


	public static void main(String [] args) {

		String cf = "../../inputs/configs/eafEvac.xml";
		ScenarioImpl sc = ScenarioLoaderImpl.createScenarioLoaderImplAndResetRandomSeed(cf).getScenario();
		NetworkImpl net = sc.getNetwork();
		new MatsimNetworkReader(sc).readFile(sc.getConfig().network().getInputFile());

		new MatsimPopulationReader(sc).readFile(sc.getConfig().plans().getInputFile());
		Population pop = sc.getPopulation();
		for (Person pers : pop.getPersons().values()) {
			Plan plan = pers.getSelectedPlan();
			((PlanImpl) plan).getNextLeg(((PlanImpl) plan).getFirstActivity()).setRoute(null);
			((PlanImpl) plan).getNextActivity(((PlanImpl) plan).getNextLeg(((PlanImpl) plan).getFirstActivity())).setType("h");
		}

		new PopulationWriter(pop, net).write(null);//sc.getConfig().plans().getOutputFile());

	}

}
