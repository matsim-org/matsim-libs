/* *********************************************************************** *
 * project: org.matsim.*
 * MATSimNet2ShapeDemo.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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
package playground.yu.utils.qgis;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.utils.gis.matsim2esri.network.CapacityBasedWidthCalculator;
import org.matsim.utils.gis.matsim2esri.network.FeatureGeneratorBuilder;
import org.matsim.utils.gis.matsim2esri.network.LanesBasedWidthCalculator;
import org.matsim.utils.gis.matsim2esri.network.LineStringBasedFeatureGenerator;
import org.matsim.utils.gis.matsim2esri.network.Links2ESRIShape;
import org.matsim.utils.gis.matsim2esri.network.PolygonFeatureGenerator;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * This class is a copy of main() from
 * org.matsim.utils.gis.matsim2esri.network.Network2ESRIShape and can convert a
 * MATSim-network to a QGIS .shp-file (link or polygon)
 *
 * @author ychen
 *
 */
public class MATSimNet2QGISDemo implements X2QGIS {

	public static void main(final String[] args) {
		// String netfile =
		// "../schweiz-ivtch-SVN/baseCase/network/ivtch-osm.xml";
		// String outputFileLs =
		// "../schweiz-ivtch-SVN/baseCase/network/ivtch-osm_Links.shp";
		// String outputFileP =
		// "../schweiz-ivtch-SVN/baseCase/network/ivtch-osm_Polygon.shp";
		// String netfile = "test/scenarios/berlin/network.xml.gz";
		// String outputFileLs = "output/bvg/berlinLinks.shp";
		// String outputFileP = "output/bvg/berlinPolygon.shp";
		String netfile = "../berlin data/network.xml";
		String outputFileLs = "../berlin data/Links.shp";
		String outputFileP = "../berlin data/Polygon.shp";

		// String coordinateSys = ch1903;
		String coordinateSys = "DHDN_GK4";
//		String coordinateSys = "Atlantis";
		ScenarioImpl scenario = new ScenarioImpl();
		scenario.getConfig().global().setCoordinateSystem(coordinateSys);

		Logger log = Logger.getLogger(Links2ESRIShape.class);
		log.info("loading network from " + netfile);
		Network network = scenario.getNetwork();
		new MatsimNetworkReader(scenario).readFile(netfile);
		log.info("done.");

		FeatureGeneratorBuilder builder = new FeatureGeneratorBuilder(network, coordinateSys);
		builder
				.setFeatureGeneratorPrototype(LineStringBasedFeatureGenerator.class);
		builder.setWidthCoefficient(0.5);
		builder.setWidthCalculatorPrototype(LanesBasedWidthCalculator.class);
		new Links2ESRIShape(network, outputFileLs, builder).write();

		CoordinateReferenceSystem crs = MGC.getCRS(coordinateSys);
		builder.setWidthCoefficient(0.01);
		builder.setFeatureGeneratorPrototype(PolygonFeatureGenerator.class);
		builder.setWidthCalculatorPrototype(CapacityBasedWidthCalculator.class);
		builder.setCoordinateReferenceSystem(crs);
		new Links2ESRIShape(network, outputFileP, builder).write();
	}
}
