/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

package playground.jbischoff.av.preparation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.misc.Time;

/**
 * @author  jbischoff
 *
 */
public class Modify24hTrips {

	public static void main(String[] args) {
		Random rnd = MatsimRandom.getRandom();
		// TODO Auto-generated method stub
//		new MatsimNetworkReader(scenario).readFile("C:/Users/Joschka/Documents/runs-svn/bvg.run132.25pct/bvg.run132.25pct.output_network.xml.gz");
		
		List<String> filenames = new ArrayList<>(Arrays.asList(new String[]{"C:/Users/Joschka/Documents/shared-svn/projects/audi_av/scenario/plans.xml.gz","C:/Users/Joschka/Documents/shared-svn/projects/audi_av/scenario/plansWithCars.xml.gz","C:/Users/Joschka/Documents/shared-svn/projects/audi_av/scenario/plansWithCars0.10.xml.gz","C:/Users/Joschka/Documents/shared-svn/projects/audi_av/scenario/plansWithCarsR.xml.gz","C:/Users/Joschka/Documents/shared-svn/projects/audi_av/scenario/plansWithCarsR0.10.xml.gz",
				"C:/Users/Joschka/Documents/shared-svn/projects/audi_av/scenario/subscenarios/rainyday/plansWithCarsR0.10.xml.gz", "C:/Users/Joschka/Documents/shared-svn/projects/audi_av/scenario/subscenarios/sunnyday/plansWithCarsR0.10.xml.gz", "C:/Users/Joschka/Documents/shared-svn/projects/audi_av/scenario/subscenarios/tenpercentpt/plansWithCars0.10.xml.gz", "C:/Users/Joschka/Documents/shared-svn/projects/audi_av/scenario/subscenarios/tenpercentpt/plansWithCarsR0.10.xml.gz"}) ); 
		
		for( String filename :filenames){
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new PopulationReader(scenario).readFile(filename);
		
		for (Person person : scenario.getPopulation().getPersons().values()){
			Plan plan = person.getSelectedPlan();
			Activity act1 = (Activity) plan.getPlanElements().get(0);
			if (act1.getEndTime()>(24*3600-1)){
				double newEndtime = act1.getEndTime() - 24*3600;
				act1.setEndTime(newEndtime);
				
				Leg leg1 = (Leg) plan.getPlanElements().get(1);
				if (leg1.getDepartureTime()!=Time.UNDEFINED_TIME){
					double newDep = leg1.getDepartureTime()-24*3600;
					leg1.setDepartureTime(newDep);
					
					double newArr = leg1.getDepartureTime() + leg1.getTravelTime()-24*3600;
					final double arrTime = newArr;
					leg1.setTravelTime( arrTime - leg1.getDepartureTime() );
					
				}
				
				
				Activity act2 = (Activity) plan.getPlanElements().get(2);
				double newStartTime =act2.getStartTime() - 24*3600;
				act2.setStartTime(newStartTime);
			}
	
			
		
		
		}
		
		new PopulationWriter(scenario.getPopulation()).write(filename);
		
	}
		}

}
