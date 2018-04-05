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
package ft.cemdap4H.planspreprocessing;

import java.util.Random;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.population.io.PopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * @author  jbischoff
 *
 */
/**
 *
 */
public class WobCemdapPlansSample {
	
	public static void main(String[] args) {
		final String fullSamplePlansFile = "D:/cemdap-vw/Output/mergedplans_filtered.xml.gz";
		double scale = 0.01;
		new WobCemdapPlansSample().run(fullSamplePlansFile, scale);
	}		

	public void run(String fullSamplePlansFile, double scale) {
		
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new PopulationReader(scenario).readFile(fullSamplePlansFile);
		Random r = MatsimRandom.getRandom();
		Population exportPop = PopulationUtils.createPopulation(ConfigUtils.createConfig());
		for (Person p : scenario.getPopulation().getPersons().values()){
			if (r.nextDouble()<scale){
				exportPop.addPerson(p);
			}
		}
		new PopulationWriter(exportPop).write(fullSamplePlansFile.replace(".xml.gz", "_"+scale+".xml.gz"));
		
	}
}
