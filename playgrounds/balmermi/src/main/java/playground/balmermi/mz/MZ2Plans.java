/* *********************************************************************** *
 * project: org.matsim.*
 * ScenarioParsing.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package playground.balmermi.mz;

import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.utils.misc.ConfigUtils;

public class MZ2Plans {

	//////////////////////////////////////////////////////////////////////
	// test run 01
	//////////////////////////////////////////////////////////////////////

	public static void createMZ2Plans(Config config) throws Exception {

		System.out.println("MATSim-DB: create Population based on micro census 2005 data.");

		//////////////////////////////////////////////////////////////////////

		System.out.println("  extracting input directory... ");
		String indir = config.plans().getInputFile();
		indir = indir.substring(0,indir.lastIndexOf("/"));
		System.out.println("    "+indir);
		System.out.println("  done.");

		System.out.println("  extracting output directory... ");
		String outdir = null;//config.plans().getOutputFile();
		outdir = outdir.substring(0,outdir.lastIndexOf("/"));
		System.out.println("    "+outdir);
		System.out.println("  done.");

		//////////////////////////////////////////////////////////////////////

		System.out.println("  creating plans object... ");
		Population plans = new ScenarioImpl().getPopulation();
		System.out.println("  done.");

		System.out.println("  running plans modules... ");
		new PlansCreateFromMZ(indir+"/wegeketten_new.dat",outdir+"/output_wegeketten_new.dat",1,7).run(plans);
//		new PlansCreateFromMZ(indir+"/wegeketten_new.dat",outdir+"/output_wegeketten_new.dat",1,5).run(plans);
//		new PlansCreateFromMZ(indir+"/wegeketten_new.dat",outdir+"/output_wegeketten_new.dat",6,6).run(plans);
//		new PlansCreateFromMZ(indir+"/wegeketten_new.dat",outdir+"/output_wegeketten_new.dat",6,7).run(plans);
//		new PlansCreateFromMZ(indir+"/wegeketten_new.dat",outdir+"/output_wegeketten_new.dat",7,7).run(plans);
		System.out.println("  done.");

		//////////////////////////////////////////////////////////////////////

		System.out.println("  writing plans xml file... ");
		new PopulationWriter(plans, null).write(null);//config.plans().getOutputFile());
		System.out.println("  done.");

//		System.out.println("  writing config xml file... ");
//		ConfigWriter config_writer = new ConfigWriter(Gbl.getConfig());
//		config_writer.write();
//		System.out.println("  done.");

		System.out.println("done.");
		System.out.println();
	}

	//////////////////////////////////////////////////////////////////////
	// main
	//////////////////////////////////////////////////////////////////////

	public static void main(final String[] args) throws Exception {
		Gbl.startMeasurement();

		Config config = ConfigUtils.loadConfig(args[0]);
		createMZ2Plans(config);

		Gbl.printElapsedTime();
	}
}
