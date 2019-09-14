/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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

package playground.vsp.openberlinscenario.planmodification;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;

import playground.vsp.openberlinscenario.cemdap.output.CemdapOutput2MatsimPlansConverter;

/**
 * Every person in sampled plans file has exactly one plan. This class reads rest plans files (100% scenario) and
 * extract the plans for sampled plans. Eventually, this writes out a sampled plans file with full choice set for each person.
 *
 * Created by amit on 24.10.17.
 */
public class SampledPlansMerger {

    public static void main(String[] args) {
    	String plansBaseDir = "../../shared-svn/studies/countries/de/open_berlin_scenario/be_5/population/500";
    	int numberOfFirstPlansFile = 500;
    	String firstPlanFileName = "plans_10pct.xml.gz";
    	int numberOfPlans = 2;
        String plansFolderPrefix = "pop_";
        String plansFolderSuffix = "_1-0";
        String otherPlansFileNames = "plans.xml.gz";
        String outputPlansFileName = plansBaseDir + "/plans_500-10-1_10pct.xml.gz";
        boolean addStayHomePlan = true;

        if (args.length > 0) {
        	plansBaseDir = args[0];
        	numberOfFirstPlansFile = Integer.valueOf(args[1]);
        	firstPlanFileName = args[2];
            numberOfPlans = Integer.valueOf(args[3]);
            plansFolderPrefix = args[4];
            plansFolderSuffix = args[5];
            otherPlansFileNames = args[6];
            outputPlansFileName = args[7];
            addStayHomePlan = Boolean.parseBoolean(args[8]);
            
            // Until 2018-10-24, this was the numbering
//          numberOfFirstPlansFile = Integer.valueOf(args[0]);
//          numberOfPlans = Integer.valueOf(args[1]);
//          plansBaseDir = args[2];
//          outPlans = args[3];
//          plansFolderPrefix = args[4];
//          plansFolderSuffix = args[5];
//          addStayHomePlan = Boolean.parseBoolean(args[6]);
        }

//      String sampledPlans = plansBaseDir + "/" + plansFolderPrefix + numberOfFirstPlansFile + plansFolderSuffix + "/plans_10pct.xml.gz"; // hardcoded until 2018-10-24
        String sampledPlans = plansBaseDir + "/" + plansFolderPrefix + numberOfFirstPlansFile + plansFolderSuffix + "/" + firstPlanFileName;
        Population sampledPop = getPopulation(sampledPlans);

        for (int planNumber = 1; planNumber < numberOfPlans; planNumber++) {
//          String unsampledPlans = plansBaseDir + "/" + plansFolderPrefix + (numberOfFirstPlansFile + planNumber) + plansFolderSuffix + "/plans.xml.gz"; // hardcoded until 2018-10-24
            String unsampledPlans = plansBaseDir + "/" + plansFolderPrefix + (numberOfFirstPlansFile + planNumber) + plansFolderSuffix + "/" + otherPlansFileNames;
            Population unsampledPop = getPopulation(unsampledPlans);

            for (Person sampledPerson : sampledPop.getPersons().values()) {
                Person person = unsampledPop.getPersons().get(sampledPerson.getId());
                if (person == null) {
                	throw new RuntimeException("Sampled person " + sampledPerson.getId() + " is not found in unsampled plans "+ unsampledPlans + ".");
                } else if (person.getPlans().size() == 0) {
                	throw new RuntimeException("Sampled person " + sampledPerson.getId() + " does not have any plan in his choice set.");
                } else if (person.getPlans().size() == 1) {
                    sampledPerson.addPlan(person.getPlans().get(0));
                } else if (person.getPlans().size()==2) {
                    Plan firstPlan = person.getPlans().get(0);
                    sampledPerson.addPlan(firstPlan); // The first plan must be added (It can be a "normal" plan or a stay-home plan.)

                    int lengthOfPlanElement = person.getPlans().get(1).getPlanElements().size();
                    if (lengthOfPlanElement == 1) { // The second plan in a given input population must be a stay-home plan if this class is applied correctly.
                    } else {
                        throw new RuntimeException("The second plan of the unsampled person " + person.getId() + " must be stay-home plan. The number of plan elements, however, is " + lengthOfPlanElement + "." );
                    }
                } else{
                    throw new RuntimeException("Unsampled person " + sampledPerson.getId() + " should have less than 3 plans in choice set. It has " + person.getPlans().size() + " in his choice set.");
                }
            }
        }
        
        if (addStayHomePlan) {
			for (Person person : sampledPop.getPersons().values()) {
				Plan firstPlan = person.getPlans().get(0);
				Activity firstActivity = (Activity) firstPlan.getPlanElements().get(0); // Get first (i.e. presumably "home") activity from agent's first plan

				Plan stayHomePlan = sampledPop.getFactory().createPlan();
				// Create new activity with type and coordinates (but without end time) and add it to stay-home plan
				Activity Activity2 = sampledPop.getFactory().createActivityFromCoord(firstActivity.getType(), firstActivity.getCoord());
				Activity2.getAttributes().putAttribute(CemdapOutput2MatsimPlansConverter.activityZoneId_attributeKey, firstActivity.getAttributes().getAttribute(CemdapOutput2MatsimPlansConverter.activityZoneId_attributeKey));
				stayHomePlan.addActivity(Activity2);
				person.addPlan(stayHomePlan);
			}
		}
        
        new PopulationWriter(sampledPop).write(outputPlansFileName);
    }

    private static Population getPopulation (String plansFile) {
        Config config = ConfigUtils.createConfig();
        config.plans().setInputFile(plansFile);
        return ScenarioUtils.loadScenario(config).getPopulation();
    }
}