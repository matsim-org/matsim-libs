/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

package playground.jbischoff.av.preparation;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.network.filter.NetworkFilterManager;
import org.matsim.core.network.filter.NetworkLinkFilter;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.network.io.NetworkWriter;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * @author  jbischoff
 *
 */
public class CleanAndFilterNetwork {
public static void main(String[] args) {
	Scenario s = ScenarioUtils.createScenario(ConfigUtils.createConfig());
	new MatsimNetworkReader(s.getNetwork()).readFile("C:/Users/Joschka/Documents/shared-svn/projects/audi_av/scenario/network.xml.gz");
	NetworkFilterManager nfm = new NetworkFilterManager(s.getNetwork());
	nfm.addLinkFilter(new NetworkLinkFilter() {
		
		@Override
		public boolean judgeLink(Link l) {
			if (l.getAllowedModes().contains("car")) return true;
			else return false;
		}
	});
	Network newNet = nfm.applyFilters();
	
	new NetworkCleaner().run(newNet);
	new NetworkWriter(newNet).write("C:/Users/Joschka/Documents/shared-svn/projects/audi_av/scenario/networkc.xml.gz");
	
}
}
