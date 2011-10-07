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

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.ConfigUtils;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlWriter;

/**
 * @author balmermi
 *
 */
public class UCSBStops2PlansConverter {

	private final static Logger log = Logger.getLogger(UCSBStops2PlansConverter.class);

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		args = new String[] {
				"D:/sandboxSenozon/senozon/data/raw/america/usa/losAngeles/UCSB/demand/CEMDAP/stops_total_actual_small.dat",
				"D:/balmermi/documents/eclipse/output/ucsb"
		};

		if (args.length < 2) {
			log.error("UCSBStops2PlansConverter cemdapStopsFile outputBase");
			System.exit(-1);
		}
		
		// store input parameters
		String cemdapStopsFile = args[0];
		String outputBase = args[1];

		// print input parameters
		log.info("cemdapStopsFile: "+cemdapStopsFile);
		log.info("outputBase: "+outputBase);
		
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		ObjectAttributes personObjectAttributes = new ObjectAttributes();
		new UCSBStopsParser().parse(cemdapStopsFile, scenario, personObjectAttributes);
		new PopulationWriter(scenario.getPopulation(),null).write(outputBase+"/plans.xml");
		new ObjectAttributesXmlWriter(personObjectAttributes).writeFile(outputBase+"/personObjectAttributes.xml");
	}

}
