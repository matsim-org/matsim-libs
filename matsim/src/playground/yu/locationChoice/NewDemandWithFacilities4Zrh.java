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

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.facilities.ActivityFacilityImpl;
import org.matsim.core.facilities.ActivityOptionImpl;
import org.matsim.core.facilities.FacilitiesWriter;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.PopulationWriter;
import org.matsim.knowledges.KnowledgeImpl;
import org.matsim.knowledges.Knowledges;
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
		private ActivityFacilitiesImpl afs = null;
		private Map<Coord, ActivityFacilityImpl> afMap = null;
		private PersonImpl currentPerson = null;
		private KnowledgeImpl currentKnowledge = null;
		private long facCnt = 0;
		private Knowledges knowledges;

		public CreateActFacility(final ActivityFacilitiesImpl activityFacilities, Knowledges knowledges) {
			afs = activityFacilities;
			afMap = new HashMap<Coord, ActivityFacilityImpl>();
			this.knowledges = knowledges;
		}

		// public ActivityFacilities getActivityfacilities() {
		// return afs;
		// }

		@Override
		public void run(final Person person) {
			currentPerson = (PersonImpl) person;
			currentKnowledge = knowledges.getKnowledgesByPersonId().get(person.getId());
			if (currentKnowledge == null)
				knowledges.getFactory().createKnowledge(person.getId(), "");
			for (Plan plan : person.getPlans())
				run(plan);
		}

		public void run(final Plan plan) {
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
			ActivityFacilityImpl af = afMap.get(coord);
			if (af == null) {
				af = afs.createFacility(new IdImpl(facCnt++), coord);
				afMap.put(coord, af);
			}
			act.setFacility(af);
			ActivityOptionImpl ao = af.getActivityOptions().get(type);
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

		ScenarioImpl scenario = new ScenarioImpl();
		
		NetworkLayer net = scenario.getNetwork();
		new MatsimNetworkReader(net).readFile(netFilename);

		Knowledges knowledges = scenario.getKnowledges();
		
		PopulationImpl pop = scenario.getPopulation();
		new MatsimPopulationReader(scenario).readFile(inputPopFilename);

		ActivityFacilitiesImpl afs = scenario.getActivityFacilities();

		new CreateActFacility(afs, knowledges).run(pop);

		new PopulationWriter(pop, knowledges).writeFile(outputPopFilename);
		new FacilitiesWriter(afs).writeFile(outputFacilitiesFilename);

		System.out.println("----->done.");
	}

}
