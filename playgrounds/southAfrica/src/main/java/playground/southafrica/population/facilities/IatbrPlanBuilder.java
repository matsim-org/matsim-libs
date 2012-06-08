/* *********************************************************************** *
 * project: org.matsim.*
 * IatbrPlanBuilder.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.southafrica.population.facilities;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.facilities.FacilitiesReaderMatsimV1;
import org.matsim.core.network.NetworkReaderMatsimV1;
import org.matsim.core.population.PopulationReaderMatsimV5;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlReader;

public class IatbrPlanBuilder {
	private final static Logger LOG = Logger.getLogger(IatbrPlanBuilder.class);

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String networkFile = args[0];
		String plansFile = args[1];
		String facilitiesFile = args[2];
		String facilityAttributeFile = args[3];
		
		ScenarioImpl sc = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		
		/* READ THE VARIOUS INPUT FILES */
		/* Read network */
		NetworkReaderMatsimV1 nr = new NetworkReaderMatsimV1(sc);
		nr.parse(networkFile);
		/* Read plans */
		PopulationReaderMatsimV5 pr = new PopulationReaderMatsimV5(sc);
		pr.parse(plansFile);
		/* Read facilities */
		FacilitiesReaderMatsimV1 fr = new FacilitiesReaderMatsimV1(sc);
		fr.parse(facilitiesFile);
		/* Read facility attributes */
		ObjectAttributes facilityAttributes = new ObjectAttributes();
		ObjectAttributesXmlReader or = new ObjectAttributesXmlReader(facilityAttributes);
		or.parse(facilityAttributeFile);
		
		/* READ THE OSM AMENITIES */
		
		
		Controler controler = new Controler(sc	);
		

	}

}

