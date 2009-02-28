/* *********************************************************************** *
 * project: org.matsim.*
 * Network2ESRIShape.java
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
import org.matsim.gbl.Gbl;
import org.matsim.interfaces.core.v01.Link;
import org.matsim.interfaces.core.v01.Network;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;
import org.matsim.utils.geometry.geotools.MGC;
import org.matsim.utils.gis.ShapeFileWriter;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * Simple class to convert MATSim network files to ESRI shape files. The network could be written either
 * as line strings or as polygons. Furthermore the width of the links could be calculated according to
 * freespeed, lanes or capacity. For some basic examples please have a look at the <code>main</code> method.
 *  
 * @author laemmel
 *
 */
public class Network2ESRIShape {

	private static Logger log = Logger.getLogger(Network2ESRIShape.class);

	private final FeatureGenerator featureGenerator;
	private final Network network;
	private final String filename;


	public Network2ESRIShape(final Network network, final String filename) {
		this(network, filename, new FeatureGeneratorBuilder(network));
	}

	public Network2ESRIShape(final Network network, final String filename, final FeatureGeneratorBuilder builder) {
		this.network = network;
		this.filename = filename;
		this.featureGenerator = builder.createFeatureGenerator();

	}


	public void write() {
		Collection<Feature> features = new ArrayList<Feature>();
		for (Link link : this.network.getLinks().values()) {
			features.add(this.featureGenerator.getFeature(link));
		}
		try {
			ShapeFileWriter.writeGeometries(features, this.filename);
		} catch (IOException e) {
			e.printStackTrace();
		} 
	}

	public static void main(final String [] args) {

		String netfile = "./examples/equil/network.xml";
//		String netfile = "./test/scenarios/berlin/network.xml.gz";

		String outputFileLs = "./plans/networkLs.shp";
		String outputFileP = "./plans/networkP.shp";
		Gbl.createConfig(null);
		Gbl.getConfig().global().setCoordinateSystem("DHDN_GK4");

		log.info("loading network from " + netfile);
		final NetworkLayer network = new NetworkLayer();
		new MatsimNetworkReader(network).readFile(netfile);
		log.info("done.");

		FeatureGeneratorBuilder builder = new FeatureGeneratorBuilder(network);
		builder.setFeatureGeneratorPrototype(LineStringBasedFeatureGenerator.class);
		builder.setWidthCoefficient(0.5);
		builder.setWidthCalculatorPrototype(LanesBasedWidthCalculator.class);		
		new Network2ESRIShape(network,outputFileLs, builder).write();

		CoordinateReferenceSystem crs = MGC.getCRS("DHDN_GK4");
		builder.setWidthCoefficient(0.001);
		builder.setFeatureGeneratorPrototype(PolygonFeatureGenerator.class);
		builder.setWidthCalculatorPrototype(CapacityBasedWidthCalculator.class);
		builder.setCoordinateReferenceSystem(crs);
		new Network2ESRIShape(network,outputFileP, builder).write();

	}

}
