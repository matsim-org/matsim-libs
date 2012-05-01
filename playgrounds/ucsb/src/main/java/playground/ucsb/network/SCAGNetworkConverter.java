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

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.network.algorithms.MultimodalNetworkCleaner;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.network.algorithms.NetworkWriteAsTable;
import org.matsim.core.scenario.ScenarioUtils;
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

//		String localInBase = "D:/balmermi/documents/eclipse/input/raw/america/usa/losAngeles/UCSB/0000";
//		String localOutBase = "D:/balmermi/documents/eclipse/output/ucsb";
//
//		args = new String[] {
//				localInBase+"/network/scagnetwork/scagnetworknode_Project_UTM_Zone_11N.shp",
//				localInBase+"/network/scagnetwork/scagnetworknodelink_Project_UTM_Zone_11N.shp",
//				localOutBase+"/scagnetwork"
//		};

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
		ObjectAttributes linkObjectAttributes = new ObjectAttributes();

		new SCAGShp2Nodes(nodeShpFile, nodeObjectAttributes).run(scenario.getNetwork());
		new SCAGShp2Links(linkShpFile, linkObjectAttributes).run(scenario.getNetwork());
		
		String base = outputBase+"/complete";
		if (!(new File(base).mkdir())) { throw new RuntimeException("Could not create "+base); }
		new NetworkWriter(scenario.getNetwork()).write(base+"/network.xml.gz");
		new NetworkWriteAsTable(base,10.0).run(scenario.getNetwork());
		new ObjectAttributesXmlWriter(nodeObjectAttributes).writeFile(base+"/nodeObjectAttributes.xml.gz");
		new ObjectAttributesXmlWriter(linkObjectAttributes).writeFile(base+"/linkObjectAttributes.xml.gz");
		
		Set<String> modes = new HashSet<String>();
		modes.add(TransportMode.car);
		new MultimodalNetworkCleaner(scenario.getNetwork()).run(modes);
		modes.clear(); modes.add(SCAGShp2Links.HOV);
		new MultimodalNetworkCleaner(scenario.getNetwork()).run(modes);
		new NetworkCleaner().run(scenario.getNetwork());

		base = outputBase+"/cleaned";
		if (!(new File(base).mkdir())) { throw new RuntimeException("Could not create "+base); }
		new NetworkWriter(scenario.getNetwork()).write(base+"/network.xml.gz");
		new NetworkWriteAsTable(base,10.0).run(scenario.getNetwork());
		new ObjectAttributesXmlWriter(nodeObjectAttributes).writeFile(base+"/nodeObjectAttributes.xml.gz");
		new ObjectAttributesXmlWriter(linkObjectAttributes).writeFile(base+"/linkObjectAttributes.xml.gz");
	}

}
