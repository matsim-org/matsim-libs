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

package playground.michalm.util.postprocess;

import java.io.*;
import java.util.*;

import org.matsim.api.core.v01.*;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;

import com.google.common.base.Predicate;
import com.vividsolutions.jts.geom.Geometry;

import playground.michalm.util.gis.PolygonBasedFilter;


public class SubNetworkLinkStats
{
    public static void main(String[] args)
    {
        String dir = "d:\\PP-rad\\poznan\\";
        String networkFile = dir + "network.xml";
        String linkStats = dir + "40.linkstats.txt.gz";
        String polygonFile = dir + "poznan_polygon\\poznan_city_polygon.shp";
        boolean includeBorderLinks = false;
        String filteredLinkStats = dir + "40.linkstats-filtered.txt.gz";

        Geometry polygonGeometry = PolygonBasedFilter.readPolygonGeometry(polygonFile);
        Predicate<Link> linkInsidePolygonPredicate = PolygonBasedFilter
                .createLinkInsidePolygonPredicate(polygonGeometry, includeBorderLinks);

        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        MatsimNetworkReader nr = new MatsimNetworkReader(scenario.getNetwork());
        nr.readFile(networkFile);

        Map<Id<Link>, ? extends Link> linkMap = scenario.getNetwork().getLinks();

        try (BufferedReader br = IOUtils.getBufferedReader(linkStats);
                PrintWriter pw = new PrintWriter(IOUtils.getBufferedWriter(filteredLinkStats))) {
            String header = br.readLine();
            pw.println(header);

            String line;
            while ( (line = br.readLine()) != null) {
                String linkId = new StringTokenizer(line).nextToken();// linkId - first column
                Link link = linkMap.get(Id.create(linkId, Link.class));

                if (linkInsidePolygonPredicate.apply(link)) {
                    pw.println(line);
                }
            }
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
