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

package playground.andreas.utils.pop;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationReader;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.ConfigUtils;

/**
 * Filter persons, not using a specific TransportMode.
 *
 * @author aneumann
 *
 */
public class FilterPersonPlan extends NewPopulation {
	private int planswritten = 0;
	private int personshandled = 0;

	public FilterPersonPlan(Network network, Population plans, String filename) {
		super(network, plans, filename);
	}

	@Override
	public void run(Person person) {

		this.personshandled++;

		if(person.getPlans().size() != 1){
			System.err.println("Person got more than one plan");
		} else {

			Plan plan = person.getPlans().get(0);
			boolean keepPlan = true;

			// only keep person if every leg is a car leg
			for (PlanElement planElement : plan.getPlanElements()) {
				if(planElement instanceof Leg){
					if(((Leg)planElement).getMode() != TransportMode.car && ((Leg)planElement).getMode() != TransportMode.pt){
						keepPlan = false;
					}
				}
			}

			if(keepPlan){
				this.popWriter.writePerson(person);
				this.planswritten++;
			}

		}

	}

	public static void main(final String[] args) {
		Gbl.startMeasurement();

		ScenarioImpl sc = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());

		String networkFile = "./bb_cl.xml.gz";
		String inPlansFile = "./baseplan.xml.gz";
		String outPlansFile = "./baseplan_car_pt_only.xml.gz";

		NetworkImpl net = sc.getNetwork();
		new MatsimNetworkReader(sc).readFile(networkFile);

		Population inPop = sc.getPopulation();
		PopulationReader popReader = new MatsimPopulationReader(sc);
		popReader.readFile(inPlansFile);

		FilterPersonPlan dp = new FilterPersonPlan(net, inPop, outPlansFile);
		dp.run(inPop);
		System.out.println(dp.personshandled + " persons handled; " + dp.planswritten + " plans written to file");
		dp.writeEndPlans();

		Gbl.printElapsedTime();
	}
}
