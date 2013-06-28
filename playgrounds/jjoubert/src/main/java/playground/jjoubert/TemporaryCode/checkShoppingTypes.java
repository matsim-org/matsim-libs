/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,     *
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

package playground.jjoubert.TemporaryCode;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationReader;
import org.matsim.core.scenario.ScenarioUtils;

import playground.southafrica.utilities.Header;

public class checkShoppingTypes {
	private final static Logger LOG = Logger.getLogger(checkShoppingTypes.class);

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Header.printHeader(checkShoppingTypes.class.toString(), args);
		
		Scenario sc  = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		PopulationReader pr = new MatsimPopulationReader(sc);
		pr.readFile(args[0]);
		
		int s1 = 0;
		int s2 = 0;
		int s3 = 0;
		int s4 = 0;
		int s5 = 0;
		
		for(Id id : sc.getPopulation().getPersons().keySet()){
			Plan plan = sc.getPopulation().getPersons().get(id).getSelectedPlan();
			for(PlanElement pe : plan.getPlanElements()){
				if(pe instanceof Activity){
					Activity act = (Activity) pe;
					String s = act.getType();
					if(s.equalsIgnoreCase("s1")){
						s1++;
					} else if(s.equalsIgnoreCase("s2")){
						s2++;
					} else if(s.equalsIgnoreCase("s3")){
						s3++;
					} else if(s.equalsIgnoreCase("s4")){
						s4++;
					} else if(s.equalsIgnoreCase("s5")){
						s5++;
					}
				}
			}
		}
		
		LOG.info("  s1: " + s1);
		LOG.info("  s2: " + s2);
		LOG.info("  s3: " + s3);
		LOG.info("  s4: " + s4);
		LOG.info("  s5: " + s5);

		Header.printFooter();
	}

}
