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
package playground.agarwalamit.utils.networkProcessing;

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
	private String networkFile;
	private double minX;
	private double maxX;
	private double minY;
	private double maxY;

//	ZZ_TODO : compare this with bounding box of matsim core.
	public BoundingBoxOfNetwork(String networkFile) {
		this.networkFile  = networkFile;
		this.minX=99999990.; 
		this.maxX=0.;
		this.minY=99999990.;
		this.maxY=0.;
		run();
	}

	private void run() {
		Config config = ConfigUtils.createConfig();
		config.network().setInputFile(this.networkFile);
		Scenario scenario = ScenarioUtils.loadScenario(config);
		Network network = scenario.getNetwork();

		for(Node node : network.getNodes().values()){
			Coord coord = node.getCoord();
			double xcoord = coord.getX();
			double ycoord = coord.getY();
			this.minX = Math.min(xcoord, this.minX);
			this.minY = Math.min(ycoord, this.minY);
			this.maxX = Math.max(xcoord, this.maxX);
			this.maxY = Math.max(ycoord, this.maxY);
		}
	}

	public double getMinX() {
		return this.minX;
	}

	public double getMaxX() {
		return this.maxX;
	}

	public double getMinY() {
		return this.minY;
	}

	public double getMaxY() {
		return this.maxY;
	}
	public static void main(String[] args) {
		BoundingBoxOfNetwork bbx = new BoundingBoxOfNetwork("./input/baseCase/SiouxFalls_networkWithRoadType.xml.gz");
		System.out.println("Bounding box for the given network : minX, maxX, minY, maxY = "+bbx.getMinX()+","+bbx.getMaxX()+","+bbx.getMinY()+","+bbx.getMaxY());
	}
	
}
