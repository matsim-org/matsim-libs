/* *********************************************************************** *
 * project: org.matsim.*
 * ConvertMatsim2dToMatsim3d.java
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

/**
 * 
 */
package playground.southafrica.utilities.network;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.NetworkReaderMatsimV1;
import org.matsim.core.network.io.NetworkWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.misc.Counter;

import playground.southafrica.utilities.Header;
import playground.southafrica.utilities.coord3D.Utils3D;

/**
 * Class to convert the basic 2D road network (typically derived from 
 * OpenStreetMap) into a 3D road network.
 *  
 * @author jwjoubert
 */
public class ConvertMatsim2dToMatsim3d {
	final private static Logger LOG = Logger.getLogger(ConvertMatsim2dToMatsim3d.class);

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Header.printHeader(ConvertMatsim2dToMatsim3d.class.toString(), args);
		
		String networkIn = args[0];
		String crs = args[1];
		String srtmPath = args[2];
		String networkOut = args[3];
		
		/* Set up and read in the network. */
		Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new NetworkReaderMatsimV1(sc.getNetwork()).readFile(networkIn);
		CoordinateTransformation ctToWgs = TransformationFactory.getCoordinateTransformation(crs, "WGS84");
		CoordinateTransformation ctFromWgs = TransformationFactory.getCoordinateTransformation("WGS84", crs);

		/* Convert each node's coordinate. */
		LOG.info("Estimating elevation for each node's coordinate...");
		Counter counter = new Counter("   node # ");
		for(Node node : sc.getNetwork().getNodes().values()){
			Coord c2d = ctToWgs.transform(node.getCoord());
			double elevation = Utils3D.estimateSrtmElevation(srtmPath, c2d);
			Coord c3d = ctFromWgs.transform(CoordUtils.createCoord(c2d.getX(), c2d.getY(), elevation));
			node.setCoord(c3d);
			counter.incCounter();
		}
		counter.printCounter();
		LOG.info("Done estimating elevation.");
		
		new NetworkWriter(sc.getNetwork()).writeFileV1(networkOut);
		
		Header.printFooter();
	}
	
}
