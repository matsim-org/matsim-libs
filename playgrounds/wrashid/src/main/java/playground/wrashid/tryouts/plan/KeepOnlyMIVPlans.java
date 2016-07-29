/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package playground.wrashid.tryouts.plan;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.internal.MatsimReader;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.MatsimFacilitiesReader;

/*
 * removes non miv plans from input file...
 *  (facilities needed for some plans files...)
 *   => probably can just remove things related to faclities and it should work (or look at earlier revision)
 *     TODO: ###########################  refactor class and move class to "lib.tools.plan" (implementation of interface "New Population" not required.
 */

public class KeepOnlyMIVPlans extends NewPopulation {
	public static void main(String[] args) {

		MutableScenario sc = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());

		String inputPlansFile = "/data/matsim/wrashid/input/plans/teleatlas/census2000v2_dilZh30km_miv_only/plans.xml.gz";
		String outputPlansFile = "/data/matsim/wrashid/input/plans/teleatlas/census2000v2_dilZh30km_miv_only/plans1.xml.gz";
		String networkFile = "/data/matsim/switzerland/ivt/studies/switzerland/networks/teleatlas/network.xml.gz";
		String facilitiesPath = "/data/matsim/switzerland/ivt/studies/switzerland/facilities/facilities.xml.gz";

		//String inputPlansFile = "./test/scenarios/chessboard/plans.xml";
		//String outputPlansFile = "./plans1.xml";
		//String networkFile = "./test/scenarios/chessboard/network.xml";
		//String facilitiesPath = "./test/scenarios/chessboard/facilities.xml";

		new MatsimFacilitiesReader(sc).readFile(facilitiesPath);

		Population inPop = sc.getPopulation();

		Network net = sc.getNetwork();
		new MatsimNetworkReader(sc.getNetwork()).readFile(networkFile);

		MatsimReader popReader = new PopulationReader(sc);
		popReader.readFile(inputPlansFile);

		KeepOnlyMIVPlans dp = new KeepOnlyMIVPlans(net, inPop, outputPlansFile);
		dp.run(inPop);
		dp.writeEndPlans();
	}

	public KeepOnlyMIVPlans(Network network, Population plans, String filename) {
		super(network, plans, filename);
	}

	@Override
	public void run(Person person) {

		if(person.getPlans().size() != 1){
			System.err.println("Person got more than one plan");
		} else {

			Plan plan = person.getPlans().get(0);
			boolean keepPlan = true;

			// only keep person if every leg is a car leg
			for (PlanElement planElement : plan.getPlanElements()) {
				if(planElement instanceof Leg){
					if(((Leg)planElement).getMode() != TransportMode.car){
						keepPlan = false;
					}
				}
			}

			if(keepPlan){
				this.popWriter.writePerson(person);
			}

		}

	}
}
