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
package playground.droeder.laemmer;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vis.otfvis.OTFFileWriterFactory;

/**
 * @author droeder
 *
 */
public class LRunner {
	private static final String DIR = "D:/VSP/projects/laemmer/4arms/";
	private static final String CONFIG = DIR + "config.xml";
	private static final String NET = DIR + "network.xml";
	private static final String P = DIR + "population_1440.xml";
	
	
	public static void main(String[] args){
		Config conf = ConfigUtils.loadConfig(CONFIG);
		conf.controler().setOutputDirectory(DIR + "out/");
		conf.network().setInputFile(NET);
		conf.plans().setInputFile(P);
		
		Scenario sc = ScenarioUtils.loadScenario(conf);
		
		Controler c = new Controler(sc);
		c.addSnapshotWriterFactory("otfvis", new OTFFileWriterFactory());
		
		c.setDumpDataAtEnd(true);
		c.setOverwriteFiles(true);
		QueueLengthHandler h = new QueueLengthHandler(sc.getNetwork());
		c.addControlerListener(h);
		c.run();
	}
	
}
