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
package playground.fhuelsmann.airpollution.testroad;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.config.Config;
import org.matsim.core.config.MatsimConfigReader;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;

/**
 * @author benjamin
 *
 */
public class RunTestRoad {

	static String inputPath = "../../detailedEval/teststreckeLandshuterAllee/sim/input/innen_nord_fuzzify/";
	static String outputPath = "../../detailedEval/teststreckeLandshuterAllee/sim/output/innen_nord_fuzzify/";
	static String configName = "multipleRunsConfig.xml";

	//suedfuzzify
//	static Id enterLinkId = new IdImpl("52804319-594971775");
//	static Id leaveLinkId = new IdImpl("52804319-594971775");
//	static Id enterLinkId = new IdImpl("52804333-52804338-53271504-52804341-53271503");
//	static Id leaveLinkId = new IdImpl("52804333-52804338-53271504-52804341-53271503");
//nord_fuzzify
		static Id<Link> enterLinkId = Id.create("592627223-52804320", Link.class);
	static Id<Link> leaveLinkId = Id.create("592627223-52804320", Link.class);
//	static Id enterLinkId = new IdImpl("52804339-52804334");
//	static Id leaveLinkId = new IdImpl("52804339-52804334");

	//static Integer leaveLinkCapacity = 1500;
	static Integer leaveLinkCapacity = 2742;

	static Integer [] days = {
	//	20080107,
	//	20080108,
	//	20080109,
	//	20080110,
	//	20080111,
	//	20080707,
		20080708,
	//	20080709,
	//	20080710,
	//	20080711
	};

	public static void main(String[] args) {

		for(int day : days){
			Config config = new Config();
			config.addCoreModules();
			MatsimConfigReader confReader = new MatsimConfigReader(config);
			confReader.readFile(inputPath + configName);
			Controler controler = new Controler(config);

			// controler settings	
			controler.getConfig().controler().setOverwriteFileSetting(
					true ?
							OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles :
							OutputDirectoryHierarchy.OverwriteFileSetting.failIfDirectoryExists );
			controler.getConfig().controler().setCreateGraphs(false);

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
