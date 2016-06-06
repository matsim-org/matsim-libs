/* *********************************************************************** *
 * project: org.matsim.*
 * NetworkConverter.java
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
package playground.jjoubert.projects.capeTownMultimodal;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NodeImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.misc.Counter;

import playground.southafrica.utilities.Header;

/**
 * Class to convert the MATSim {@link Network} file from one coordinate
 * reference system to another.
 *  
 * @author jwjoubert
 */
public class NetworkConverter {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Header.printHeader(NetworkConverter.class.toString(), args);
		String inputNetwork = args[0];
		String inputCRS = args[1];
		String outputNetwork = args[2];
		String outputCRS = args[3];
		
		Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(sc.getNetwork()).parse(inputNetwork);
		
		CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation(inputCRS, outputCRS);
		Counter counter = new Counter("  node # ");
		
		for(Node node : sc.getNetwork().getNodes().values()){
			((NodeImpl)node).setCoord(ct.transform(node.getCoord()));
			counter.incCounter();
		}
		counter.printCounter();
		
		/* Write the resulting network to file. */
		new NetworkWriter(sc.getNetwork()).write(outputNetwork);
		
		Header.printFooter();
	}

}
