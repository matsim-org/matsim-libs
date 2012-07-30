/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.acmarmol.microcensus2010;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;

public class MZ2010ToXmlFiles {

	
	public static void createXmls() throws Exception {
		
	System.out.println("MATSim-DB: creating xml files from MicroCensus 2010 database");
	
	System.out.println("  creating scenario object... ");
	Scenario scenario = ((ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig()));
	System.out.println("  done.");
	

	//household and vehicles xmls...
	new HouseholdsFromMZ("input/Microcensus2010/haushalte.dat", "input/Microcensus2010/haushaltspersonen.dat", "input/Microcensus2010/fahrzeuge.dat").run();
	
	//population xmls...
	new PopulationFromMZ("input/Microcensus2010/zielpersonen.dat","input/Microcensus2010/wege.dat").run(scenario.getPopulation());
		
	}
	
		
	//////////////////////////////////////////////////////////////////////
	// main
	//////////////////////////////////////////////////////////////////////

	public static void main(final String[] args) throws Exception {
		Gbl.startMeasurement();

		createXmls();

		Gbl.printElapsedTime();
	}
	
	
}
