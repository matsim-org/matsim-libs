/* *********************************************************************** *
 * project: org.matsim.*
 * AnalyseEquil.java
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

package playground.jjoubert.TemporaryCode;

import java.io.File;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.MatsimNetworkReader;

public class AnalyseEquil {
	private final static Logger log  = Logger.getLogger(AnalyseEquil.class);
	
	public static void main(String[] args) {
		File f = new File("src/main/java/playground/jjoubert/TemporaryCode/equilNetwork.xml");
		log.info("Network: " + f.getAbsolutePath());
		Scenario s = new ScenarioImpl();
		MatsimNetworkReader nr = new MatsimNetworkReader(s);
		nr.readFile(f.getAbsolutePath());
		double minX = Double.POSITIVE_INFINITY;
		double maxX = Double.NEGATIVE_INFINITY;
		double minY = Double.POSITIVE_INFINITY;
		double maxY = Double.NEGATIVE_INFINITY;
		for(Node n : s.getNetwork().getNodes().values()){
			minX = Math.min(minX, n.getCoord().getX());
			maxX = Math.max(maxX, n.getCoord().getX());
			minY = Math.min(minY, n.getCoord().getY());
			maxY = Math.max(maxY, n.getCoord().getY());
			log.info("Node: " + n.getId() + " (" + n.getCoord().getX() + "," + n.getCoord().getY() + ")");
		}
		log.info("Min X: " + minX);
		log.info("Max X: " + maxX);
		log.info("Min Y: " + minY);
		log.info("Max Y: " + maxY);
	}

}
