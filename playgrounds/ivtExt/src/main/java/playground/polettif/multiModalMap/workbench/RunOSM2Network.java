/* *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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


package playground.polettif.multiModalMap.workbench;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.io.OsmNetworkReader;
import playground.polettif.boescpa.converters.osm.Osm2Network;

public class RunOSM2Network {

    public static void main(String[] args) {
//		final String gtfsPath = "C:/Users/polettif/Desktop/data/gtfs/zvv/";
        final String osmPath = "E:/data/osm/zurich-plus.osm";

        final String outputPath = "E:/data/network/zurich-plus.xml.gz";
//        final String mtsFile = "C:/Users/polettif/Desktop/output/gtfs2mts/google_sample.xml";

        Config config = ConfigUtils.createConfig();
        Scenario sc = ScenarioUtils.createScenario(config);
        Network net = sc.getNetwork();
        CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation(
                TransformationFactory.WGS84, TransformationFactory.CH1903_LV03_Plus);

        OsmNetworkReader onr = new OsmNetworkReader(net, ct);
        onr.parse(osmPath);

        new NetworkCleaner().run(net);
        new NetworkWriter(net).write(outputPath);

//        Osm2Network.main(new String[]{osmPath, outputPath});

    }

}