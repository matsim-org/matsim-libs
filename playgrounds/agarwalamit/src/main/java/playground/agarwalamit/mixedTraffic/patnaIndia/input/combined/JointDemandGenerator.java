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
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.utils.objectattributes.ObjectAttributesXmlWriter;
import org.matsim.vehicles.VehicleWriterV1;

import playground.agarwalamit.mixedTraffic.patnaIndia.input.PatnaVehiclesGenerator;
import playground.agarwalamit.mixedTraffic.patnaIndia.input.urban.UrbanDemandGenerator;
import playground.agarwalamit.mixedTraffic.patnaIndia.utils.PatnaPersonFilter;
import playground.agarwalamit.mixedTraffic.patnaIndia.utils.PatnaUtils;
import playground.agarwalamit.utils.LoadMyScenarios;

/**
 * Combines the Cadyts calibrated commuters and through traffic with urban demand.
 * This needs to be further calibrated for urban mode specific ASCs.
 * 
 * @author amit
 */

public class JointDemandGenerator {

	private static final String EXT_PLANS = "../../../../repos/runs-svn/patnaIndia/run108/external/"+PatnaUtils.PATNA_NETWORK_TYPE.toString()+"/outerCordonOutput_10pct_OC1Excluded/output_plans.xml.gz"; // calibrated from cadyts.
	private static final String JOINT_PLANS_10PCT = PatnaUtils.INPUT_FILES_DIR+"/simulationInputs/joint/"+PatnaUtils.PATNA_NETWORK_TYPE.toString()+"/joint_plans_10pct.xml.gz"; //
	private static final String JOINT_PERSONS_ATTRIBUTE_10PCT = PatnaUtils.INPUT_FILES_DIR+"/simulationInputs/joint/"+PatnaUtils.PATNA_NETWORK_TYPE.toString()+"/joint_personAttributes_10pct.xml.gz"; //
	private static final String JOINT_VEHICLES_10PCT = PatnaUtils.INPUT_FILES_DIR+"/simulationInputs/joint/"+PatnaUtils.PATNA_NETWORK_TYPE.toString()+"/joint_vehicles_10pct.xml.gz";
	private static final int CLONING_FACTOR = 10; 
	private static Scenario sc;
	
	public static void main(String[] args) {
		JointDemandGenerator pjdg = new JointDemandGenerator();
		pjdg.combinedPlans();
		pjdg.createSubpopulationAttributes();
		new PopulationWriter(sc.getPopulation()).write(JOINT_PLANS_10PCT);
		new ObjectAttributesXmlWriter(sc.getPopulation().getPersonAttributes()).writeFile(JOINT_PERSONS_ATTRIBUTE_10PCT);
		pjdg.createAndWriteVehiclesFile();
	}
	
	public void createAndWriteVehiclesFile(){
		PatnaVehiclesGenerator pvg = new PatnaVehiclesGenerator(JOINT_PLANS_10PCT);
		pvg.createVehicles(PatnaUtils.ALL_MAIN_MODES);
		new VehicleWriterV1(pvg.getPatnaVehicles()).writeFile(JOINT_VEHICLES_10PCT);
	}
	
	public void createSubpopulationAttributes(){
		sc.getConfig().plans().setSubpopulationAttributeName(PatnaUtils.SUBPOP_ATTRIBUTE);
		Population pop = sc.getPopulation();
		for(Person p : pop.getPersons().values()){
			pop.getPersonAttributes().putAttribute(p.getId().toString(), PatnaUtils.SUBPOP_ATTRIBUTE, PatnaPersonFilter.getUserGroup(p.getId()).toString() );	
		}
	}

	private void combinedPlans(){
		Population popUrban = getUrbanPlans();
		Population popExtDemand = getExternalPlans();

		sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());

		for(Person p : popUrban.getPersons().values()){
			sc.getPopulation().addPerson(p);
			// also put all person attributes to scenario.
			sc.getPopulation().getPersonAttributes().putAttribute(p.getId().toString(), PatnaUtils.INCOME_ATTRIBUTE, 
					popUrban.getPersonAttributes().getAttribute(p.getId().toString(), PatnaUtils.INCOME_ATTRIBUTE));
			sc.getPopulation().getPersonAttributes().putAttribute(p.getId().toString(), PatnaUtils.TRANSPORT_COST_ATTRIBUTE, 
					popUrban.getPersonAttributes().getAttribute(p.getId().toString(), PatnaUtils.TRANSPORT_COST_ATTRIBUTE));
		}

		for(Person p : popExtDemand.getPersons().values()){
			sc.getPopulation().addPerson(p);
		}	
	}

	private Population getUrbanPlans() {
		UrbanDemandGenerator pudg = new UrbanDemandGenerator(CLONING_FACTOR);// 10% sample
		pudg.startProcessing();
		return pudg.getPopulation();
	}

	private Population getExternalPlans(){ // take only selected plans, add 'ext' suffix to leg mode (if does not exists) so that mode choice is enabled for sub population.
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
					mode = mode.contains("ext") ? mode : mode.concat("_ext");
					planOut.addLeg(pf.createLeg(mode));
				} else {
					planOut.addActivity((Activity)pe);
				}
			}
			popOut.addPerson(pOut);
		}
		return popOut;
	}
}