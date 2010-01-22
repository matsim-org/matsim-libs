/* *********************************************************************** *
 * project: org.matsim.*
 * ActsLocComp.java
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.facilities.MatsimFacilitiesReader;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationImpl;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;
import org.matsim.population.algorithms.PlanAlgorithm;

import playground.yu.utils.io.SimpleWriter;

/**
 * @author yu
 * 
 */
public class ActsLocComp {
	public static class ActsLocRecorder extends AbstractPersonAlgorithm
			implements PlanAlgorithm {
/**@variable actsLocs - Map<"String personId, List<"Coord activityLocationCoordinate>>*/
		private final Map<Id, List<ActivityImpl>> actsLocs = new HashMap<Id, List<ActivityImpl>>();

		public Map<Id, List<ActivityImpl>> getActsLocs() {
			return actsLocs;
		}

		@Override
		public void run(final Person person) {
			run(person.getSelectedPlan());
		}

		public void run(final Plan plan) {
			List<ActivityImpl> acts = new ArrayList<ActivityImpl>();
			for (PlanElement pe : plan.getPlanElements())
				if (pe instanceof ActivityImpl)
					acts.add((ActivityImpl) pe);
			actsLocs.put(plan.getPerson().getId(), acts);
		}
	}

	public static void compare2ActsLocs(final Map<Id, List<ActivityImpl>> mapA,
			final Map<Id, List<ActivityImpl>> mapB, final SimpleWriter writer) {
		for (Entry<Id, List<ActivityImpl>> entry : mapA.entrySet()) {
			Id personId = entry.getKey();
			List<ActivityImpl> listA = entry.getValue(), listB = mapB.get(personId);
			int sizeA = listA.size();
			if (sizeA == listB.size())
				for (int i = 0; i < sizeA; i++) {
					ActivityImpl actA = listA.get(i), actB = listB.get(i);
					if (!actA.getCoord().equals(actB.getCoord())
							&& (actA.getType().startsWith("w")
									|| actA.getType().startsWith("h")
									|| actA.getType().startsWith("e")
									|| actB.getType().startsWith("w")
									|| actB.getType().startsWith("h") || actB
									.getType().startsWith("e")))
						writer.writeln(personId + "\t" + i + "\t"
								+ actA.getCoord() + "\t" + i + "\t"
								+ actB.getCoord());
					writer.flush();
				}
			else {
				writer.writeln(personId
						+ " has different \"Activitys\" im mapA and mapB");
				writer.flush();
			}
		}
	}

	/** @param args */
	public static void main(final String[] args) {
		// String netFile =
		// "../schweiz-ivtch-SVN/baseCase/network/ivtch-osm.xml";
		// String facFile =
		// "../schweiz-ivtch-SVN/baseCase/facilities/facilities_v1.xml.gz";
		// String popFileA =
		// "../schweiz-ivtch-SVN/baseCase/plans/plans_all_zrh30km_transitincl_10pct_facility_v1.xml.gz";
		// String popFileB = "../matsimTests/locationChoice/100.plans.xml.gz";
		// String outputFile =
		// "../matsimTests/locationChoice/actLocsComp.txt.gz";
		String netFile = args[0];
		String facFile = args[1];
		String popFileA = args[2];
		String popFileB = args[3];
		String outputFile = args[4];
		// senario A
		ScenarioImpl scenarioA = new ScenarioImpl();

		new MatsimNetworkReader(scenarioA).readFile(netFile);

		new MatsimFacilitiesReader(scenarioA).readFile(facFile);

		PopulationImpl popA = scenarioA.getPopulation();
		new MatsimPopulationReader(scenarioA).readFile(popFileA);

		ActsLocRecorder alrA = new ActsLocRecorder();
		alrA.run(popA);

		// scenario B
		ScenarioImpl scenarioB = new ScenarioImpl();

		scenarioB.setNetwork(scenarioA.getNetwork());

		new MatsimFacilitiesReader(scenarioB).readFile(facFile);

		PopulationImpl popB = scenarioB.getPopulation();
		new MatsimPopulationReader(scenarioB).readFile(popFileB);

		ActsLocRecorder alrB = new ActsLocRecorder();
		alrB.run(popB);

		SimpleWriter writer = new SimpleWriter(outputFile);
		writer.writeln("personId\tactIdA\tCoordA\tactIdB\tCoordB");
		compare2ActsLocs(alrA.getActsLocs(), alrB.getActsLocs(), writer);
		writer.close();
	}

}
