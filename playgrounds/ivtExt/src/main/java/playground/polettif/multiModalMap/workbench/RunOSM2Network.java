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

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.network.algorithms.NetworkTransform;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
//import playground.polettif.multiModalMap.osm.MultimodalNetworkCreatorPT;

public class RunOSM2Network {

    public static void main(String[] args){

        Config config = ConfigUtils.createConfig();
        Scenario sc = ScenarioUtils.createScenario(config);
        Network network = sc.getNetwork();

        String path2OSMFile = "C:/Users/polettif/Desktop/data/osm/zurich-plus.osm";
        String outputMultimodalNetwork = "C:/Users/polettif/Desktop/data/network/mm/zurich-plus-mm.xml";

//        new MultimodalNetworkCreatorPT(network).createMultimodalNetwork(path2OSMFile);
        Logger.getLogger(RunOSM2Network.class).fatal("did not compile with the above line, thus commenting it out. kai") ;
        System.exit(-1);
        NetworkTransform networkTransform = new NetworkTransform(TransformationFactory.getCoordinateTransformation("WGS84", "CH1903_LV03_Plus"));
        networkTransform.run(network);
        new NetworkWriter(network).write(outputMultimodalNetwork);

    }
}
