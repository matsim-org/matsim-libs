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

package playground.dziemke.cemdap2matsim.multiple;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkReaderMatsimV1;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.population.algorithms.XY2Links;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlWriter;
import org.opengis.feature.simple.SimpleFeature;



/**
 * @author dziemke
 * @see balmermi: UCSBStops2PlansConverter.java
 *
 */
public class CemdapStops2MatsimPlansConverter {

	private final static Logger log = Logger.getLogger(CemdapStops2MatsimPlansConverter.class);

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		String cemdapStopsFile = "D:/Workspace/CEMDAP_Test_Version/Output/30/stops.out1";
		String cemdapStopsFile2 = "D:/Workspace/CEMDAP_Test_Version/Output/31/stops.out1";
		String cemdapStopsFile3 = "D:/Workspace/CEMDAP_Test_Version/Output/32/stops.out1";
		
		String tazShapeFile = "D:/Workspace/container/demand/input/shapefiles/gemeindenLOR_DHDN_GK4.shp";
		String networkFile = "D:/Workspace/berlin/counts/iv_counts/network-base_ext.xml";
		
		Double popFraction = 1.0;
		String outputBase = "D:/Workspace/container/demand/input/cemdap2matsim/11";

		
		// print input parameters
		log.info("cemdapStopsFile: "+cemdapStopsFile);
		log.info("cemdapStopsFile2: "+cemdapStopsFile2);
		log.info("cemdapStopsFile3: "+cemdapStopsFile3);
				
		log.info("tazShapeFile: "+tazShapeFile);
		log.info("networkFile: "+networkFile);
		log.info("popFraction: "+popFraction);
		log.info("outputBase: "+outputBase);

				
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		ObjectAttributes personObjectAttributes = new ObjectAttributes();
		ObjectAttributes personObjectAttributes2 = new ObjectAttributes();
		ObjectAttributes personObjectAttributes3 = new ObjectAttributes();
		
		
		log.info("parsing network data...");
		new NetworkReaderMatsimV1(scenario).parse(networkFile);
		log.info("done. (parsing)");

				
		log.info("reading "+tazShapeFile+" file for cemdap...");
		Map<String,SimpleFeature> cemdapTazFeatures = new HashMap<String, SimpleFeature>();
		for (SimpleFeature feature: ShapeFileReader.getAllFeatures(tazShapeFile)) {
			Integer schluessel = Integer.parseInt((String) feature.getAttribute("NR"));
			String id = schluessel.toString();
			cemdapTazFeatures.put(id,feature);
		}
		log.info(cemdapTazFeatures.size()+" features stored.");
		log.info("done. (reading)");
		
		
		// ----------- START FIRST PLAN ---------------
		log.info("parsing "+cemdapStopsFile+" file...");
		int planNumber = 0;
		new CemdapStopsParser().parse(cemdapStopsFile, planNumber, scenario, personObjectAttributes, popFraction);
		log.info("done. (parsing)");
		
		log.info("assigning coordinates to activities...");
		new Feature2Coord().assignCoords(scenario, planNumber, personObjectAttributes, cemdapTazFeatures);
		log.info("done. (assigning)");
		// ----------- END FIRST PLAN ---------------
		
		
		// ----------- START Second PLAN ---------------
		log.info("parsing "+cemdapStopsFile2+" file...");
		planNumber = 1;
		new CemdapStopsParser().parse(cemdapStopsFile2, planNumber, scenario, personObjectAttributes2, popFraction);
		log.info("done. (parsing)");

		log.info("assigning coordinates to activities...");
		new Feature2Coord().assignCoords(scenario, planNumber, personObjectAttributes2, cemdapTazFeatures);
		log.info("done. (assigning)");
		// ----------- END SECOND PLAN ---------------
		
		
		// ----------- START THIRD PLAN ---------------
		log.info("parsing "+cemdapStopsFile3+" file...");
		planNumber = 2;
		new CemdapStopsParser().parse(cemdapStopsFile3, planNumber, scenario, personObjectAttributes3, popFraction);
		log.info("done. (parsing)");

		log.info("assigning coordinates to activities...");
		new Feature2Coord().assignCoords(scenario, planNumber, personObjectAttributes3, cemdapTazFeatures);
		log.info("done. (assigning)");
		// ----------- END THIRD PLAN ---------------
		
		
		// new
		log.info("checking number of plans...");
		int counter = 0;
		for (Person person : scenario.getPopulation().getPersons().values()) {
			if (person.getPlans().size() < 3) {
				log.warn("Person with ID=" + person.getId() + " has less than three plans");
			}
			if (person.getPlans().size() > 3) {
				log.warn("Person with ID=" + person.getId() + " has more than three plans");
				}
			if (person.getPlans().size() == 3) {
				counter++;
			}
		}
		log.info(counter + "persons have three plans.");
		//
		
		
		log.info("assigning activities to links...");
		new XY2Links((ScenarioImpl)scenario).run(scenario.getPopulation());
		log.info("done. (assigning)");
		

		log.info("writing data to "+outputBase+"...");
		new PopulationWriter(scenario.getPopulation(), null).write(outputBase+"/plans.xml.gz");
//		new ObjectAttributesXmlWriter(personObjectAttributes).writeFile(outputBase+"/personObjectAttributes.xml.gz");
//		new ObjectAttributesXmlWriter(personObjectAttributes2).writeFile(outputBase+"/personObjectAttributes2.xml.gz");
		log.info("done. (writing)");
	}

}
