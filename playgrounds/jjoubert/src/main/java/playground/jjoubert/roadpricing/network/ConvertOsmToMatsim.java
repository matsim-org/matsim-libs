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
import org.matsim.utils.gis.matsim2esri.network.Links2ESRIShape;
import org.matsim.utils.gis.matsim2esri.network.Nodes2ESRIShape;
import org.xml.sax.SAXException;

public class ConvertOsmToMatsim {

	/**
	 * @param args
	 */
	public static void main(String[] args) {

		String inputFile = "/Users/johanwjoubert/MATSim/workspace/MATSimData/Gauteng/SANRAL/Network/gauteng.osm";
		String outputFile = "/Users/johanwjoubert/MATSim/workspace/MATSimData/Gauteng/SANRAL/Network/gautengNetwork_Full.xml.gz";
		String shapefileLinks = "/Users/johanwjoubert/MATSim/workspace/MATSimData/Gauteng/SANRAL/Network/gautengNetwork_Full_Links.shp";
		String shapefileNodes = "/Users/johanwjoubert/MATSim/workspace/MATSimData/Gauteng/SANRAL/Network/gautengNetwork_Full_Nodes.shp";

		Scenario sc = new ScenarioImpl();
		Network nw = sc.getNetwork();
		CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, TransformationFactory.WGS84_UTM35S);
		OsmNetworkReader onr = new OsmNetworkReader(nw, ct);
		onr.setKeepPaths(true);
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
		
		Links2ESRIShape l2e = new Links2ESRIShape(nw, shapefileLinks, TransformationFactory.WGS84_UTM35S);
		l2e.write();
		
		Nodes2ESRIShape n2e = new Nodes2ESRIShape(nw, shapefileNodes, TransformationFactory.WGS84_UTM35S);
		n2e.write();
	}

}
