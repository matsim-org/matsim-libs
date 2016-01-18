/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,     *
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

package playground.southafrica.utilities.network;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.utils.gis.matsim2esri.network.FeatureGeneratorBuilderImpl;
import org.matsim.utils.gis.matsim2esri.network.LanesBasedWidthCalculator;
import org.matsim.utils.gis.matsim2esri.network.Links2ESRIShape;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import playground.southafrica.utilities.Header;

public class SouthAfricaLinks2EsriShapefile {

	/**
	 * Converts a given network file to an ESRI shapefile (polygons).
	 * @param args The following arguments, and in the following order:
	 * <ol>
	 * 		<li> the absolute path of the {@link Network} file to read;
	 * 		<li> the absolute path of the shapefile output.
	 * </ol>
	 */
	public static void main(String[] args) {
		Header.printHeader(SouthAfricaLinks2EsriShapefile.class.toString(), args);
		String input = args[0];
		String output = args[1];
		
		/* Read the network. */
		Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		MatsimNetworkReader mnr = new MatsimNetworkReader(sc.getNetwork());
		mnr.parse(input);
		
		/* Set up the ESRI conversion. */
		CoordinateReferenceSystem crs = MGC.getCRS("WGS84_SA_Albers");
		FeatureGeneratorBuilderImpl builder = new FeatureGeneratorBuilderImpl(sc.getNetwork(), "WGS84_SA_Albers");
		builder.setWidthCoefficient(-1*1);
		builder.setWidthCalculatorPrototype(LanesBasedWidthCalculator.class);
		builder.setCoordinateReferenceSystem(crs);
		
		/* Write the shapefile. */
		Links2ESRIShape lts = new Links2ESRIShape(sc.getNetwork(), output, builder);
		lts.write();
		
		Header.printFooter();
	}

}
