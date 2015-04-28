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

/**
 * 
 */
package playground.jjoubert.projects.capeTownFreight;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.network.NodeImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;

import playground.southafrica.utilities.Header;

/**
 * @author jwjoubert
 *
 */
public class ConvertNetworkToWgs {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Header.printHeader(ConvertNetworkToWgs.class.toString(), args);
		
		String input = args[0];
		String output = args[1];
		
		/* Read the network. */
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(scenario ).parse(input);
		
		/* Transform each node. */
		CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation("WGS84_SA_Albers", "EPSG:3857");
		for(Node node : scenario.getNetwork().getNodes().values()){
			((NodeImpl)node).setCoord(ct.transform(node.getCoord()));
		}
		
		/* Write the resulting network. */
		new NetworkWriter(scenario.getNetwork()).write(output);
		
		Header.printFooter();
	}

}
