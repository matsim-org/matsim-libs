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

/**
 * 
 */
package playground.jbischoff.pt.scenario;

import java.util.Random;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.population.io.PopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.Time;

/**
 * @author  jbischoff
 *
 */
/**
 *
 */
public class ExtractSamplePopulation {

	public static void main(String[] args) {
		String inputPlans = "C:/Users/Joschka/Documents/shared-svn/projects/audi_av/scenario/flowpaper/taxiplans/plansCarsRoutesAV1.00.xml.gz";
		String net = "C:/Users/Joschka/Documents/shared-svn/studies/jbischoff/multimodal/berlin/input/network.final10pct.xml.gz";
		double samplesize = 0.01;
		String outputPlans = "C:/Users/Joschka/Documents/shared-svn/studies/jbischoff/multimodal/berlin/input/testplans"+samplesize+".xml.gz";
		Random r = MatsimRandom.getLocalInstance();
		
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Scenario scenario2 = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new PopulationReader(scenario).readFile(inputPlans);
		new MatsimNetworkReader(scenario.getNetwork()).readFile(net);
		for (Person p : scenario.getPopulation().getPersons().values()){
			if (p.getId().toString().endsWith("t")){
				if (r.nextDouble()<=samplesize){
				Plan plan = p.getSelectedPlan();
				Activity act0 = (Activity) plan.getPlanElements().get(0);
				Leg leg = (Leg) plan.getPlanElements().get(1);
				Activity act1 = (Activity) plan.getPlanElements().get(2);
				leg.setMode("pt");
				leg.setTravelTime(Time.UNDEFINED_TIME);
				leg.setRoute(null);
				act0.setCoord(scenario.getNetwork().getLinks().get(act0.getLinkId()).getCoord());
				act0.setLinkId(null);
				act1.setCoord(scenario.getNetwork().getLinks().get(act1.getLinkId()).getCoord());
				act1.setLinkId(null);
				scenario2.getPopulation().addPerson(p);
				
				
			}}
		}
		new PopulationWriter(scenario2.getPopulation()).write(outputPlans);
		
	}
}
