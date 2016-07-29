/* *********************************************************************** *
 * project: org.matsim.*
 * Events2TTMatrix.java
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

package playground.toronto.maneuvers;

import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.network.io.NetworkWriter;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;


public class ManeuverCreation {
	
	public static void main(String[] args) throws Exception {
		// input arguments
		if (args.length != 6) {
			System.out.println("Usage: ManeuverCreation XMLnetwork TXTmaneuvers outputdir removeUTurns linkSeparation expansionRadius");
			System.out.println("       XMLnetwork:   MATSim xml network");
			System.out.println("       TXTmaneuvers: tab separated text file of the maneuver restrictions from EMME");
			System.out.println("       outputdir:  where to store the output");
			System.out.println("       removeUTurns [boolean]: if 'true', U-turns will be removed; if ''false, U-turns will remain");
			System.out.println("       linkSeparation [double]>=0: offset between parallel links");
			System.out.println("       expansionRadius [double]>=0: defines the size of the expanding intersection");
			System.out.println();
			System.out.println("----------------");
			System.out.println("2008, matsim.org");
			System.out.println();
			System.exit(-1);
		}

		String networkfile = args[0];
		String maneuverfile = args[1];
		String outputdir = args[2];
		NetworkAddEmmeManeuverRestrictions naemr = new NetworkAddEmmeManeuverRestrictions(maneuverfile);
		naemr.removeUTurns = Boolean.parseBoolean(args[3]);
		naemr.linkSeparation = Double.parseDouble(args[4]);
		naemr.expansionRadius = Double.parseDouble(args[5]);
		System.out.println("Arguments:");
		System.out.println("  XMLnetwork: "+networkfile);
		System.out.println("  TXTevents:  "+maneuverfile);
		System.out.println("  outputdir:  "+outputdir);
		naemr.printInfo("");
		
		MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Network network = scenario.getNetwork();
		new MatsimNetworkReader(scenario.getNetwork()).readFile(networkfile);
		naemr.run(network);
		new NetworkCleaner().run(network);

		System.out.println("writing xml file...");
		new NetworkWriter(network).write(outputdir+"/output_network.xml.gz");
		System.out.println("done.");

//		System.out.println("writing txt files...");
//		NetworkWriteAsTable nwat = new NetworkWriteAsTable(outputdir);
//		nwat.run(network);
//		nwat.close();
//		System.out.println("done.");
	}
}
