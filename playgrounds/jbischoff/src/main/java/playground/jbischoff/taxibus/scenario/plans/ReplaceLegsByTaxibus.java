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

package playground.jbischoff.taxibus.scenario.plans;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;

import playground.jbischoff.taxibus.utils.TaxibusUtils;

/**
 * @author  jbischoff
 *
 */
public class ReplaceLegsByTaxibus {
public static void main(String[] args) {
	DecimalFormatSymbols otherSymbols = new DecimalFormatSymbols(Locale.US);
	
	 DecimalFormat df = new DecimalFormat("#.##",otherSymbols);

	for (double threshold = 0.1; threshold < 0.6; threshold = threshold + 0.1) {
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
	String filename = "../../../shared-svn/projects/vw_rufbus/scenario/input/subpopulations/taxibusplans_"+df.format(threshold)+".xml.gz";
	new MatsimPopulationReader(scenario).readFile(filename);
	for (Person p : scenario.getPopulation().getPersons().values()){
		for (Plan plan : p.getPlans()){
			Leg l1 = (Leg) plan.getPlanElements().get(1);
			if (l1.getMode().equals("transit_walk")) continue;
			
			l1.setMode(TaxibusUtils.TAXIBUS_MODE);
			
			Leg l2 = (Leg) plan.getPlanElements().get(3);
			l2.setMode(TaxibusUtils.TAXIBUS_MODE);
			
		}
	}
	new PopulationWriter(scenario.getPopulation()).write("../../../shared-svn/projects/vw_rufbus/scenario/input/subpopulations/taxibusplans_"+df.format(threshold)+"tb.xml.gz");
	
	}
	}
}
