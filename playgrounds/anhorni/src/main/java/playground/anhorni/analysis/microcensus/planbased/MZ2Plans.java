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

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;

public class MZ2Plans {	
	private final static Logger log = Logger.getLogger(MZ2Plans.class);

	public static void createMZ2Plans(String indir, String outdir) throws Exception {
		log.info("MATSim-DB: create Population based on micro census 2005/2010 data.");
		log.info("  creating plans object... ");
		Population plans = ((ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig())).getPopulation();
		log.info("  done.");

		log.info("  running plans modules... ");
		new PlansCreateFromMZ(1, 7).run(plans, indir);
		log.info("  done.");

		log.info("  writing plans xml file... ");
		new PopulationWriter(plans, NetworkImpl.createNetwork()).write(outdir + "plansMOSO.xml.gz");
		log.info("  done.");
		
		log.info("-------------------------------------------------------------");
		log.info("Analyzing MC: ... ");
		AnalyzeMicrocensus analyzer = new AnalyzeMicrocensus();
		analyzer.run("car", "l", outdir + "plansMOSO.xml.gz", indir + "network.xml");
		analyzer.run("car", "s", outdir + "plansMOSO.xml.gz", indir + "network.xml");
		log.info("-------------------------------------------------------------");
	}

	public static void main(final String[] args) throws Exception {
		Gbl.startMeasurement();
		String indir = args[0];
		String outdir = args[1];
		createMZ2Plans(indir, outdir);
		Gbl.printElapsedTime();
	}
}
