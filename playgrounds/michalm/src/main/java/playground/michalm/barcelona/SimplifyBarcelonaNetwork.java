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

package playground.michalm.barcelona;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;

import com.google.common.collect.Sets;

import playground.andreas.utils.net.NetworkSimplifier;

public class SimplifyBarcelonaNetwork
{
    public static void main(String[] args)
    {
        String dir = "d:/PP-rad/Barcelona/data/network/";
        String networkFile = dir + "barcelona_network.xml";
        String simplifiedNetworkFile = dir + "barcelona_simplified_network.xml";

        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        new MatsimNetworkReader(scenario).readFile(networkFile);

        NetworkSimplifier simplifier = new NetworkSimplifier();
        simplifier.setNodesToMerge(Sets.newHashSet(4, 5));
        simplifier.run(scenario.getNetwork());
        simplifier.run(scenario.getNetwork());//2nd run

        new NetworkWriter(scenario.getNetwork()).write(simplifiedNetworkFile);
    }
}
