/* *********************************************************************** *
 * project: org.matsim.*
 * LinkLeaveEventHandler.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package playground.balmermi.toggenburg;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.scenario.ScenarioLoaderImpl;

import playground.balmermi.toggenburg.modules.PopulationAnalysis;


public class Analysis {

	//////////////////////////////////////////////////////////////////////
	// variables
	//////////////////////////////////////////////////////////////////////

	private final static Logger log = Logger.getLogger(Analysis.class);

	
	//////////////////////////////////////////////////////////////////////
	// parse File 
	//////////////////////////////////////////////////////////////////////

	private static final Set<IdImpl> setLinkSet(String infile) {
		Set<IdImpl> linkSet = new HashSet<IdImpl>();
		try {
			FileReader fr = new FileReader(infile);
			BufferedReader br = new BufferedReader(fr);

			// Skip header
			String curr_line = br.readLine();
			while ((curr_line = br.readLine()) != null) {
				String[] entries = curr_line.split("\t", -1);
				// LinkId
				// 0
				IdImpl linkId = new IdImpl(entries[0]);
				linkSet.add(linkId);
			}
		} catch (IOException e) {
			Gbl.errorMsg(e);
		}
		
		return linkSet;
	}
	
	//////////////////////////////////////////////////////////////////////
	// main
	//////////////////////////////////////////////////////////////////////

	public static void main(String[] args) {
		
		log.info("loading scenario...");
		Scenario scenario = new ScenarioLoaderImpl(args[0]).loadScenario();
		log.info("done.");
		
		log.info("extracting output directory... ");
		String outdir = scenario.getConfig().facilities().getOutputFile();
		outdir = outdir.substring(0,outdir.lastIndexOf("/"));
		log.info("=> "+outdir);
		log.info("done.");
		
		new PopulationAnalysis().run(scenario.getPopulation(),setLinkSet(args[1]));
	}
}
