/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.michalm.util.gis;

import java.util.*;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.*;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;

import com.vividsolutions.jts.geom.Geometry;

public class SubNetworkCreator {
	public static void main(String[] args) {
		// String dir = "D:/PP-rad/poznan/";
		// String networkFile = dir + "network.xml";
		// String polygonFile = dir + "poznan_polygon/poznan_city_polygon.shp";
		// String subNetworkFile = dir + "sub-network-2.xml";

		String dir = "d:/svn-vsp/sustainability-w-michal-and-dlr/data/";
		String networkFile = dir + "network/berlin_brb.xml.gz";
		String polygonFile = dir + "shp_merged/berlin_zones_convex_hull_with_buffer_DHDN_GK4.shp";
		String subNetworkFile = dir + "network/berlin.xml.gz";

		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		MatsimNetworkReader nr = new MatsimNetworkReader(scenario.getNetwork());
		nr.readFile(networkFile);

		Network network = scenario.getNetwork();
		List<Link> allLinks = new ArrayList<>(network.getLinks().values());

		Geometry polygonGeometry = PolygonBasedFilter.readPolygonGeometry(polygonFile);
		Iterable<? extends Link> outerLinks = PolygonBasedFilter.filterLinksOutsidePolygon(allLinks, polygonGeometry,
				false);

		for (Link link : outerLinks) {
			network.removeLink(link.getId());
		}

		new NetworkCleaner().run(network);
		new NetworkWriter(network).write(subNetworkFile);
	}
}
