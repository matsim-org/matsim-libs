/* *********************************************************************** *
 * project: org.matsim.*
 * RunTestRoad.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package playground.benjamin.szenarios.munich.testroad;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.MatsimConfigReader;
import org.matsim.core.controler.Controler;

/**
 * @author benjamin
 *
 */
public class RunTestRoad {

	static String inputPath = "../../detailedEval/teststrecke/sim/input/";
	static String outputPath = "../../detailedEval/teststrecke/sim/output/";
	static String configName = "multipleRunsConfig.xml";

	static Id enterLinkId = new IdImpl("592536888");
	static Id leaveLinkId = new IdImpl("590000822");

	static Integer leaveLinkCapacity = 1400;

	static Integer [] days = {
		20060127,
		20060131,
		20090317,
		20090318,
		20090319,
		20090707,
		20090708,
		20090709
	};

	public static void main(String[] args) {

		for(int day : days){
			Config config = new Config();
			config.addCoreModules();
			MatsimConfigReader confReader = new MatsimConfigReader(config);
			confReader.readFile(inputPath + configName);
			Controler controler = new Controler(config);

			// controler settings	
			controler.setOverwriteFiles(true);
			controler.setCreateGraphs(false);

			// config settings
			config.controler().setOutputDirectory(outputPath + day + "/");
			config.controler().setFirstIteration(0);
			config.controler().setLastIteration(0);
			config.network().setInputFile(inputPath + "network.xml");
			config.network().setTimeVariantNetwork(true);
			config.network().setChangeEventInputFile(inputPath + "capacityChanges.xml");
			config.plans().setInputFile(inputPath + day + "_plans.xml.gz");

			Scenario scenario = controler.getScenario();

			controler.addControlerListener(new SimpleControlerListener(scenario, enterLinkId, leaveLinkId, leaveLinkCapacity));

			controler.run();
		}

	}

}
