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
import org.matsim.contrib.dvrp.run.VrpConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.utils.gis.matsim2esri.network.*;


public class Net2GIS
{
    public static void main(String[] args)
    {
        String dirName;
        String netFileName;
        String outFileNameLs;
        String outFileNameN;

        if (args.length == 0) {// for testing
            dirName = "D:\\PP-rad\\taxi\\mielec-2-peaks\\";
            netFileName = dirName + "network.xml";
            outFileNameLs = dirName + "GIS\\linksLs.shp";
            outFileNameN = dirName + "GIS\\nodes.shp";
        }
        else if (args.length == 5) {
            dirName = args[0];
            netFileName = dirName + args[1];
            outFileNameLs = dirName + args[2];
            outFileNameN = dirName + args[4];
        }
        else {
            throw new IllegalArgumentException("Incorrect program arguments: "
                    + Arrays.toString(args));
        }

        String coordSystem = TransformationFactory.WGS84_UTM33N;

        Scenario scenario = ScenarioUtils.createScenario(VrpConfigUtils.createConfig());
        scenario.getConfig().global().setCoordinateSystem(coordSystem);

        Network network = scenario.getNetwork();
        new MatsimNetworkReader(scenario).readFile(netFileName);

        // links as lines
        FeatureGeneratorBuilderImpl builder = new FeatureGeneratorBuilderImpl(network, coordSystem);
        builder.setFeatureGeneratorPrototype(LineStringBasedFeatureGenerator.class);
        builder.setWidthCalculatorPrototype(LanesBasedWidthCalculator.class);
        new Links2ESRIShape(network, outFileNameLs, builder).write();

        // nodes as points
        new Nodes2ESRIShape(network, outFileNameN, coordSystem).write();
    }
}
