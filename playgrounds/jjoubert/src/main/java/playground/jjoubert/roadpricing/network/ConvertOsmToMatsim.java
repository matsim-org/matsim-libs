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

package playground.jjoubert.roadpricing.network;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.io.OsmNetworkReader;
import org.matsim.utils.gis.matsim2esri.network.CapacityBasedWidthCalculator;
import org.matsim.utils.gis.matsim2esri.network.FeatureGeneratorBuilderImpl;
import org.matsim.utils.gis.matsim2esri.network.Links2ESRIShape;
import org.matsim.utils.gis.matsim2esri.network.Nodes2ESRIShape;
import org.matsim.utils.gis.matsim2esri.network.PolygonFeatureGenerator;
import org.xml.sax.SAXException;

public class ConvertOsmToMatsim {

	/**
	 * Class to 
	 * @param args
	 */
	public static void main(String[] args) {

		String inputFile = "/Users/johanwjoubert/MATSim/workspace/MATSimData/Gauteng/SANRAL/Network/gauteng.osm";
		String outputFile = "/Users/johanwjoubert/MATSim/workspace/MATSimData/Gauteng/SANRAL/Network/gautengNetwork_Clean.xml.gz";
		String shapefileLinks = "/Users/johanwjoubert/MATSim/workspace/MATSimData/Gauteng/SANRAL/Network/gautengNetwork_Clean_Links.shp";
		String shapefileNodes = "/Users/johanwjoubert/MATSim/workspace/MATSimData/Gauteng/SANRAL/Network/gautengNetwork_Clean_Nodes.shp";

		Scenario sc = new ScenarioImpl();
		Network nw = sc.getNetwork();
		CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, TransformationFactory.WGS84_UTM35S);
		OsmNetworkReader onr = new OsmNetworkReader(nw, ct);
		onr.setKeepPaths(false);
		/*
		 * Configure the highway classification.
		 */
		
		try {
			onr.parse(inputFile);
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		new NetworkWriter(nw).writeFileV1(outputFile);
		
		sc.getConfig().global().setCoordinateSystem("WGS84_UTM35S");
		FeatureGeneratorBuilderImpl builder = new FeatureGeneratorBuilderImpl(nw, "WGS84_UTM35S");
		builder.setWidthCoefficient(-0.01);
		builder.setFeatureGeneratorPrototype(PolygonFeatureGenerator.class);
		builder.setWidthCalculatorPrototype(CapacityBasedWidthCalculator.class);
		
		new Links2ESRIShape(nw, shapefileLinks, builder).write();
		new Nodes2ESRIShape(nw, shapefileNodes, "WGS84_UTM35S").write();
	}

}
