/* *********************************************************************** *
 * project: org.matsim.*
 * NetworkFromOsm.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package playground.jjoubert.Utilities.matsim2urbansim.NMBM;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.io.OsmNetworkReader;
import org.xml.sax.SAXException;

public class NetworkFromOsm {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String osmFilename = "C:/temp/GIS/NMBM/NMBM.osm";
		String outputFilename = "C:/temp/GIS/NMBM/NMBM.xml.gz";
		Scenario sc = new ScenarioImpl();
		Network nw = sc.getNetwork();
		CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, TransformationFactory.WGS84_Albers);
		OsmNetworkReader onr = new OsmNetworkReader(nw, ct);
		try {
			onr.parse(osmFilename);
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		new NetworkWriter(nw).write(outputFilename);
	}

}

