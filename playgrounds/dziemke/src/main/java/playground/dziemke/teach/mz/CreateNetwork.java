/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * ${file_name}
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) ${year} by the members listed in the COPYING,        *
 *  *                   LICENSE and WARRANTY file.                            *
 *  * email           : info at matsim dot org                                *
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  *   This program is free software; you can redistribute it and/or modify  *
 *  *   it under the terms of the GNU General Public License as published by  *
 *  *   the Free Software Foundation; either version 2 of the License, or     *
 *  *   (at your option) any later version.                                   *
 *  *   See also COPYING, LICENSE and WARRANTY file                           *
 *  *                                                                         *
 *  * ***********************************************************************
 *
 * ${filecomment}
 * ${package_declaration}
 *
 * ${typecomment}
 * ${type_declaration}
 */

package playground.dziemke.teach.mz;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.tabularFileParser.TabularFileHandler;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParser;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParserConfig;

/**
 * Created by IntelliJ IDEA.
 * User: zilske
 * Date: 11/2/11
 * Time: 11:57 AM
 * To change this template use File | Settings | File Templates.
 */
public class CreateNetwork {

    public static void main(String[] args) {
        final Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        final Network network = scenario.getNetwork();
        String FILENAME = " ** FILENAME HERE ** ";
        TabularFileParserConfig tabularFileParserConfig = new TabularFileParserConfig();
        tabularFileParserConfig.setFileName(FILENAME);
        TabularFileHandler tabularFileHandler = new TabularFileHandler() {
            @Override
            public void startRow(String[] row) {
                String firstValue = row[0];
                String secondValue = row[1];
                // ...

                // Create a link from the current row.
                // Take care to create nodes, but no duplicates:

                Id fromNodeId = scenario.createId("** some node id **");
                Node fromNode;
                if (network.getNodes().containsKey(fromNodeId)) {
                    // Take existing node from scenario if it is already there!
                    fromNode = network.getNodes().get(fromNodeId);
                } else {
                    fromNode = network.getFactory().createNode(fromNodeId, scenario.createCoord(0.0, 0.0));
                    network.addNode(fromNode);
                }

                Id toNodeId = scenario.createId("** some node id **");
                Node toNode;
                if (network.getNodes().containsKey(fromNodeId)) {
                    // Take existing node from scenario if it is already there!
                    toNode = network.getNodes().get(fromNodeId);
                } else {
                    toNode = network.getFactory().createNode(toNodeId, scenario.createCoord(0.0, 0.0));
                    network.addNode(toNode);
                }


                Link link = network.getFactory().createLink(scenario.createId("** some link id **"), fromNode, toNode);
                // Set these attributes from the values of the row.
                link.setFreespeed(0.0);
                link.setCapacity(0.0);
                link.setNumberOfLanes(1.0);
                network.addLink(link);
            }
        };
        TabularFileParser tabularFileParser = new TabularFileParser();
        tabularFileParser.parse(tabularFileParserConfig, tabularFileHandler);
    }

}
