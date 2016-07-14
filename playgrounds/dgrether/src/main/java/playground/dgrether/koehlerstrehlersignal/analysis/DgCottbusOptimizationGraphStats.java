/* *********************************************************************** *
 * project: org.matsim.*
 * DgCottbusOptimizationGraphStats
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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
package playground.dgrether.koehlerstrehlersignal.analysis;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;

import playground.dgrether.DgPaths;


public class DgCottbusOptimizationGraphStats {

	public static void main(String[] args) {
		String signalsBBNet = DgPaths.REPOS + "shared-svn/projects/cottbus/cb2ks2010/2013-07-31_minflow_10_evening_peak/network_small.xml.gz";
		Scenario scSignalsBoundingBox = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		MatsimNetworkReader netReader = new MatsimNetworkReader(scSignalsBoundingBox.getNetwork());
		netReader.readFile(signalsBBNet);

		Network n = scSignalsBoundingBox.getNetwork();
		
		double c = 0.0;
		double v = 0.0;
		double l = 0.0;
		for (Link link : n.getLinks().values()) {
			c += link.getCapacity();
			v += link.getFreespeed();
			l += (link.getLength() * link.getNumberOfLanes());
		}
		c = c / n.getLinks().size();
		v = v / n.getLinks().size();
		l = l / n.getLinks().size();
		System.out.println("Average capacity: " + c);
		System.out.println("Average speed m/s " + v + " km/h: " + v * 3.6);
		System.out.println("Average link length: " + l);
		
	}

}
