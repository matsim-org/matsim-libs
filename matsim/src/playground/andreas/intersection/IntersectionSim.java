/* *********************************************************************** *
 * project: org.matsim.*
 * ItsumoSim.java
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

package playground.andreas.intersection;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;

import org.matsim.config.ConfigWriter;
import org.matsim.controler.Controler;
import org.matsim.events.Events;
import org.matsim.gbl.Gbl;
import org.matsim.mobsim.ExternalMobsim;
import org.matsim.network.Link;
import org.matsim.plans.Act;
import org.matsim.plans.Leg;
import org.matsim.plans.Person;
import org.matsim.plans.Plan;
import org.matsim.plans.Plans;
import org.matsim.plans.Route;
import org.matsim.utils.io.IOUtils;

import playground.andreas.itsumo.ItsumoSim;

public class IntersectionSim extends ExternalMobsim {

	protected static final String CONFIG_MODULE = "intersection";

	public IntersectionSim(final Plans population, final Events events) {
		super(population, events);
		System.out.println("\n##################################################################################################\n" +
				"#   REMINDER - Header in writeItsumoConfig has to be changed\n" +
		"#              according to the itsumo scenario description file." +
		"\n##################################################################################################\n");
	}

	@Override
	public void run() {
		// ONLY reason why this needs to be overridden is because of the different events file name !!
		// Since it new exists, writeItsumoConfig is different from writeConfig.
		
		String iterationPlansFile = Controler.getIterationFilename(this.plansFileName);
//		String iterationEventsFile = Controler.getIterationFilename(this.eventsFileName);
		String iterationEventsFile = "./drivers.txt" ;
		String iterationConfigFile = Controler.getIterationFilename(this.configFileName);

		try {
			writeConfig( iterationPlansFile, iterationEventsFile, "output/config.xml" ) ;
			writeIntersectionConfig( iterationConfigFile ) ;
			runExe(iterationConfigFile);
			readEvents(iterationEventsFile);
		}
		catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	protected void writeIntersectionConfig(String outFileName ) throws FileNotFoundException, IOException {
		System.out.println("copying Gbl.getConfig() into config file for intersection mobsim at " + (new Date()));

		BufferedWriter out = null ;
		try {
			out = IOUtils.getBufferedWriter( outFileName ) ;
			
			ConfigWriter configwriter = new ConfigWriter(Gbl.getConfig(), out);
			configwriter.write();
			
//			out.write(Gbl.getConfig().config().toString());
			out.flush();
			
		} catch (IOException e) {
			Gbl.errorMsg(e);
		}

	}
	
}
