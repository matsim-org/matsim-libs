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

/**
 * 
 */
package playground.jbischoff.taxi.inclusion;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;

/**
 * @author  jbischoff
 *
 */
/**
 *
 */
public class CreateInclusionPlans {

	private static final String DIR = "C:/Users/Joschka/Documents/shared-svn/projects/sustainability-w-michal-and-dlr/data/scenarios/inclusion/";
	private static final int sets = 10;
//	private static final int customers = 642;
	private static final int customers = 1500;
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		new CreateInclusionPlans().run();
	}
	
	private void run(){
		Network orig_network = NetworkUtils.createNetwork();
		new MatsimNetworkReader(orig_network).readFile(DIR+"orig_demand/networkc.xml.gz");
		Scenario orig_scen = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new PopulationReader(orig_scen).readFile(DIR+"orig_demand/plans.xml.gz");
		Network taxi_network = NetworkUtils.createNetwork();
		new MatsimNetworkReader(taxi_network).readFile(DIR+"berlin_brb.xml.gz");
		CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation("EPSG:25833",TransformationFactory.DHDN_GK4);
		for (int i = 0; i<sets; i++){
			Scenario taxi_scen = ScenarioUtils.createScenario(ConfigUtils.createConfig());
			new PopulationReader(taxi_scen).readFile(DIR+"orig_demand/plans4to3_1.5.xml.gz");
			
			Random random = new Random(4711+i);
			ArrayList<Person> allPlans = new ArrayList<>();
			allPlans.addAll(orig_scen.getPopulation().getPersons().values());
			Collections.shuffle(allPlans, random);
			for (int n = 0; n<customers; n++){
				Person p = allPlans.get(n);
				Person q = orig_scen.getPopulation().getFactory().createPerson(Id.createPersonId("hc_"+p.getId().toString()));
				Plan plan = orig_scen.getPopulation().getFactory().createPlan();
				Activity o_act0 = (Activity) p.getSelectedPlan().getPlanElements().get(0);
				Coord coord0 = ct.transform(orig_network.getLinks().get(o_act0.getLinkId()).getCoord());

				Activity o_act1 = (Activity) p.getSelectedPlan().getPlanElements().get(2);
				Coord coord1 = ct.transform(orig_network.getLinks().get(o_act1.getLinkId()).getCoord());
				
				Activity act0 = taxi_scen.getPopulation().getFactory().createActivityFromCoord("departure", coord0);
				act0.setEndTime(o_act0.getEndTime());
				Activity act1 = taxi_scen.getPopulation().getFactory().createActivityFromCoord("arrival", coord1);
				act1.setEndTime(o_act1.getEndTime());
				plan.addActivity(act0);
				plan.addLeg(taxi_scen.getPopulation().getFactory().createLeg("taxi"));
				plan.addActivity(act1);
				
				
				q.addPlan(plan);
				taxi_scen.getPopulation().addPerson(q);
			}
			new PopulationWriter(taxi_scen.getPopulation()).write(DIR+"itaxi_"+i+".xml.gz");
			
		}
		
	}

}
