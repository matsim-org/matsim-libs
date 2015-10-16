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
package playground.agarwalamit.munich.inputs;

import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ModeParams;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.replanning.DefaultPlanStrategiesModule;
import org.matsim.utils.objectattributes.ObjectAttributesXmlWriter;

import playground.agarwalamit.utils.LoadMyScenarios;
import playground.benjamin.scenarios.munich.analysis.filter.PersonFilter;

/**
 * @author amit
 */

public class SubPopInputs4Munich {

	PersonFilter pf = new PersonFilter();
	private final String subPopAttributeName = "userGroup";
	private String outPopAttributeFile = "../../../../repos/runs-svn/detEval/emissionCongestionInternalization/input/personsAttributes_1pct_usrGrp.xml.gz";

	public static void main(String[] args) {
		SubPopInputs4Munich inputs = new SubPopInputs4Munich();
		inputs.writePersonAttributes();
		inputs.modifyConfig();
	}

	private void writePersonAttributes(){

		// read plans with subActivities (basically these are inital plans from different sources + subActivities)
		String initialPlans = "../../../../repos/runs-svn/detEval/emissionCongestionInternalization/input/mergedPopulation_All_1pct_scaledAndMode_workStartingTimePeakAllCommuter0800Var2h_gk4_wrappedSubActivities.xml.gz";
		String outPlansFile = "../../../../repos/runs-svn/detEval/emissionCongestionInternalization/input/mergedPopulation_All_1pct_scaledAndMode_workStartingTimePeakAllCommuter0800Var2h_gk4_wrappedSubActivities_usrGrp.xml.gz";
		
		Scenario sc = LoadMyScenarios.loadScenarioFromPlans(initialPlans);
		Population pop = sc.getPopulation();	
		
		for(Person p : pop.getPersons().values()){
			
			pop.getPersonAttributes().putAttribute(p.getId().toString(), subPopAttributeName, getUserGroupFromPersonId(p.getId()));

			//pt of commuter and rev_commuter need to be replaced by some other mode.
			if(pf.isPersonInnCommuter(p.getId()) || pf.isPersonOutCommuter(p.getId())){
				List<PlanElement> pes = p.getSelectedPlan().getPlanElements(); // only one plan each person in initial plans
				for(PlanElement pe : pes){

					if(pe instanceof Leg){
						if(((Leg)pe).getMode().equals(TransportMode.pt)){
							((Leg)pe).setMode("pt_COMMUTER_REV_COMMUTER");
						}
					}
				}
			}

		}

		new PopulationWriter(pop).write(outPlansFile);
		
		ObjectAttributesXmlWriter writer = new ObjectAttributesXmlWriter(pop.getPersonAttributes()) ;
		writer.writeFile(outPopAttributeFile);
	}

	private void modifyConfig(){

		// I think, config with all sub activities info can be taken.
		String existingConfig = "../../../../repos/runs-svn/detEval/emissionCongestionInternalization/input/config_wrappedSubActivities_baseCase_msa.xml"; 
		String outConfigFile = "../../../../repos/runs-svn/detEval/emissionCongestionInternalization/input/config_wrappedSubActivities_usrGrp_baseCase_msa.xml"; // need manual verification later

		Config config =  ConfigUtils.loadConfig(existingConfig);

		config.plans().setSubpopulationAttributeName(subPopAttributeName); // if this is set then, one have to set same strategy for all sub pops.
		config.plans().setInputPersonAttributeFile(outPopAttributeFile);

		String usrGrps [] = {"OTHERS","COMMUTER_REV_COMMUTER"};

		// once subPop attribute is set, strategy for all sub pop groups neet to set seprately.
		StrategySettings reroute = new StrategySettings(ConfigUtils.createAvailableStrategyId(config));
		reroute.setStrategyName(DefaultPlanStrategiesModule.DefaultStrategy.ReRoute.name());
		reroute.setSubpopulation(usrGrps[1]);
		reroute.setDisableAfter(800);
		reroute.setWeight(0.15);
		config.strategy().addStrategySettings(reroute);

		StrategySettings expBeta = new StrategySettings(ConfigUtils.createAvailableStrategyId(config));
		expBeta.setStrategyName("ChangeExpBeta");
		expBeta.setSubpopulation(usrGrps[1]);
		expBeta.setWeight(0.7);
		config.strategy().addStrategySettings(expBeta);

		StrategySettings modeChoice_comm = new StrategySettings(ConfigUtils.createAvailableStrategyId(config));
		modeChoice_comm.setStrategyName("SubtourModeChoice_".concat(usrGrps[1]));
		modeChoice_comm.setDisableAfter(800);
		modeChoice_comm.setWeight(0.15);
		modeChoice_comm.setSubpopulation(usrGrps[1]);
		config.strategy().addStrategySettings(modeChoice_comm);

		// first use existing pt mode parameters and set them as new pt mode parameters
		ModeParams ptParams = config.planCalcScore().getModes().get(TransportMode.pt);

		config.planCalcScore().getOrCreateModeParams("pt_".concat("COMMUTER_REV_COMMUTER")).setConstant(-0.3);
		config.planCalcScore().getOrCreateModeParams("pt_".concat("COMMUTER_REV_COMMUTER")).setMarginalUtilityOfDistance(ptParams.getMarginalUtilityOfDistance());
		config.planCalcScore().getOrCreateModeParams("pt_".concat("COMMUTER_REV_COMMUTER")).setMarginalUtilityOfTraveling(ptParams.getMarginalUtilityOfTraveling());
		config.planCalcScore().getOrCreateModeParams("pt_".concat("COMMUTER_REV_COMMUTER")).setMonetaryDistanceRate(ptParams.getMonetaryDistanceRate());

		// teleportation speeds for different pts
		config.plansCalcRoute().getOrCreateModeRoutingParams("pt_".concat("COMMUTER_REV_COMMUTER")).setTeleportedModeSpeed(50/3.6);

		config.strategy().setFractionOfIterationsToDisableInnovation(0.8);

		Logger.getLogger(SubPopInputs4Munich.class).warn("Config from this is not the final config used for calibration. Some unavoidable modifications are made manually in the .xml file. For e.g. "
				+ "\n 1) existing strategies are taken for urban and freight and for reverse commuters and commuters new modules are added."
//				+ "\n 2) At the moment, same module name can not be used for two different sub populations and \n therefore parameters are added with different name in config "
//				+ " and then added to controler directly."
				);

		new ConfigWriter(config).write(outConfigFile);
	}

	private String getUserGroupFromPersonId(Id<Person> personId){
		if(pf.isPersonInnCommuter(personId)) return "COMMUTER_REV_COMMUTER";
		else if(pf.isPersonOutCommuter(personId)) return "COMMUTER_REV_COMMUTER";
		else return "OTHERS";
	}
}
