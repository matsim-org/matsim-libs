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

import java.io.IOException;
import java.util.Map;

import org.apache.log4j.Logger;
import org.geotools.feature.Feature;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.ConfigUtils;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlWriter;

import playground.ucsb.UCSBUtils;

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
		
//		args = new String[] {
//				"D:/sandboxSenozon/senozon/data/raw/america/usa/losAngeles/UCSB/demand/CEMDAP/stops_total_actual.dat.gz",
//				"D:/sandboxSenozon/senozon/data/raw/america/usa/losAngeles/UCSB/geographics/TAZ/taz.shp",
//				"TAZ2K",
//				"0.001",
//				"D:/balmermi/documents/eclipse/output/ucsb"
//		};

		if (args.length < 4) {
			log.error("UCSBStops2PlansConverter cemdapStopsFile tazShapeFile tazIdName popFraction outputBase");
			System.exit(-1);
		}
		
		// store input parameters
		String cemdapStopsFile = args[0];
		String tazShapeFile = args[1];
		String tazIdName = args[2];
		Double popFraction = Double.parseDouble(args[3]);
		String outputBase = args[4];

		// print input parameters
		log.info("cemdapStopsFile: "+cemdapStopsFile);
		log.info("tazShapeFile: "+tazShapeFile);
		log.info("tazIdName: "+tazIdName);
		log.info("popFraction: "+popFraction);
		log.info("outputBase: "+outputBase);
		
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		ObjectAttributes personObjectAttributes = new ObjectAttributes();

		log.info("parsing "+cemdapStopsFile+" file...");
		new UCSBStopsParser().parse(cemdapStopsFile, scenario, personObjectAttributes, popFraction);
		log.info("done. (parsing)");

		log.info("reading "+tazShapeFile+" file...");
		Map<String,Feature> features = UCSBUtils.getFeatureMap(tazShapeFile, tazIdName);
		log.info("done. (reading)");

		log.info("assigning coordinates to activities...");
		new UCSBTAZ2Coord().assignCoords(scenario, personObjectAttributes, features);
		log.info("done. (assigning)");

		log.info("writing data to "+outputBase+"...");
		new PopulationWriter(scenario.getPopulation(), null).write(outputBase+"/plans.xml.gz");
		new ObjectAttributesXmlWriter(personObjectAttributes).writeFile(outputBase+"/personObjectAttributes.xml.gz");
		log.info("done. (writing)");
	}

}
