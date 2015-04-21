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

package playground.johannes.gsv.sim.misc;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlReader;

import playground.johannes.gsv.synPop.CommonKeys;
import playground.johannes.gsv.synPop.Proxy2Matsim;

/**
 * @author johannes
 * 
 */
public class DistanceCompare {

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);

		MatsimPopulationReader pReader = new MatsimPopulationReader(scenario);
		pReader.readFile(args[0]);

		ObjectAttributes oAttrs = scenario.getPopulation().getPersonAttributes();
		ObjectAttributesXmlReader oaReader = new ObjectAttributesXmlReader(oAttrs);
		oaReader.putAttributeConverter(ArrayList.class, new Proxy2Matsim.Converter());
		oaReader.parse(args[1]);

		BufferedWriter writer = new BufferedWriter(new FileWriter(args[2]));
		writer.write("mid\tsim");
		writer.newLine();
		
		for (Person person : scenario.getPopulation().getPersons().values()) {
			List<Double> targetDists = (List<Double>) oAttrs.getAttribute(person.getId().toString(), CommonKeys.LEG_GEO_DISTANCE);
			Plan plan = person.getSelectedPlan();
			for(int i = 1; i < plan.getPlanElements().size(); i+=2) {
				Leg leg = (Leg) plan.getPlanElements().get(i);
				double dist = leg.getRoute().getDistance();
				double target = targetDists.get((i - 1)/2);
		
				writer.write(String.valueOf(target));
				writer.write("\t");
				writer.write(String.valueOf(dist));
				writer.newLine();
			}
		}
		
		writer.close();

	}

}
