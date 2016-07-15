/* *********************************************************************** *
 * project: org.matsim.*
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

package playground.mrieser.svi.converters;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;

import playground.mrieser.svi.data.ShapeZonesReader;
import playground.mrieser.svi.data.Zones;

/**
 * @author mrieser / senozon
 */
public class Main {

	public static void main(final String[] args) {
		convertNetwork(
				"/Volumes/Data/virtualbox/rdc_exchange/svidata/morgenspitze_matsim/network.car.xml.gz",
				"/Volumes/Data/virtualbox/exchange/bellevue");
		convertZones(
				"/Volumes/Data/virtualbox/rdc_exchange/svidata/morgenspitze/zonen.shp",
				"/Volumes/Data/virtualbox/exchange/bellevue",
				"NO");
	}

	private static void convertZones(final String zonesShpFile, final String targetDirectory, final String zoneIdAttributeName) {
		Zones zones = new Zones();
		new ShapeZonesReader(zones).readShapefile(zonesShpFile);
		new DynusTZonesWriter(zones, zoneIdAttributeName).writeToDirectory(targetDirectory);
	}

	private static void convertNetwork(final String networkFilename, final String targetDirectory) {
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(scenario.getNetwork()).readFile(networkFilename);

		DynusTNetworkWriter writer = new DynusTNetworkWriter(scenario.getNetwork());
		writer.writeToDirectory(targetDirectory);

		DynusTNetworkWriterXls writer2 = new DynusTNetworkWriterXls(scenario.getNetwork());
		writer2.writeToDirectory(targetDirectory);
	}

}
