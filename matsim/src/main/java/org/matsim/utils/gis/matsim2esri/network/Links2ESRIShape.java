/* *********************************************************************** *
 * project: org.matsim.*
 * Links2ESRIShape.java
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

package org.matsim.utils.gis.matsim2esri.network;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import org.apache.log4j.Logger;
import org.geotools.feature.Feature;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * Simple class to convert the links of MATSim network files to ESRI shape files. The network could be written either
 * as line strings or as polygons. Furthermore the width of the links could be calculated according to
 * freespeed, lanes or capacity. For some basic examples please have a look at the <code>main</code> method.
 * Can also be called as Links2ESRIShape inputNetwork.xml outputAsLines.shp outputAsPolygons.shp .
 *  
 * <p> <strong>Keywords:</strong> converter, network, links, esri, shp, matsim </p>
 *
 * @author laemmel
 */
public class Links2ESRIShape {

	private static Logger log = Logger.getLogger(Links2ESRIShape.class);

	private final FeatureGenerator featureGenerator;
	private final NetworkLayer network;
	private final String filename;


	public Links2ESRIShape(final Network network, final String filename) {
		this(network, filename, new FeatureGeneratorBuilder(network));
	}

	public Links2ESRIShape(final Network network, final String filename, final FeatureGeneratorBuilder builder) {
		this.network = (NetworkLayer) network;
		this.filename = filename;
		this.featureGenerator = builder.createFeatureGenerator();

	}


	public void write() {
		Collection<Feature> features = new ArrayList<Feature>();
		for (LinkImpl link : this.network.getLinks().values()) {
			features.add(this.featureGenerator.getFeature(link));
		}
		try {
			ShapeFileWriter.writeGeometries(features, this.filename);
		} catch (IOException e) {
			e.printStackTrace();
		} 
	}

	public static void main(final String [] args) {
		String netfile = null ;
		String outputFileLs = null ;
		String outputFileP = null ;
		
		if ( args.length == 0 ) {
			netfile = "./examples/equil/network.xml";
//		String netfile = "./test/scenarios/berlin/network.xml.gz";

			outputFileLs = "./plans/networkLs.shp";
			outputFileP = "./plans/networkP.shp";
		} else if ( args.length == 3 ) {
			netfile = args[0] ;
			outputFileLs = args[1] ;
			outputFileP  = args[2] ;
		} else {
			log.error("Arguments cannot be interpreted.  Aborting ...") ;
			System.exit(-1) ;
		}
		
		ScenarioImpl scenario = new ScenarioImpl();
		scenario.getConfig().global().setCoordinateSystem("DHDN_GK4");

		log.info("loading network from " + netfile);
		final NetworkLayer network = scenario.getNetwork();
		new MatsimNetworkReader(scenario).readFile(netfile);
		log.info("done.");

		FeatureGeneratorBuilder builder = new FeatureGeneratorBuilder(network);
		builder.setFeatureGeneratorPrototype(LineStringBasedFeatureGenerator.class);
		builder.setWidthCoefficient(0.5);
		builder.setWidthCalculatorPrototype(LanesBasedWidthCalculator.class);		
		new Links2ESRIShape(network,outputFileLs, builder).write();

		CoordinateReferenceSystem crs = MGC.getCRS("DHDN_GK4");
		builder.setWidthCoefficient(0.001);
		builder.setFeatureGeneratorPrototype(PolygonFeatureGenerator.class);
		builder.setWidthCalculatorPrototype(CapacityBasedWidthCalculator.class);
		builder.setCoordinateReferenceSystem(crs);
		new Links2ESRIShape(network,outputFileP, builder).write();
		
	}

}
