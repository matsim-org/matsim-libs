/* *********************************************************************** *
 * project: org.matsim.*
 * ConvertOsmToMatsim.java
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

package playground.southafrica.utilities.network;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.io.OsmNetworkReader;
import org.matsim.core.config.ConfigUtils;
import org.matsim.utils.gis.matsim2esri.network.CapacityBasedWidthCalculator;
import org.matsim.utils.gis.matsim2esri.network.FeatureGeneratorBuilderImpl;
import org.matsim.utils.gis.matsim2esri.network.Links2ESRIShape;
import org.matsim.utils.gis.matsim2esri.network.PolygonFeatureGenerator;

import playground.southafrica.utilities.Header;

public class ConvertOsmToMatsim {
	final private static Logger LOG = Logger.getLogger(ConvertOsmToMatsim.class);
	
	/**
	 * Class to 
	 * @param args
	 */
	public static void main(String[] args) {
		Header.printHeader(ConvertOsmToMatsim.class.toString(), args);
		
		String inputFile = null;
		String outputFile = null;
		String shapefileLinks = null;
		boolean fullNetwork = true;
		String CRS = null;
		
		if(args.length != 5){
			throw new IllegalArgumentException("Must have five arguments: and osm-file; network-file; shapefile; boolean indicating full or cleaned network; and the final coordinate reference system.");
		} else{
			inputFile = args[0];
			outputFile = args[1];
			shapefileLinks = args[2].equalsIgnoreCase("null") ? null : args[2];	
			fullNetwork = Boolean.parseBoolean(args[3]);
			CRS = args[4];
		}

		Scenario sc = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Network nw = sc.getNetwork();
		CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, CRS);
		OsmNetworkReader onr = new OsmNetworkReader(nw, ct, true);
		onr.setKeepPaths(fullNetwork);
		/*
		 * Configure the highway classification.
		 */
		LOG.info("Overwriting some highway defaults...");
		onr.setHighwayDefaults(1, "trunk", 1, 120/3.6, 1, 2000);
		onr.setHighwayDefaults(1, "primary", 2, 80/3.6, 1, 1500);
		onr.setHighwayDefaults(1, "secondary", 1, 80/3.6, 1, 1000);
		onr.setHighwayDefaults(1, "tertiary", 1, 60/3.6, 1, 1000);
		onr.setHighwayDefaults(1, "unclassified", 1, 60/3.6, 1, 800);
		onr.setHighwayDefaults(1, "residential", 1, 45/3.6, 1, 600);
		
		LOG.info("Parsing the OSM file...");
		onr.parse(inputFile);
		
		NetworkCleaner nc = new NetworkCleaner();
		nc.run(nw);		
		
		new NetworkWriter(nw).writeFileV1(outputFile);
		
		sc.getConfig().global().setCoordinateSystem(CRS);
		FeatureGeneratorBuilderImpl builder = new FeatureGeneratorBuilderImpl(nw, CRS);
		builder.setWidthCoefficient(-0.01);
		builder.setFeatureGeneratorPrototype(PolygonFeatureGenerator.class);
		builder.setWidthCalculatorPrototype(CapacityBasedWidthCalculator.class);
		
		if(shapefileLinks != null){
			new Links2ESRIShape(nw, shapefileLinks, builder).write();
		}
		Header.printFooter();
	}

}
