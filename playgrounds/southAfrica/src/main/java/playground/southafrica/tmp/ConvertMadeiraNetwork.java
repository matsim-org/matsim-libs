/* *********************************************************************** *
 * project: org.matsim.*
 * ConvertMadeiraNetwork.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package playground.southafrica.tmp;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.NetworkWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.io.OsmNetworkReader;

import playground.southafrica.utilities.Header;

/**
 * @author jwjoubert
 *
 */
public class ConvertMadeiraNetwork {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Header.printHeader(ConvertMadeiraNetwork.class.toString(), args);
		
		String osm = args[0];
		String network = args[1];
		
		Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, "EPSG:3061");
		
		OsmNetworkReader onr = new OsmNetworkReader(sc.getNetwork(), ct);
		onr.setKeepPaths(true);
		onr.parse(osm);
		
		new NetworkWriter(sc.getNetwork()).write(network);
		
		Header.printFooter();
	}

}
