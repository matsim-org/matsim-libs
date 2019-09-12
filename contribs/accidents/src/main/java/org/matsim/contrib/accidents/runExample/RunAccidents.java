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

package org.matsim.contrib.accidents.runExample;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.accidents.AccidentsConfigGroup;
import org.matsim.contrib.accidents.AccidentsModule;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;

/**
* @author ikaddoura, mmayobre
*/

public class RunAccidents {
	private static final Logger log = Logger.getLogger(RunAccidents.class);
		
	public static void main(String[] args) throws IOException {		
		RunAccidents main = new RunAccidents();
		main.run();
	}

	private void run() {
		log.info("Loading scenario...");
		
		String configFile = "path/to/configFile.xml";
		
		Config config = ConfigUtils.loadConfig(configFile );
		
		AccidentsConfigGroup accidentsSettings = ConfigUtils.addOrGetModule(config, AccidentsConfigGroup.class);
		accidentsSettings.setEnableAccidentsModule(true);
		
		final Scenario scenario = ScenarioUtils.loadScenario(config);
		
		// Preprocess network
		AccidentsNetworkModification networkModification = new AccidentsNetworkModification(scenario);
		
		String[] tunnelLinks = readCSVFile("tunnelLinksCSVfile");
		String[] planfreeLinks = readCSVFile("planfreeLinksCSVfile");
				
		networkModification.setLinkAttributsBasedOnOSMFile("osmlandUseFile", "osmPopDensityFile", "EPSG:31468" , tunnelLinks, planfreeLinks );
		
		Controler controler = new Controler(scenario);
		controler.addOverridingModule(new AccidentsModule());
				
		controler.getConfig().controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
		controler.run();
	}
	
	private String[] readCSVFile(String csvFile) {
		ArrayList<Id<Link>> links = new ArrayList<>();

		BufferedReader br = IOUtils.getBufferedReader(csvFile);
		
		String line = null;
		try {
			line = br.readLine();
		} catch (IOException e) {
			e.printStackTrace();
		}

		try {
			int countWarning = 0;
			while ((line = br.readLine()) != null) {
				
				String[] columns = line.split(";");
				Id<Link> linkId = null;
				for (int column = 0; column < columns.length; column++) {
					if (column == 0) {
						linkId = Id.createLinkId(columns[column]);
					} else {
						if (countWarning < 1) {
							log.warn("Expecting the link Id to be in the first column. Ignoring further columns...");
						} else if (countWarning == 1) {
							log.warn("This message is only given once.");
						}
						countWarning++;
					}						
				}
				log.info("Adding link ID " + linkId);
				links.add(linkId);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		String[] linkIDsArray = (String[]) links.toArray();
		return linkIDsArray ;
	}

}

