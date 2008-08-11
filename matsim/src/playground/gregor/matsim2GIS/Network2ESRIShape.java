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

package playground.gregor.matsim2GIS;

import org.apache.log4j.Logger;
import org.geotools.feature.Feature;
import org.geotools.feature.SchemaException;
import org.matsim.gbl.Gbl;
import org.matsim.network.Link;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;
import org.matsim.utils.gis.ShapeFileWriter;
import org.opengis.referencing.FactoryException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

public class Network2ESRIShape {

	private static Logger log = Logger.getLogger(Network2ESRIShape.class);

	private final FeatureGenerator featureGenerator;
	private final NetworkLayer network;
	private final String filename;


	public Network2ESRIShape(final NetworkLayer network, final String filename) {
		this(network, filename, new FeatureGeneratorBuilder(network));
	}

	public Network2ESRIShape(final NetworkLayer network, final String filename, final FeatureGeneratorBuilder builder) {
		this.network = network;
		this.filename = filename;
		this.featureGenerator = builder.getFeatureGenerator();

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
		} catch (FactoryException e) {
			e.printStackTrace();
		} catch (SchemaException e) {
			e.printStackTrace();
		}
	}

	public static void main(final String [] args) {


		String netfile = "./examples/equil/network.xml";
		String outputFileLs = "./output/networkLs.shp";
		String outputFileP = "./output/networkP.shp";
		Gbl.createConfig(null);
		Gbl.getConfig().global().setCoordinateSystem("WGS84_UTM47S");

		log.info("loading network from " + netfile);
		final NetworkLayer network = new NetworkLayer();
		new MatsimNetworkReader(network).readFile(netfile);
		//		world.setNetworkLayer(network);
		//		world.complete();
		log.info("done.");

		FeatureGeneratorBuilder builder = new FeatureGeneratorBuilder(network);
		builder.setFeatureGeneratorPrototype(LineStringBasedFeatureGenerator.class);
		builder.setWidthCoefficient(0.5);
		builder.setWidthCalculatorPrototype(LanesBasedWidthCalculator.class);

		new Network2ESRIShape(network,outputFileLs, builder).write();
		builder.setWidthCoefficient(0.01);
		builder.setFeatureGeneratorPrototype(PolygonFeatureGenerator.class);
		builder.setWidthCalculatorPrototype(CapacityBasedWidthCalculator.class);
		new Network2ESRIShape(network,outputFileP, builder).write();

	}

}
