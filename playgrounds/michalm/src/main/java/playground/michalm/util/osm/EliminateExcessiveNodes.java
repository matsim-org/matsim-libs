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

import java.util.*;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.*;
import org.matsim.contrib.dvrp.run.VrpConfigUtils;
import org.matsim.core.network.*;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.scenario.ScenarioUtils;


/**
 * Finds Link-Node-Link sequences where both links have the same capacity and speed. Then removes
 * the node and merges both links into a single one.
 * 
 * @author michalm
 */
public class EliminateExcessiveNodes
{
    public static void main(String[] args)
    {
        String dir;
        String inNetworkFile;
        String outNetworkFile;

        if (args.length == 3) {
            dir = args[0];
            inNetworkFile = dir + args[1];
            outNetworkFile = dir + args[2];
        }
        else if (args.length == 0) {
            dir = "D:\\PP-rad\\matsim-poznan\\";
            inNetworkFile = dir + "network.xml";
            outNetworkFile = dir + "network-cleaned.xml";
        }
        else {
            throw new IllegalArgumentException();
        }

        Scenario scenario = ScenarioUtils.createScenario(VrpConfigUtils.createConfig());
        Network network = scenario.getNetwork();
        new MatsimNetworkReader(scenario).readFile(inNetworkFile);

        List<Node> nodesToBeRemoved = new ArrayList<>();

        for (Node node : network.getNodes().values()) {
            if (node.getInLinks().size() == 1 && node.getOutLinks().size() == 1) {
                Link inLink = getInLink(node);
                Link outLink = getOutLink(node);

                if (inLink.getCapacity() != outLink.getCapacity()
                        || inLink.getFreespeed() != outLink.getFreespeed()
                        || inLink.getNumberOfLanes() != outLink.getNumberOfLanes()) {
                    continue;
                }

                // seems that both links are the same
                nodesToBeRemoved.add(node);
            }
        }

        NetworkFactory networkFactory = new NetworkFactoryImpl(network);

        for (Node node : nodesToBeRemoved) {
            Link inLink = getInLink(node);
            Node fromNode = inLink.getFromNode();

            Link outLink = getOutLink(node);
            Node toNode = outLink.getToNode();

            if (fromNode == toNode) {
                continue;
            }

            Link newLink = networkFactory.createLink(inLink.getId(), fromNode, toNode);
            newLink.setLength(inLink.getLength() + outLink.getLength());
            newLink.setFreespeed(inLink.getFreespeed());
            newLink.setCapacity(inLink.getCapacity());
            newLink.setNumberOfLanes(inLink.getNumberOfLanes());

            //replace the old Link-Node-Link triple with a new Link
            network.removeNode(node.getId());
            network.addLink(newLink);
        }

        new NetworkWriter(network).write(outNetworkFile);
    }


    private static Link getInLink(Node node)
    {
        return node.getInLinks().values().iterator().next();
    }


    private static Link getOutLink(Node node)
    {
        return node.getOutLinks().values().iterator().next();
    }
}
