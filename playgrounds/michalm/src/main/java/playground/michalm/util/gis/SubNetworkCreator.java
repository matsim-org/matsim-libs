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

import com.vividsolutions.jts.geom.Geometry;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.contrib.dvrp.run.VrpConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.scenario.ScenarioUtils;

import java.util.ArrayList;
import java.util.List;


public class SubNetworkCreator
{
    public static void main(String[] args)
    {
        String dir = "D:\\PP-rad\\poznan\\";
        String networkFile = dir + "network.xml";
        String polygonFile = dir + "poznan_polygon\\poznan_city_polygon.shp";
        String subNetworkFile = dir + "sub-network-2.xml";

        Scenario scenario = ScenarioUtils.createScenario(VrpConfigUtils.createConfig());
        MatsimNetworkReader nr = new MatsimNetworkReader(scenario);
        nr.readFile(networkFile);

        Network network = scenario.getNetwork();
        List<Link> allLinks = new ArrayList<>(network.getLinks().values());

        Geometry polygonGeometry = PolygonBasedFilter.readPolygonGeometry(polygonFile);
        Iterable<? extends Link> outerLinks = PolygonBasedFilter.filterLinksOutsidePolygon(
                allLinks, polygonGeometry, true);

        for (Link link : outerLinks) {
            network.removeLink(link.getId());
        }

        new NetworkCleaner().run(network);
        new NetworkWriter(network).write(subNetworkFile);
    }
}
