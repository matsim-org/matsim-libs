/* *********************************************************************** *
 * project: org.matsim.*
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

package playground.michalm.util.osm;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.*;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.io.OsmNetworkReader;


public class CreatePoznanNetwork
{
    public static void main(String[] args)
    {
        String dir;
        String osmFile;
        String networkFile;

        if (args.length == 3) {
            dir = args[0];
            osmFile = dir + args[1];
            networkFile = dir + args[2];
        }
        else if (args.length == 0) {
            dir = "D:\\PP-rad\\matsim-poznan\\";
            osmFile = dir + "poznan.osm";
            networkFile = dir + "network.xml";
        }
        else {
            throw new IllegalArgumentException();
        }

        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        Network network = scenario.getNetwork();

        CoordinateTransformation coordTrans = TransformationFactory.getCoordinateTransformation(
                TransformationFactory.WGS84, TransformationFactory.WGS84_UTM33N);

        OsmNetworkReader onr = new OsmNetworkReader(network, coordTrans);

        onr.setHighwayDefaults(1, "motorway", 2, 140.0 / 3.6, 1.0, 2000, true);
        onr.setHighwayDefaults(1, "motorway_link", 1, 70.0 / 3.6, 1.0, 1500, true);
        onr.setHighwayDefaults(2, "trunk", 1, 120.0 / 3.6, 1.0, 2000);
        onr.setHighwayDefaults(2, "trunk_link", 1, 70.0 / 3.6, 1.0, 1500);
        onr.setHighwayDefaults(3, "primary", 1, 90.0 / 3.6, 1.0, 1500);
        onr.setHighwayDefaults(3, "primary_link", 1, 60.0 / 3.6, 1.0, 1500);
        onr.setHighwayDefaults(4, "secondary", 1, 50.0 / 3.6, 1.0, 1000);
        onr.setHighwayDefaults(4, "secondary_link", 1, 40.0 / 3.6, 1.0, 1000);
        onr.setHighwayDefaults(5, "tertiary", 1, 50.0 / 3.6, 1.0, 800);
        onr.setHighwayDefaults(5, "tertiary_link", 1, 45.0 / 3.6, 1.0, 600);
        onr.setHighwayDefaults(6, "road", 1, 40.0 / 3.6, 1.0, 600);
        onr.setHighwayDefaults(6, "unclassified", 1, 40.0 / 3.6, 1.0, 600);
        onr.setHighwayDefaults(6, "residential", 1, 30.0 / 3.6, 1.0, 600);
        onr.setHighwayDefaults(6, "living_street", 1, 20.0 / 3.6, 1.0, 300);

        onr.parse(osmFile);

        new NetworkCleaner().run(network);
        new NetworkWriter(network).write(networkFile);
    }
}
