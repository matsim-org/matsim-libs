/* *********************************************************************** *
 * project: org.matsim.*
 * newDemandWithFacilities.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

/**
 * 
 */
package playground.yu.locationChoice;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.basic.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.core.api.facilities.ActivityFacilities;
import org.matsim.core.api.facilities.ActivityFacility;
import org.matsim.core.api.network.Network;
import org.matsim.core.api.population.Activity;
import org.matsim.core.api.population.Person;
import org.matsim.core.api.population.Plan;
import org.matsim.core.api.population.PlanElement;
import org.matsim.core.api.population.Population;
import org.matsim.core.facilities.FacilitiesWriter;
import org.matsim.core.facilities.MatsimFacilitiesReader;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PopulationWriter;
import org.matsim.population.Knowledge;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;
import org.matsim.population.algorithms.PlanAlgorithm;

/**
 * @author yu
 * 
 */
public class NewDemandWithFacilities4Zrh {
	public static class InsertActFacility extends AbstractPersonAlgorithm
			implements PlanAlgorithm {
		private Map<Coord, ActivityFacility> afMap = null;
		private PersonImpl currentPerson = null;
		private Knowledge currentKnowledge = null;

		public InsertActFacility(ActivityFacilities afs) {
			afMap = new HashMap<Coord, ActivityFacility>();
			for (ActivityFacility af : afs.getFacilities().values())
				afMap.put(af.getCoord(), af);
		}

		@Override
		public void run(Person person) {
			currentPerson = (PersonImpl) person;
			currentKnowledge = currentPerson.getKnowledge();
			if (currentKnowledge == null)
				currentKnowledge = new Knowledge();
			for (Plan plan : person.getPlans())
				run(plan);
			currentPerson.setKnowledge(currentKnowledge);
		}

		public void run(Plan plan) {
			for (PlanElement pe : plan.getPlanElements())
				if (pe instanceof Activity) {
					Activity act = (Activity) pe;
					String type = act.getType();
					allocateFacility2PrimaryActs4Zrh(type, act);
				}
		}

		private void allocateFacility2PrimaryActs4Zrh(String type, Activity act) {
			if (type.startsWith("h") || type.startsWith("w")
					|| type.startsWith("e")) {
				ActivityFacility af = afMap.get(act.getCoord());
				if (af != null) {
					act.setFacility(af);
					currentKnowledge.addActivity(af.createActivityOption(type),
							true);
				}
			}
		}
	}

	/**
	 * @param args
	 *            - args0 netFilename, args1 inputPopFilename, args2
	 *            facilitiesFilename, args3 outputPopFilename, args4
	 *            outputFacilitiesFilename
	 */
	public static void main(String[] args) {
		// String netFilename =
		// "../schweiz-ivtch-SVN/baseCase/network/ivtch-osm.xml";
		// String inputPopFilename =
		// "../schweiz-ivtch-SVN/baseCase/plans/plans_all_zrh30km_transitincl_10pct.xml.gz";
		// String facilitiesFilename =
		// "../schweiz-ivtch-SVN/baseCase/facilities/facilities.xml.gz";
		// String outputPopFilename =
		// "../matsimTests/locationChoice/pop.xml.gz";
		// String outputFacilitiesFilename =
		// "../matsimTests/locationChoice/facs.xml.gz";

		String netFilename = args[0];
		String inputPopFilename = args[1];
		String facilitiesFilename = args[2];
		String outputPopFilename = args[3];
		String outputFacilitiesFilename = args[4];

		Scenario scenario = new ScenarioImpl();

		Network net = scenario.getNetwork();
		new MatsimNetworkReader(net).readFile(netFilename);

		Population pop = scenario.getPopulation();
		new MatsimPopulationReader(pop, net).readFile(inputPopFilename);

		ActivityFacilities afs = scenario.getActivityFacilities();
		new MatsimFacilitiesReader(afs).readFile(facilitiesFilename);

		new InsertActFacility(afs).run(pop);

		new PopulationWriter(pop, outputPopFilename).write();
		new FacilitiesWriter(afs, outputFacilitiesFilename).write();

		System.out.println("----->done.");
	}

}
