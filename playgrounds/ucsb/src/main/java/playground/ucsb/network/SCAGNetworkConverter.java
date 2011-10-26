/* *********************************************************************** *
 * project: org.matsim.*
 * SCAGNetworkConverter.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package playground.ucsb.network;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.network.algorithms.NetworkWriteAsTable;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.ConfigUtils;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlWriter;

import playground.ucsb.network.algorithms.SCAGShp2Links;
import playground.ucsb.network.algorithms.SCAGShp2Nodes;

/**
 * @author balmermi
 *
 */
public class SCAGNetworkConverter {

	private final static Logger log = Logger.getLogger(SCAGNetworkConverter.class);

	/**
	 * @param args
	 */
	public static void main(String[] args) {

		args = new String[] {
				"D:/sandboxSenozon/senozon/data/raw/america/usa/losAngeles/UCSB/scagnetwork/scagnetworknode_Project_UTM_Zone_11N.shp",
				"D:/sandboxSenozon/senozon/data/raw/america/usa/losAngeles/UCSB/scagnetwork/scagnetworknodelink_Project_UTM_Zone_11N.shp",
				"D:/balmermi/documents/eclipse/output/ucsb"
		};

		if (args.length != 3) {
			log.error("SCAGNetworkConverter nodeShpFile linkShpFile outputBase");
			System.exit(-1);
		}
		
		// store input parameters
		String nodeShpFile = args[0];
		String linkShpFile = args[1];
		String outputBase = args[2];

		// print input parameters
		log.info("nodeShpFile: "+nodeShpFile);
		log.info("linkShpFile: "+linkShpFile);
		log.info("outputBase: "+outputBase);
		
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		ObjectAttributes nodeObjectAttributes = new ObjectAttributes();
		new SCAGShp2Nodes(nodeShpFile, nodeObjectAttributes).run(scenario.getNetwork());
		new SCAGShp2Links().run(scenario.getNetwork());
		new NetworkWriter(scenario.getNetwork()).write(outputBase+"/network.xml.gz");
		new NetworkWriteAsTable(outputBase).run(scenario.getNetwork());
		new ObjectAttributesXmlWriter(nodeObjectAttributes).writeFile(outputBase+"/nodeObjectAttributes.xml.gz");
	}

}
