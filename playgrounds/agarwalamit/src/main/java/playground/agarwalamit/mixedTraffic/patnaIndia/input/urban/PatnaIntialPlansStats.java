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

package playground.agarwalamit.mixedTraffic.patnaIndia.input.urban;

import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;

import playground.agarwalamit.mixedTraffic.patnaIndia.utils.PatnaUtils;
import playground.agarwalamit.utils.MapUtils;

/**
 * @author amit
 */

public class PatnaIntialPlansStats {
	
	private final String initialPlansFile = PatnaUtils.INPUT_FILES_DIR+"/simulationInputs/urban/"+PatnaUtils.PATNA_NETWORK_TYPE.toString()+"/initial_urban_plans_1pct.xml.gz";
	private final String personAttributeFile = PatnaUtils.INPUT_FILES_DIR+"/simulationInputs/urban/"+PatnaUtils.PATNA_NETWORK_TYPE.toString()+"/initial_urban_persionAttributes_1pct.xml.gz";
	
	public static void main(String[] args) {
		new PatnaIntialPlansStats().run();
	}
	
	private void run(){
		Config config = ConfigUtils.createConfig();
		config.plans().setInputFile(initialPlansFile);
		config.plans().setInputPersonAttributeFile(personAttributeFile);
		Scenario sc = ScenarioUtils.loadScenario(config);
		
		SortedMap<String,Integer> mode2counter = new TreeMap<>();
		SortedMap<Double,Integer> income2counter = new TreeMap<>();
		SortedMap<Double,Integer> cost2counter = new TreeMap<>();
		
		for (Person p :sc.getPopulation().getPersons().values()){
			String mode = ((Leg)p.getSelectedPlan().getPlanElements().get(1)).getMode();
			
			if (mode2counter.containsKey(mode)) mode2counter.put(mode, mode2counter.get(mode)+1);
			else mode2counter.put(mode, 1);
			
			double inc = (Double) sc.getPopulation().getPersonAttributes().getAttribute(p.getId().toString(), PatnaUtils.INCOME_ATTRIBUTE);
			double cost = (Double) sc.getPopulation().getPersonAttributes().getAttribute(p.getId().toString(), PatnaUtils.TRANSPORT_COST_ATTRIBUTE);
			
			if (income2counter.containsKey(inc)) income2counter.put(inc, income2counter.get(inc)+1);
			else income2counter.put(inc, 1);
			
			if (cost2counter.containsKey(cost)) cost2counter.put(cost, cost2counter.get(cost)+1);
			else cost2counter.put(cost, 1);
		}
		
		// check the distributions
		SortedMap<String,Double> mode2share = MapUtils.getIntPercentShare( mode2counter);
		for (Entry<String,Double> e : mode2share.entrySet()) {
			System.out.println("mode "+e.getKey()+"\t share "+e.getValue());
		}
		System.out.println("\n");
		
		SortedMap<Double, Double> inc2share = MapUtils.getIntPercentShare( income2counter );
		for (Entry<Double,Double> e : inc2share.entrySet()) {
			System.out.println("income "+e.getKey()+"\t share "+e.getValue());
		}
		System.out.println("\n");
		
		SortedMap<Double, Double> cost2share = MapUtils.getIntPercentShare(  cost2counter );
		for (Entry<Double,Double> e : cost2share.entrySet()) {
			System.out.println("daily transport cost "+e.getKey()+"\t share "+e.getValue());
		}
	}
}
