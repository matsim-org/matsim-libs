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
import org.matsim.core.api.experimental.Scenario;
import org.matsim.core.api.experimental.ScenarioImpl;
import org.matsim.core.api.experimental.population.PlanElement;
import org.matsim.core.api.experimental.population.Population;
import org.matsim.core.api.facilities.ActivityFacilities;
import org.matsim.core.api.facilities.ActivityFacility;
import org.matsim.core.api.facilities.ActivityOption;
import org.matsim.core.api.network.Network;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.facilities.FacilitiesWriter;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.PopulationWriter;
import org.matsim.knowledges.Knowledge;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;
import org.matsim.population.algorithms.PlanAlgorithm;

/**
 * create facilities to the zrh-demand
 * 
 * @author yu
 * 
 */
public class NewDemandWithFacilities4Zrh {
	public static class CreateActFacility extends AbstractPersonAlgorithm
			implements PlanAlgorithm {
		private ActivityFacilities afs = null;
		private Map<Coord, ActivityFacility> afMap = null;
		private PersonImpl currentPerson = null;
		private Knowledge currentKnowledge = null;
		private long facCnt = 0;

		public CreateActFacility(final ActivityFacilities activityFacilities) {
			afs = activityFacilities;
			afMap = new HashMap<Coord, ActivityFacility>();
		}

		// public ActivityFacilities getActivityfacilities() {
		// return afs;
		// }

		@Override
		public void run(final PersonImpl person) {
			currentPerson = (PersonImpl) person;
			currentKnowledge = currentPerson.getKnowledge();
			if (currentKnowledge == null)
				currentKnowledge = new Knowledge();
			for (PlanImpl plan : person.getPlans())
				run(plan);
			currentPerson.setKnowledge(currentKnowledge);
		}

		public void run(final PlanImpl plan) {
			for (PlanElement pe : plan.getPlanElements())
				if (pe instanceof ActivityImpl) {
					ActivityImpl act = (ActivityImpl) pe;
					String type = act.getType();
					allocateFacility2PrimaryActs4Zrh(type, act);
				}
		}

		private void allocateFacility2PrimaryActs4Zrh(final String type,
				final ActivityImpl act) {
			Coord coord = act.getCoord();
			ActivityFacility af = afMap.get(coord);
			if (af == null) {
				af = afs.createFacility(new IdImpl(facCnt++), coord);
				afMap.put(coord, af);
			}
			act.setFacility(af);
			ActivityOption ao = af.getActivityOption(type);
			if (ao == null)
				ao = af.createActivityOption(type);
			// 3 primary type in Zurich scenario
			currentKnowledge.addActivity(ao, (type.startsWith("h")
					|| type.startsWith("w") || type.startsWith("e")));

		}
	}

	/**
	 * @param args
	 *            - args0 netFilename, args1 inputPopFilename, args2
	 *            outputPopFilename, args3 outputFacilitiesFilename
	 */
	public static void main(final String[] args) {
		String netFilename = "examples/equil/network.xml";
		String inputPopFilename = "../matsimTests/locationChoice/plans2.xml";
		// String facilitiesFilename =
		// "../schweiz-ivtch-SVN/baseCase/facilities/facilities.xml.gz";
		String outputPopFilename = "../matsimTests/locationChoice/pop.xml.gz";
		String outputFacilitiesFilename = "../matsimTests/locationChoice/facs.xml.gz";

		// String netFilename = args[0];
		// String inputPopFilename = args[1];
		// // String facilitiesFilename = args[2];
		// String outputPopFilename = args[2];
		// String outputFacilitiesFilename = args[3];

		Scenario scenario = new ScenarioImpl();

		Network net = scenario.getNetwork();
		new MatsimNetworkReader(net).readFile(netFilename);

		Population pop = scenario.getPopulation();
		new MatsimPopulationReader(pop, net).readFile(inputPopFilename);

		ActivityFacilities afs = scenario.getActivityFacilities();

		new CreateActFacility(afs).run(pop);

		new PopulationWriter(pop, outputPopFilename).write();
		new FacilitiesWriter(afs, outputFacilitiesFilename).write();

		System.out.println("----->done.");
	}

}
