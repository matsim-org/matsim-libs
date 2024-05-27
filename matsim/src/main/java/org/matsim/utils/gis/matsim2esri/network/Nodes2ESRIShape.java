/* *********************************************************************** *
 * project: org.matsim.*
 * Nodes2ESRIShape.java
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

import java.util.ArrayList;
import java.util.Collection;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.locationtech.jts.geom.Point;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.GeoFileWriter;

/**
 * Simple class to convert the nodess of MATSim network files to ESRI shape files. The nodes could be written points
 * For a basic example please have a look at the <code>main</code> method.
 * Can also be called as Nodes2ESRIShape inputNetwork.xml output.shp .
 *
 * <p> <strong>Keywords:</strong> converter, network, nodes, esri, shp, matsim </p>
 *
 * @author laemmel
 */
public class Nodes2ESRIShape {

	private final static Logger log = LogManager.getLogger(Nodes2ESRIShape.class);

	private final Network network;
	private final String filename;
	private SimpleFeatureBuilder builder;


	public Nodes2ESRIShape(final Network network, final String filename, final String coordinateSystem) {
		this(network, filename, MGC.getCRS(coordinateSystem));
	}

	public Nodes2ESRIShape(Network network, String filename, CoordinateReferenceSystem crs) {
		this.network = network;
		this.filename = filename;
		initFeatureType(crs);
	}

	public void write() {
		Collection<SimpleFeature> features = new ArrayList<SimpleFeature>();
		for (Node node : NetworkUtils.getSortedNodes(this.network)) {
			features.add(getFeature(node));
		}
		GeoFileWriter.writeGeometries(features, this.filename);

	}

	private SimpleFeature getFeature(Node node) {
		Point p = MGC.coord2Point(node.getCoord());
		try {
			return this.builder.buildFeature(null, new Object[]{p,node.getId().toString()});
		} catch (IllegalArgumentException e) {
			throw new RuntimeException(e);
		}
	}

	private void initFeatureType(final CoordinateReferenceSystem crs) {
		SimpleFeatureTypeBuilder typeBuilder = new SimpleFeatureTypeBuilder();
		typeBuilder.setName("node");
		typeBuilder.setCRS(crs);
		typeBuilder.add("location", Point.class);
		typeBuilder.add("ID", String.class);

		this.builder = new SimpleFeatureBuilder(typeBuilder.buildFeatureType());
	}

	public static void main(String [] args) {
		String netfile = null ;
		String outputFile = null ;

		if ( args.length == 0 ) {
			netfile = "./examples/equil/network.xml";
//		String netfile = "./test/scenarios/berlin/network.xml.gz";

			outputFile = "./plans/networkNodes.shp";
		} else if ( args.length == 2 ) {
			netfile = args[0] ;
			outputFile = args[1] ;
		} else {
			log.error("Arguments cannot be interpreted.  Aborting ...") ;
			System.exit(-1) ;
		}

		MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
//		scenario.getConfig().global().setCoordinateSystem("DHDN_GK4");

		log.info("loading network from " + netfile);
		final Network network = scenario.getNetwork();
		new MatsimNetworkReader(scenario.getNetwork()).readFile(netfile);
		log.info("done.");

		new Nodes2ESRIShape(network,outputFile, "DHDN_GK4").write();
	}

}
