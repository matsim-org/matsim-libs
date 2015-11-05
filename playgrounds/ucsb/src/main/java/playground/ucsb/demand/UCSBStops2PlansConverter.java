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
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.population.algorithms.XY2Links;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlReader;
import org.matsim.utils.objectattributes.ObjectAttributesXmlWriter;
import org.opengis.feature.simple.SimpleFeature;

import playground.ucsb.UCSBUtils;
import playground.ucsb.network.algorithms.SCAGShp2Links;
import playground.ucsb.singleTrips.UCSBSingleTripsConverter;

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
//				localInBase+"/demand/goods_trips",
//				localInBase+"/geographics/TAZ/taz_Project_UTM_Zone_11N.shp",
//				"TAZ2K",
//				"ID",
//				localOutBase+"/scagnetwork/cleaned/network.xml.gz",
//				localOutBase+"/scagnetwork/cleaned/linkObjectAttributes.xml.gz",
//				"0.001",
//				localOutBase+"/demand"
//		};

		if (args.length != 9) {
			log.error("UCSBStops2PlansConverter cemdapStopsFile inputBaseGoods tazShapeFile tazCemdapIdName tazGoodsIdName networkFile linkObjectAttributeFile popFraction outputBase");
			System.exit(-1);
		}
		
		// store input parameters
		String cemdapStopsFile = args[0];
		String inputBaseGoods = args[1];
		String tazShapeFile = args[2];
		String tazCemdapIdName = args[3];
		String tazGoodsIdName = args[4];
		String networkFile = args[5];
		String linkObjectAttributeFile = args[6];
		Double popFraction = Double.parseDouble(args[7]);
		String outputBase = args[8];

		// print input parameters
		log.info("cemdapStopsFile: "+cemdapStopsFile);
		log.info("inputBaseGoods: "+inputBaseGoods);
		log.info("tazShapeFile: "+tazShapeFile);
		log.info("tazCemdapIdName: "+tazCemdapIdName);
		log.info("tazGoodsIdName: "+tazGoodsIdName);
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
		new NetworkWriteAsTable(base,10.0).run(scenario.getNetwork());
		log.info("done. (extracting)");

		log.info("parsing "+cemdapStopsFile+" file...");
		new UCSBStopsParser().parse(cemdapStopsFile, scenario, personObjectAttributes, popFraction);
		log.info("done. (parsing)");

		log.info("reading "+tazShapeFile+" file for cemdap...");
		Map<String,SimpleFeature> cemdapTazFeatures = UCSBUtils.getFeatureMap(tazShapeFile, tazCemdapIdName);
		log.info("done. (reading)");

		log.info("reading "+tazShapeFile+" file for goods...");
		Map<String,SimpleFeature> goodsTazFeatures = UCSBUtils.getFeatureMap(tazShapeFile, tazGoodsIdName);
		log.info("done. (reading)");

		log.info("assigning coordinates to activities...");
		new UCSBTAZ2Coord().assignCoords(scenario, personObjectAttributes, cemdapTazFeatures);
		log.info("done. (assigning)");
		
		log.info("reading goods trip matrices from "+inputBaseGoods+" ...");
		UCSBSingleTripsConverter converter = new UCSBSingleTripsConverter(goodsTazFeatures);
		File file = new File(inputBaseGoods);
		for (int i=0; i<file.list().length; i++) {
			if (file.list()[i].endsWith(".txt")) {
				converter.createPlansFromTripFile(inputBaseGoods+"/"+file.list()[i],scenario.getPopulation(),popFraction);
			}
		}
		log.info("done. (reading)");
		
		log.info("assigning activities to links...");
		new XY2Links((MutableScenario)scenario).run(scenario.getPopulation());
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
