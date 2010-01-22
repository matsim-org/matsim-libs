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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import org.apache.log4j.Logger;
import org.geotools.factory.FactoryRegistryException;
import org.geotools.feature.AttributeType;
import org.geotools.feature.AttributeTypeFactory;
import org.geotools.feature.DefaultAttributeTypeFactory;
import org.geotools.feature.Feature;
import org.geotools.feature.FeatureType;
import org.geotools.feature.FeatureTypeBuilder;
import org.geotools.feature.IllegalAttributeException;
import org.geotools.feature.SchemaException;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Point;

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
	
	private final static Logger log = Logger.getLogger(Nodes2ESRIShape.class);
	
	private final Network network;
	private final String filename;
	private FeatureType featureType;

	public Nodes2ESRIShape(final Network network, final String filename) {
		this.network = network;
		this.filename = filename;
		initFeatureType();
	}
	
	public void write() {
		Collection<Feature> features = new ArrayList<Feature>();
		for (Node node : this.network.getNodes().values()) {
			features.add(getFeature(node));
		}
		try {
			ShapeFileWriter.writeGeometries(features, this.filename);
		} catch (IOException e) {
			e.printStackTrace();
		} 
	
	}

	private Feature getFeature(Node node) {
		Point p = MGC.coord2Point(node.getCoord());
		try {
			return this.featureType.create(new Object[]{p,node.getId().toString()});
		} catch (IllegalAttributeException e) {
			throw new RuntimeException(e);
		}
	}
	
	private void initFeatureType() {
		CoordinateReferenceSystem crs = MGC.getCRS(Gbl.getConfig().global().getCoordinateSystem());
		AttributeType [] attribs = new AttributeType[2];
		attribs[0] = DefaultAttributeTypeFactory.newAttributeType("Point",Point.class, true, null, null, crs);
		attribs[1] = AttributeTypeFactory.newAttributeType("ID", String.class);

		try {
			this.featureType = FeatureTypeBuilder.newFeatureType(attribs, "node");
		} catch (FactoryRegistryException e) {
			e.printStackTrace();
		} catch (SchemaException e) {
			e.printStackTrace();
		}

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
		
		ScenarioImpl scenario = new ScenarioImpl();
		scenario.getConfig().global().setCoordinateSystem("DHDN_GK4");

		log.info("loading network from " + netfile);
		final NetworkLayer network = scenario.getNetwork();
		new MatsimNetworkReader(scenario).readFile(netfile);
		log.info("done.");
		
		new Nodes2ESRIShape(network,outputFile).write();
	}

}
