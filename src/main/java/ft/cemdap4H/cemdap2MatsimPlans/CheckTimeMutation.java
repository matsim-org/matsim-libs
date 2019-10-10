/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

package ft.cemdap4H.cemdap2MatsimPlans;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.population.algorithms.TripPlanMutateTimeAllocation;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.population.io.StreamingPopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;

import java.util.Random;

/**
 * @author saxer
 */
public class CheckTimeMutation {

	public static void main(String[] args) {

//		Collection<String> stages = new ArrayList<String>();
//		stages.add(PtConstants.TRANSIT_ACTIVITY_TYPE);
//		StageActivityTypes blackList = new StageActivityTypesImpl(stages);


		long nr = 1896;
		Random r = MatsimRandom.getRandom();
		r.setSeed(nr);

        //since PtConstants.TRANSIT_ACTIVITY_TYPE ends on 'interaction', TripPlanMutateTimeAllocation still acounts for this stageActivityType
        TripPlanMutateTimeAllocation TripPlanMutator = new TripPlanMutateTimeAllocation(7200,
				true, r);

		// Create a Scenario
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		// Fill this Scenario with a population.
		new PopulationReader(scenario).readFile("D:\\Thiel\\Programme\\MatSim\\01_HannoverModel_2.0\\Simulation\\output\\vw251.1.0\\vw251.1.0.output_plans.xml.gz");
		String modifiedPop_Path = "D:\\Thiel\\Programme\\MatSim\\01_HannoverModel_2.0\\Simulation\\output\\vw251.1.0\\vw251.1.0.output_plans_endFix.xml.gz";
		StreamingPopulationWriter modifiedPop = new StreamingPopulationWriter();
		modifiedPop.startStreaming(modifiedPop_Path);

		for (Person person : scenario.getPopulation().getPersons().values()) {
			PersonUtils.removeUnselectedPlans(person);
			//TimeMutate each plan 100 times
//			for (int i = 0; i < 100; i++) {
//				
//			}
			
			for (Plan plan : person.getPlans()) {
			
			TripPlanMutator.run(plan);
            }

            //Check time consistency between end times of activities
			double now = 0;

//			for (PlanElement pe : person.getSelectedPlan().getPlanElements()) {
//				if (pe instanceof Activity) {
//					Activity act = (Activity) pe;
//
//					if (!Time.isUndefinedTime(act.getEndTime())) {
//
//						if (act.getEndTime() < now) {
//							System.out.println("time consistency broken - see agent: " + person.getId() );
//							break;
//						}
//						now = act.getEndTime();
//					
//					}
//
//				}
//
//			}

			modifiedPop.writePerson(person);
		}

		modifiedPop.closeStreaming();

	}
}
