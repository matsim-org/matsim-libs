/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package playground.anhorni.analysis.microcensus.planbased;

import org.matsim.api.core.v01.population.Population;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.config.ConfigUtils;

public class MZ2Plans {

	public static void createMZ2Plans(String indir, String outdir) throws Exception {

		System.out.println("MATSim-DB: create Population based on micro census 2005/2010 data.");

		System.out.println("  creating plans object... ");
		Population plans = ((ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig())).getPopulation();
		System.out.println("  done.");

		System.out.println("  running plans modules... ");
		new PlansCreateFromMZ(1, 7).run(plans, indir);
		new PlansCreateFromMZ(1, 7).run(plans, indir);
		System.out.println("  done.");

		System.out.println("  writing plans xml file... ");
		new PopulationWriter(plans, NetworkImpl.createNetwork()).write(outdir + "plansMOSO.xml.gz");
		System.out.println("  done.");
		
		System.out.println("-------------------------------------------------------------");
		System.out.println("Analyzing MC: ... ");
		AnalyzeMicrocensus analyzer = new AnalyzeMicrocensus();
		analyzer.run("car", "l", outdir + "plansMOSO.xml.gz", indir + "network.xml");
		analyzer.run("car", "s", outdir + "plansMOSO.xml.gz", indir + "network.xml");
		System.out.println("-------------------------------------------------------------");
	}

	public static void main(final String[] args) throws Exception {
		Gbl.startMeasurement();
		String indir = args[0];
		String outdir = args[1];
		createMZ2Plans(indir, outdir);
		Gbl.printElapsedTime();
	}
}
