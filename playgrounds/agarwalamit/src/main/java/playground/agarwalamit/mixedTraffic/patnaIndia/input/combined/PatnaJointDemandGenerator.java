/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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
package playground.agarwalamit.mixedTraffic.patnaIndia.input.combined;

import java.util.List;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;

import playground.agarwalamit.mixedTraffic.patnaIndia.input.urban.PatnaUrbanDemandGenerator;
import playground.agarwalamit.utils.LoadMyScenarios;

/**
 * Combines the Cadyts calibrated commuters and through traffic with urban demand.
 * This needs to be further calibrated for urban mode specific ASCs.
 * 
 * @author amit
 */

public class PatnaJointDemandGenerator {
	
	private static final String EXT_PLANS = "../../../../repos/runs-svn/patnaIndia/run108/outerCordonOutput_10pct_OC1Excluded_ctd/output_plans.xml.gz"; // calibrated from cadyts.
	private static final String JOINT_PLANS_10PCT = "../../../../repos/shared-svn/projects/patnaIndia/inputs/simulationInputs/joint_plans_10pct.xml.gz"; //
	private static Scenario sc;
	
	public static void main(String[] args) {
		PatnaJointDemandGenerator pjdg = new PatnaJointDemandGenerator();
		pjdg.combinedPlans();
		new PopulationWriter(sc.getPopulation()).write(JOINT_PLANS_10PCT);
	}
	
	private void combinedPlans(){
		Population popUrban = getUrbanPlans();
		Population popExtDemand = getExternalPlans();
		
		sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		
		for(Person p : popUrban.getPersons().values()){
			sc.getPopulation().addPerson(p);
		}
		
		for(Person p : popExtDemand.getPersons().values()){
			sc.getPopulation().addPerson(p);
		}	
	}
	
	private Population getUrbanPlans() {
		PatnaUrbanDemandGenerator pudg = new PatnaUrbanDemandGenerator(10);// 10% sample
		pudg.startProcessing();
		return pudg.getPopulation();
	}
	
	private Population getExternalPlans(){ // take only selected plans, add 'ext' suffix to leg mode so that mode choice is enabled for sub population.
		Population popIn = LoadMyScenarios.loadScenarioFromPlans(EXT_PLANS).getPopulation();
		
		Scenario scOut = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Population popOut = scOut.getPopulation();
		PopulationFactory pf = popOut.getFactory();
		
		for(Person p : popIn.getPersons().values() ){
			Plan plan = p.getSelectedPlan();
			Person pOut = pf.createPerson(p.getId());
			Plan planOut = pf.createPlan();
			pOut.addPlan(planOut);
			
			List<PlanElement> pes = plan.getPlanElements();
			for(PlanElement pe : pes){
				if(pe instanceof Leg) {
					String mode = ((Leg) pe).getMode();
					planOut.addLeg(pf.createLeg(mode.concat("_ext")));
				} else {
					planOut.addActivity((Activity)pe);
				}
			}
			popOut.addPerson(pOut);
		}
		return popOut;
	}
}