/* *********************************************************************** *
 * project: org.matsim.*
 * SaAlbersToDecimalDegreesConverter.java                                                                        *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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
package playground.southafrica.utilities.network;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.NetworkReaderMatsimV2;
import org.matsim.core.network.io.NetworkWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;

import playground.southafrica.utilities.Header;

/**
 * Small utility class to convert a given network from the projected SA-Albers
 * coordinate reference system (CRS) to WGS84 decimal degrees.
 *  
 * @author jwjoubert
 */
public class SaAlbersToDecimalDegreesConverter {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Header.printHeader(SaAlbersToDecimalDegreesConverter.class.toString(), args);
		String inFile = args[0];
		String outFile = args[1];
		
		/* Parse the network. */
		Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation("WGS84_SA_Albers", "WGS84");
		new NetworkReaderMatsimV2(ct, sc.getNetwork()).readFile(inFile);
		
		/* Write the converted network to file. */
		new NetworkWriter(sc.getNetwork()).write(outFile);
		
		Header.printFooter();
	}

}
