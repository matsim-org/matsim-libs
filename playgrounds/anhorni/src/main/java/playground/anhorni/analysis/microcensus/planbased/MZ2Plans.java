/* *********************************************************************** *
 * project: org.matsim.*
 * MZ2Plans.java
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

package playground.anhorni.analysis.microcensus.planbased;

import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.PopulationWriter;

public class MZ2Plans {

	//////////////////////////////////////////////////////////////////////
	// test run 01
	//////////////////////////////////////////////////////////////////////

	public static void createMZ2Plans() throws Exception {

		System.out.println("MATSim-DB: create Population based on micro census 2005 data.");

		//////////////////////////////////////////////////////////////////////

		System.out.println("  extracting input directory... ");
		String indir = "src/main/java/playground/anhorni/input/microcensus/";
		String outdir = "src/main/java/playground/anhorni/output/microcensus/";

		//////////////////////////////////////////////////////////////////////

		System.out.println("  creating plans object... ");
		Population plans = new ScenarioImpl().getPopulation();
		System.out.println("  done.");

		System.out.println("  running plans modules... ");
		new PlansCreateFromMZ(indir+"/Wege_merged01112010.dat",outdir+"/Wege_merged01112010.dat",1,7).run(plans);
//		new PlansCreateFromMZ(indir+"/wegeketten_new.dat",outdir+"/output_wegeketten_new.dat",1,5).run(plans);
//		new PlansCreateFromMZ(indir+"/wegeketten_new.dat",outdir+"/output_wegeketten_new.dat",6,6).run(plans);
//		new PlansCreateFromMZ(indir+"/wegeketten_new.dat",outdir+"/output_wegeketten_new.dat",6,7).run(plans);
//		new PlansCreateFromMZ(indir+"/wegeketten_new.dat",outdir+"/output_wegeketten_new.dat",7,7).run(plans);
		System.out.println("  done.");

		//////////////////////////////////////////////////////////////////////

		System.out.println("  writing plans xml file... ");
		new PopulationWriter(plans, NetworkImpl.createNetwork()).write(outdir + "plansMOSO.xml.gz");
		System.out.println("  done.");
		
		System.out.println("-------------------------------------------------------------");
		System.out.println("Analyzing MC: ... ");
		AnalyzeMicrocensus analyzer = new AnalyzeMicrocensus();
		analyzer.run("car", outdir + "plansMOSO.xml.gz", "l");
		analyzer.run("car", outdir + "plansMOSO.xml.gz", "s");
		System.out.println("-------------------------------------------------------------");
	}

	//////////////////////////////////////////////////////////////////////
	// main
	//////////////////////////////////////////////////////////////////////

	public static void main(final String[] args) throws Exception {
		Gbl.startMeasurement();
		createMZ2Plans();
		Gbl.printElapsedTime();
	}
}
