/* *********************************************************************** *
 * project: org.matsim.*
 * UCSBStops2PlansConverter.java
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

package playground.ucsb.demand;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.geotools.feature.Feature;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkReaderMatsimV1;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.network.algorithms.NetworkWriteAsTable;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.population.algorithms.XY2Links;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlReader;
import org.matsim.utils.objectattributes.ObjectAttributesXmlWriter;

import playground.ucsb.UCSBUtils;
import playground.ucsb.network.algorithms.SCAGShp2Links;

/**
 * @author balmermi
 *
 */
public class UCSBStops2PlansConverter {

	private final static Logger log = Logger.getLogger(UCSBStops2PlansConverter.class);

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		
//		String localInBase = "D:/balmermi/documents/eclipse/input/raw/america/usa/losAngeles/UCSB/0000";
//		String localOutBase = "D:/balmermi/documents/eclipse/output/ucsb";
//		args = new String[] {
//				localInBase+"/demand/CEMDAP/stops_total_actual.dat.gz",
//				localInBase+"/geographics/TAZ/taz_Project_UTM_Zone_11N.shp",
//				"TAZ2K",
//				localOutBase+"/scagnetwork/20120411/cleaned/network.xml.gz",
//				localOutBase+"/scagnetwork/20120411/cleaned/linkObjectAttributes.xml.gz",
//				"0.002",
//				localOutBase+"/demand"
//		};

		if (args.length != 7) {
			log.error("UCSBStops2PlansConverter cemdapStopsFile tazShapeFile tazIdName networkFile linkObjectAttributeFile popFraction outputBase");
			System.exit(-1);
		}
		
		// store input parameters
		String cemdapStopsFile = args[0];
		String tazShapeFile = args[1];
		String tazIdName = args[2];
		String networkFile = args[3];
		String linkObjectAttributeFile = args[4];
		Double popFraction = Double.parseDouble(args[5]);
		String outputBase = args[6];

		// print input parameters
		log.info("cemdapStopsFile: "+cemdapStopsFile);
		log.info("tazShapeFile: "+tazShapeFile);
		log.info("tazIdName: "+tazIdName);
		log.info("networkFile: "+networkFile);
		log.info("linkObjectAttributeFile: "+linkObjectAttributeFile);
		log.info("popFraction: "+popFraction);
		log.info("outputBase: "+outputBase);
		
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		ObjectAttributes personObjectAttributes = new ObjectAttributes();

		log.info("parsing network data...");
		new NetworkReaderMatsimV1(scenario).parse(networkFile);
		ObjectAttributes linkObjectAttributes = new ObjectAttributes();
		new ObjectAttributesXmlReader(linkObjectAttributes).parse(linkObjectAttributeFile);
		log.info("done. (parsing)");

		log.info("extracting subnetwork...");
		prepareNetworkForXY2Links(scenario.getNetwork(), linkObjectAttributes);
		String base = outputBase+"/subnet";
		if (!(new File(base).mkdir())) { throw new RuntimeException("Could not create "+base); }
		new NetworkWriter(scenario.getNetwork()).write(base+"/network.xml.gz");
		new NetworkWriteAsTable(base).run(scenario.getNetwork());
		log.info("done. (extracting)");

		log.info("parsing "+cemdapStopsFile+" file...");
		new UCSBStopsParser().parse(cemdapStopsFile, scenario, personObjectAttributes, popFraction);
		log.info("done. (parsing)");

		log.info("reading "+tazShapeFile+" file...");
		Map<String,Feature> features = UCSBUtils.getFeatureMap(tazShapeFile, tazIdName);
		log.info("done. (reading)");

		log.info("assigning coordinates to activities...");
		new UCSBTAZ2Coord().assignCoords(scenario, personObjectAttributes, features);
		log.info("done. (assigning)");
		
		log.info("assigning activities to links...");
		new XY2Links((ScenarioImpl)scenario).run(scenario.getPopulation());
		log.info("done. (assigning)");

		log.info("writing data to "+outputBase+"...");
		new PopulationWriter(scenario.getPopulation(), null).write(outputBase+"/plans.xml.gz");
		new ObjectAttributesXmlWriter(personObjectAttributes).writeFile(outputBase+"/personObjectAttributes.xml.gz");
		log.info("done. (writing)");

	}

	
	private static final void prepareNetworkForXY2Links(Network network, ObjectAttributes linkObjectAttributes) {
		Set<Id> toRemove = new HashSet<Id>();
		for (Link l : network.getLinks().values()) {
			if (!l.getAllowedModes().contains(TransportMode.car)) {
				toRemove.add(l.getId());
			}
			int linkType = (Integer)linkObjectAttributes.getAttribute(l.getId().toString(), SCAGShp2Links.LINK_TYPE);
			if ((linkType < 40) || (linkType >= 80)) {
				toRemove.add(l.getId());
			}
		}
		for (Id lid : toRemove) {
			network.removeLink(lid);
		}
		log.info(toRemove.size()+" links removed - "+network.getLinks().size()+" links remaining.");
		toRemove.clear();
		for (Node n : network.getNodes().values()) {
			if (n.getInLinks().isEmpty() && n.getOutLinks().isEmpty()) {
				toRemove.add(n.getId());
			}
		}
		for (Id nid : toRemove) {
			network.removeNode(nid);
		}
		log.info(toRemove.size()+" empty nodes removed - "+network.getNodes().size()+" nodes remaining.");
	}
}
