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

import java.util.Arrays;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.utils.gis.matsim2esri.network.*;


public class Net2GIS
{
    public static void main(String[] args)
    {
        String dir;
        String netFile;
        String outFileLs;
        String outFileN;

        if (args.length == 0) {// for testing
            dir = "D:\\PP-rad\\taxi\\mielec-2-peaks\\";
            netFile = dir + "network.xml";
            outFileLs = dir + "GIS\\linksLs.shp";
            outFileN = dir + "GIS\\nodes.shp";
        }
        else if (args.length == 5) {
            dir = args[0];
            netFile = dir + args[1];
            outFileLs = dir + args[2];
            outFileN = dir + args[4];
        }
        else {
            throw new IllegalArgumentException(
                    "Incorrect program arguments: " + Arrays.toString(args));
        }

        String coordSystem = TransformationFactory.WGS84_UTM33N;

        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        scenario.getConfig().global().setCoordinateSystem(coordSystem);

        Network network = scenario.getNetwork();
        new MatsimNetworkReader(scenario.getNetwork()).readFile(netFile);

        // links as lines
        FeatureGeneratorBuilderImpl builder = new FeatureGeneratorBuilderImpl(network, coordSystem);
        builder.setFeatureGeneratorPrototype(LineStringBasedFeatureGenerator.class);
        builder.setWidthCalculatorPrototype(LanesBasedWidthCalculator.class);
        new Links2ESRIShape(network, outFileLs, builder).write();

        // nodes as points
        new Nodes2ESRIShape(network, outFileN, coordSystem).write();
    }
}
