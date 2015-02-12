/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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
package playground.agarwalamit.munich.calibration;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ModeParams;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.population.PopulationWriter;

import playground.agarwalamit.munich.inputs.AddingActivitiesInPlans;
import playground.agarwalamit.utils.LoadMyScenarios;
import playground.benjamin.scenarios.munich.analysis.filter.PersonFilter;
import playground.benjamin.scenarios.munich.analysis.filter.UserGroup;

/**
 * @author amit
 */

public class SubPopInputs4Munich {

	PersonFilter pf = new PersonFilter();

	public static void main(String[] args) {
		SubPopInputs4Munich inputs = new SubPopInputs4Munich();
		inputs.modifyConfig();
	}

	private void modifyPlans(){

		// read initial plans, (may be not merged one)
		String rawPlans = "";
		String outPopFile = "";

		Scenario sc_raw = LoadMyScenarios.loadScenarioFromPlans(rawPlans);
		Population pop = sc_raw.getPopulation();	

		for(Person p : pop.getPersons().values()){
			pop.getPersonAttributes().putAttribute(p.getId().toString(), "userGroup", getUserGroupFromPersonId(p.getId()));
		}

		// now  sub activity types
		AddingActivitiesInPlans newPlansInfo = new AddingActivitiesInPlans(sc_raw);
		newPlansInfo.run();

		new PopulationWriter(newPlansInfo.getOutPop()).write(outPopFile);
	}

	private void modifyConfig(){

		// I think, config with all sub activities info can be taken.
		String existingConfig = "../../../repos/runs-svn/detEval/emissionCongestionInternalization/input/config_subActivities_baseCase.xml"; 
		String outConfigFile = "../../../repos/runs-svn/detEval/emissionCongestionInternalization/input/config_subPop_subAct_baseCase.xml";

		Config config =  ConfigUtils.loadConfig(existingConfig);

		config.controler().setFirstIteration(0);
		config.controler().setLastIteration(1000);
		
		config.plans().setSubpopulationAttributeName("userGroup");
		
		//remove existing subtourModeChoice strategy settings; operation is not supported, may be remove it manually later. or just load the desired config modules only.
//		config.strategy().getStrategySettings().remove("SubtourModeChoice");

		// add corresponding to all user groups
		for(UserGroup ug : UserGroup.values()){

			StrategySettings modeChoice = new StrategySettings(ConfigUtils.createAvailableStrategyId(config));
			modeChoice.setStrategyName("subtourModeChoice_".concat(ug.toString()));
			modeChoice.setWeight(0.15);
			modeChoice.setSubpopulation(ug.toString());
			config.strategy().addStrategySettings(modeChoice);

			// first use existing pt mode parameters and set them as new pt mode parameters
			ModeParams ptParams = config.planCalcScore().getModes().get("pt");
			
//			for(String param : ptParams.getParams().keySet()){
//				config.planCalcScore().getOrCreateModeParams("pt_".concat(ug.toString())).addParam(param, ptParams.getParams().get(param));
//			}
//			config.planCalcScore().getOrCreateModeParams("pt_".concat(ug.toString())).addParameterSet(ptParams);
			
			config.planCalcScore().getOrCreateModeParams("pt_".concat(ug.toString())).setConstant(ptParams.getConstant());
			config.planCalcScore().getOrCreateModeParams("pt_".concat(ug.toString())).setMarginalUtilityOfDistance(ptParams.getMarginalUtilityOfDistance());
			config.planCalcScore().getOrCreateModeParams("pt_".concat(ug.toString())).setMarginalUtilityOfTraveling(ptParams.getMarginalUtilityOfTraveling());
			config.planCalcScore().getOrCreateModeParams("pt_".concat(ug.toString())).setMonetaryDistanceCostRate(ptParams.getMonetaryDistanceCostRate());
		
			// teleportation speeds for different pts
			
			config.plansCalcRoute().getOrCreateModeRoutingParams("pt_".concat(ug.toString())).
			setTeleportedModeSpeed(config.plansCalcRoute().getTeleportedModeSpeeds().get("pt"));
		}

		config.strategy().setFractionOfIterationsToDisableInnovation(0.8);
		
		// remove existing pt manually.

		new ConfigWriter(config).write(outConfigFile);

	}

	private String getUserGroupFromPersonId(Id<Person> personId){
		if(pf.isPersonFromMunich(personId)) return UserGroup.URBAN.toString();
		else if(pf.isPersonInnCommuter(personId)) return UserGroup.COMMUTER.toString();
		else if(pf.isPersonOutCommuter(personId)) return UserGroup.REV_COMMUTER.toString();
		else if (pf.isPersonFreight(personId)) return UserGroup.FREIGHT.toString();
		else throw new RuntimeException("Person "+personId+" does not belong to any user group. Aborting ...");
	}
}
