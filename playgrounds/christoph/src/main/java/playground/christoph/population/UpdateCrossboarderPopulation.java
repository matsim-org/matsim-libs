/* *********************************************************************** *
 * project: org.matsim.*
 * UpdateCrossboarderPopulation.java
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

package playground.christoph.population;

import java.util.List;
import java.util.Random;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.ConfigUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.population.Desires;

/*
 * Updating:
 * Activity types are converted (e.g. h12, s5 -> home, shop) and desires are created.
 * duplicated TTA Activities that have the same coordinate are reduced to one.
 * 
 * Work activities are converted to work_sector2 and work_sector3.
 * The distribution is based on the number of such Activities in the CensusV2 Population
 * (5281715 work_sector2 Activities, 8391761 work_sector3 Activities).
 */
public class UpdateCrossboarderPopulation {

	private static final Logger log = Logger.getLogger(UpdateCrossboarderPopulation.class);

	private String populationFile = "../../matsim/mysimulations/crossboarder/plansCB.xml.gz";
	private String outFile = "../../matsim/mysimulations/crossboarder/plansCBV2.xml.gz";
	private double fraction = 1.0;

	private double work_sector2 = 5281715;
	private double work_sector3 = 8391761;
	private double work_sector2Probability = work_sector2 / (work_sector2 + work_sector3);
	private Random random = MatsimRandom.getLocalInstance();

	public static void main(String[] args) {
		new UpdateCrossboarderPopulation(((ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig())));
	}

	public UpdateCrossboarderPopulation(Scenario scenario) {

		log.info("Reading population file...");
		new MatsimPopulationReader(scenario).readFile(populationFile);
		log.info("done.");

		log.info("Updating persons...");
		for (Person person : scenario.getPopulation().getPersons().values()) {
			updatePerson((PersonImpl) person);
		}
		log.info("done.");

		log.info("Writing MATSim population to file...");
		new PopulationWriter(scenario.getPopulation(), scenario.getNetwork(), fraction).write(outFile);
		log.info("done.");
	}

	private void updatePerson(PersonImpl person) {
		Desires desires = person.createDesires("");
		
		boolean shopProcessed = false;
		boolean homeProcessed = false;
		boolean workProcessed = false;
		boolean leisureProcessed = false;
		boolean ttaProcessed = false;		
		
		/*
		 * The structure is always h-?-h or tta-tta
		 * Upate activity type
		 */
		for (PlanElement planElement : person.getSelectedPlan().getPlanElements()) {
			if (planElement instanceof Activity) {
				Activity activity = (Activity) planElement;
				
				String type = activity.getType();
				
				if (type.startsWith("w")) {
					String work = "";
					double prob = random.nextDouble();
					if (prob < work_sector2Probability) {
						work = "work_sector2";				
					} else {
						work = "work_sector3";
					}
						
					activity.setType(work);
					if (workProcessed) continue;
					
					type = type.substring(1);
					int duration = Integer.valueOf(type);
					desires.accumulateActivityDuration(work, duration*3600);
					workProcessed = true;
				} else if (type.startsWith("h")) {
					activity.setType("home");
					if (homeProcessed) continue;
					
					type = type.substring(1);
					int duration = Integer.valueOf(type);
					desires.accumulateActivityDuration("home", duration*3600);
					homeProcessed = true;
				} else if (type.startsWith("s")) {
					activity.setType("shop");
					if (shopProcessed) continue;
					
					type = type.substring(1);
					int duration = Integer.valueOf(type);
					desires.accumulateActivityDuration("shop", duration*3600);
					shopProcessed = true;
				} else if (type.startsWith("l")) {
					activity.setType("leisure");
					if (leisureProcessed) continue;
					
					type = type.substring(1);
					int duration = Integer.valueOf(type);
					desires.accumulateActivityDuration("leisure", duration*3600);
					leisureProcessed = true;
				}  else if (type.startsWith("t")) {
					if (ttaProcessed) continue;
					
					desires.accumulateActivityDuration("tta", 24*3600);
					ttaProcessed = true;
				}
				else log.info("unknown type: " + type);
			}
		}
		
		/*
		 * Update activity start- and endtime
		 */
		List<PlanElement> planElements = person.getSelectedPlan().getPlanElements();
		int numPlanElements = planElements.size();

		if (numPlanElements == 3) {
			Activity firstActivity = (Activity) planElements.get(0);
			Activity secondActivity = (Activity) planElements.get(2);
			
			firstActivity.setStartTime(0.0);
			secondActivity.setStartTime(firstActivity.getEndTime());
			secondActivity.setEndTime(Time.MIDNIGHT);
			
		} else if (numPlanElements == 5) {
			Activity firstActivity = (Activity) planElements.get(0);
			Activity secondActivity = (Activity) planElements.get(2);
			Activity thirdActivity = (Activity) planElements.get(4);
			
			firstActivity.setStartTime(0.0);
			secondActivity.setStartTime(firstActivity.getEndTime());
			secondActivity.setEndTime(secondActivity.getStartTime() + secondActivity.getMaximumDuration());
			secondActivity.setMaximumDuration(Time.UNDEFINED_TIME);
			thirdActivity.setStartTime(secondActivity.getEndTime());
			thirdActivity.setEndTime(Time.MIDNIGHT);
		} else log.warn("Unknown number of PlanElements: " + numPlanElements);
		
		/*
		 * If only two tta activities are performed and both are at the same location we merge
		 * them to a single activity with duration 24h.
		 */
		if (ttaProcessed) {
			if (person.getSelectedPlan().getPlanElements().size() == 3) {
				Activity startActivity = (Activity) person.getSelectedPlan().getPlanElements().get(0);
				Activity endActivity = (Activity) person.getSelectedPlan().getPlanElements().get(2);
				
				if (startActivity.getCoord().equals(endActivity.getCoord())) {
					PlanImpl plan = (PlanImpl) person.getSelectedPlan();
					plan.removeActivity(2);
					startActivity.setStartTime(0.0);
					startActivity.setMaximumDuration(Time.UNDEFINED_TIME);
					startActivity.setEndTime(Time.MIDNIGHT);
				}
			}
		}
	}
}
