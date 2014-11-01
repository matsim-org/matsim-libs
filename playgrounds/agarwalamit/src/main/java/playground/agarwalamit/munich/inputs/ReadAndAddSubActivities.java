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

import java.util.SortedMap;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.config.MatsimConfigReader;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.utils.collections.Tuple;

import playground.agarwalamit.analysis.LoadMyScenarios;


/**
 * @author amit
 */
public class ReadAndAddSubActivities {

	public ReadAndAddSubActivities(String inputConfig, Scenario sc) {
		this.inputConfig = inputConfig;
		this.sc = sc;
	}

	private Config config;
	private String inputConfig;
	private Scenario sc;
	
	
	public static void main(String[] args) {
		
		String initialPlans = "/Users/aagarwal/Desktop/ils4/agarwal/munich/input/mergedPopulation_All_1pct_scaledAndMode_workStartingTimePeakAllCommuter0800Var2h_gk4.xml.gz";
		String initialConfig = "/Users/aagarwal/Desktop/ils4/agarwal/munich/input/config_munich_1pct_baseCase.xml";
		Scenario sc = LoadMyScenarios.loadScenarioFromPlansAndConfig(initialPlans,initialConfig);
		String outConfig = "/Users/aagarwal/Desktop/ils4/agarwal/munich/input/config_subActivities.xml";
		ReadAndAddSubActivities add2Config =  new ReadAndAddSubActivities(initialConfig,sc);
		add2Config.readConfig();
		add2Config.addDataAndWriteConfig(outConfig);

	}
	
	private void readConfig(){
		config = new Config();
		config.addCoreModules();
		MatsimConfigReader reader = new MatsimConfigReader(config);
		reader.readFile(inputConfig);
	}
	
	private void addDataAndWriteConfig(String outConfig){
		AddingActivitiesInPlans newPlans= new AddingActivitiesInPlans(sc);
		newPlans.run();
		SortedMap<String, Tuple<Double, Double>> acts = newPlans.getActivityType2TypicalAndMinimalDuration();
		
		for(String act :acts.keySet()){
			ActivityParams params = new ActivityParams(act);
			params.setTypicalDuration(acts.get(act).getFirst());
			params.setMinimalDuration(acts.get(act).getSecond());
			config.planCalcScore().addActivityParams(params);
		}
		new ConfigWriter(config).write(outConfig);
	}

}
