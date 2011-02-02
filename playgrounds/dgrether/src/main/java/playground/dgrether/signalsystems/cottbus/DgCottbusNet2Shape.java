/* *********************************************************************** *
 * project: org.matsim.*
 * DgCottbusNet2Shape
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
package playground.dgrether.signalsystems.cottbus;

import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkImpl;
import org.matsim.utils.gis.matsim2esri.network.Links2ESRIShape;


/**
 * @author dgrether
 *
 */
public class DgCottbusNet2Shape {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String netFile = "/media/data/work/repos/shared-svn/studies/dgrether/cottbus/Cottbus-BA/network_wo_junctions.xml";
		ScenarioImpl scenario = new ScenarioImpl();
		NetworkImpl net = scenario.getNetwork();
		new MatsimNetworkReader(scenario).readFile(netFile);

		
		new Links2ESRIShape(net, "/media/data/work/repos/shared-svn/studies/dgrether/cottbus/Cottbus-BA/network_wo_junctions.shp", "WGS84").write();
		
		
	}

}
