/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

/**
 * 
 */
package playground.ikaddoura.utils.prepare;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.io.OsmNetworkReader;

/**
 * @author dhosse, ikaddoura
 *
 */
public class IKOSMFile2Network {
	
	private String osmInputFile = "/../osmFile.xml";
	private String networkOutputFile = "/../network.xml";
	
	private Scenario scenario;

	public static void main(String[] args) {
		
		IKOSMFile2Network main = new IKOSMFile2Network();
		main.loadScenario();
		
		main.generateAndWriteNetwork();
	}

	private void generateAndWriteNetwork(){
		
		CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation(
				TransformationFactory.WGS84, "PROJCS[\"WGS 84 / UTM zone 14N\","+
				                                    "GEOGCS[\"WGS 84\","+
				                                           "DATUM[\"WGS_1984\","+
				                                              "SPHEROID[\"WGS 84\",6378137,298.257223563,"+
				                                                   "AUTHORITY[\"EPSG\",\"7030\"]],"+
				                                               "AUTHORITY[\"EPSG\",\"6326\"]],"+
				                                           "PRIMEM[\"Greenwich\",0,"+
				                                               "AUTHORITY[\"EPSG\",\"8901\"]],"+
				                                           "UNIT[\"degree\",0.01745329251994328,"+
				                                               "AUTHORITY[\"EPSG\",\"9122\"]],"+
				                                           "AUTHORITY[\"EPSG\",\"4326\"]],"+
				                                       "UNIT[\"metre\",1,"+
				                                           "AUTHORITY[\"EPSG\",\"9001\"]],"+
				                                       "PROJECTION[\"Transverse_Mercator\"],"+
				                                       "PARAMETER[\"latitude_of_origin\",0],"+
				                                       "PARAMETER[\"central_meridian\",-99],"+
				                                       "PARAMETER[\"scale_factor\",0.9996],"+
				                                       "PARAMETER[\"false_easting\",500000],"+
				                                       "PARAMETER[\"false_northing\",0],"+
				                                       "AUTHORITY[\"EPSG\",\"32614\"],"+
				                                       "AXIS[\"Easting\",EAST],"+
				                                       "AXIS[\"Northing\",NORTH]]");
		Network network = scenario.getNetwork();
		OsmNetworkReader or = new OsmNetworkReader(network, ct);
		or.parse(this.osmInputFile);
		new NetworkCleaner().run(network);
		new NetworkWriter(network).writeV1(this.networkOutputFile);
	}
	
	private void loadScenario() {
		Config config = ConfigUtils.createConfig();
		scenario = ScenarioUtils.createScenario(config);
	}
}
