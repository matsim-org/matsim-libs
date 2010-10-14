/* *********************************************************************** *
 * project: org.matsim.*
 * DgActivities2ShapeWriter
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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
package playground.dgrether.prognose2025;

import java.io.BufferedWriter;
import java.io.IOException;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.utils.io.IOUtils;

import playground.dgrether.DgPaths;

/**
 * @author dgrether
 * 
 */
public class DgActivities2TxtWriter {

	public void writeShape(String filename, Population pop) throws IOException {
		BufferedWriter writer = IOUtils.getBufferedWriter(filename);
		writer.write("id\tx_home\ty_home\tx_work\ty_work\tx_home2\ty_home2");
		writer.newLine();
		for (Person person : pop.getPersons().values()) {
			writer.write(person.getId().toString());
			writer.write("\t");
			Plan plan = person.getPlans().get(0);
			for (PlanElement pe : plan.getPlanElements()) {
				if (pe instanceof Activity) {
					Activity act = (Activity) pe;
//					writer.write(Double.toString(act.getCoord().getX()).replace(".", ","));
					writer.write(Double.toString(act.getCoord().getX()));
					writer.write("\t");
					writer.write(Double.toString(act.getCoord().getY()));
					writer.write("\t");
				}
			}
			writer.newLine();
		}
		writer.close();
	}
	
	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		Scenario scenario = new ScenarioImpl();
		MatsimPopulationReader reader = new MatsimPopulationReader(scenario);
		reader.readFile(DgPaths.REPOS
				+ "shared-svn/studies/countries/de/prognose_2025/demand/population_pv_1pct.xml");
		new DgActivities2TxtWriter().writeShape(DgPaths.REPOS
				+ "shared-svn/studies/countries/de/prognose_2025/demand/population_pv_1pct.txt", scenario
				.getPopulation());

	}

}
