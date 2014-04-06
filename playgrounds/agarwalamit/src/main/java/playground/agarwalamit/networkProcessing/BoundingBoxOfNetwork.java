/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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
package playground.agarwalamit.networkProcessing;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * @author amit
 */
public class BoundingBoxOfNetwork {
	String networkFile;
	double minX;
	double maxX;
	double minY;
	double maxY;

	public BoundingBoxOfNetwork(String networkFile) {
		this.networkFile  = networkFile;
		minX=99999990.; 
		maxX=0.;
		minY=99999990.;
		maxY=0.;
		run();
	}

	private void run() {
		Config config = ConfigUtils.createConfig();
		config.network().setInputFile(networkFile);
		Scenario scenario = ScenarioUtils.loadScenario(config);
		Network network = scenario.getNetwork();

		for(Node node : network.getNodes().values()){
			Coord coord = node.getCoord();
			double xcoord = coord.getX();
			double ycoord = coord.getY();
			minX = Math.min(xcoord, minX);
			minY = Math.min(ycoord, minY);
			maxX = Math.max(xcoord, maxX);
			maxY = Math.max(ycoord, maxY);
		}
	}

	public double getMinX() {
		return minX;
	}

	public double getMaxX() {
		return maxX;
	}

	public double getMinY() {
		return minY;
	}

	public double getMaxY() {
		return maxY;
	}
	public static void main(String[] args) {
		BoundingBoxOfNetwork bbx = new BoundingBoxOfNetwork("./input/baseCase/SiouxFalls_networkWithRoadType.xml.gz");
		System.out.println("Bounding box for the given network : minX, maxX, minY, maxY = "+bbx.getMinX()+","+bbx.getMaxX()+","+bbx.getMinY()+","+bbx.getMaxY());
	}
	
}
