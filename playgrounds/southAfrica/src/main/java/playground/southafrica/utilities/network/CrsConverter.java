/* *********************************************************************** *
 * project: org.matsim.*
 * CrsConverter.java
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
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.network.io.NetworkWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.misc.Counter;

import playground.southafrica.utilities.Header;

/**
 * Basic class to convert a given network from one given coordinate 
 * reference system to another. 
 * @param args
 *
 * @author jwjoubert
 */
public class CrsConverter {
	final private static Logger LOG = Logger.getLogger(CrsConverter.class);

	public static void main(String[] args) {
		Header.printHeader(CrsConverter.class.toString(), args);
		run(args);
		Header.printFooter();
	}
	
	public static void run(String[] args){
		String inNetwork = args[0];
		String inCrs = args[1];
		String outNetwork = args[2];
		String outCrs = args[3];

		Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(sc.getNetwork()).readFile(inNetwork);
		
		LOG.info("Converting the network...");
		CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation(inCrs, outCrs);
		Counter counter = new Counter(" nodes # ");
		for(Node node : sc.getNetwork().getNodes().values()){
			node.setCoord(ct.transform(node.getCoord()));
			counter.incCounter();
		}
		counter.printCounter();
		LOG.info("Done converting network nodes.");
		
		new NetworkWriter(sc.getNetwork()).write(outNetwork);
	}

}
