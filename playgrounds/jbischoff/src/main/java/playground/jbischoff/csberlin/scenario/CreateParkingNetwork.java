/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
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

/**
 * 
 */
package playground.jbischoff.csberlin.scenario;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.parking.parkingsearch.ParkingUtils;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.tabularFileParser.TabularFileHandler;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParser;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParserConfig;
import org.matsim.facilities.ActivityFacilitiesFactory;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.ActivityOption;
import org.matsim.facilities.FacilitiesWriter;

/**
 * @author  jbischoff
 *
 */
/**
 *
 */
public class CreateParkingNetwork {

	public static void main(String[] args) {
		
		final Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
//		new MatsimNetworkReader(scenario.getNetwork()).readFile("../../../shared-svn/projects/bmw_carsharing/example/grid_network.xml");
		new MatsimNetworkReader(scenario.getNetwork()).readFile("../../../shared-svn/projects/bmw_carsharing/data/scenario/network.xml.gz");
		final ActivityFacilitiesFactory fac = scenario.getActivityFacilities().getFactory();
		TabularFileParserConfig config = new TabularFileParserConfig();
        config.setDelimiterTags(new String[] {"\t"});
        config.setFileName("../../../shared-svn/projects/bmw_carsharing/data/parkplaetze.txt");
        config.setCommentTags(new String[] { "#" });
        Network network2 = NetworkUtils.createNetwork();
        new TabularFileParser().parse(config, new TabularFileHandler() {

			@Override
			public void startRow(String[] row) {
				Id<Link> linkId = Id.createLinkId(row[0]);
				
				Link link = scenario.getNetwork().getLinks().get(linkId);
				
				Node outN = NetworkUtils.createNode(link.getToNode().getId(), link.getToNode().getCoord());
				if (!network2.getNodes().containsKey(outN.getId())) network2.addNode(outN);
				Node inN = NetworkUtils.createNode(link.getFromNode().getId(), link.getFromNode().getCoord());
				if (!network2.getNodes().containsKey(inN.getId())) network2.addNode(inN);
				Link nl = NetworkUtils.createLink(link.getId(), inN, outN, network2, link.getLength(), link.getFreespeed(), link.getCapacity(), link.getNumberOfLanes());
				if (!network2.getLinks().containsKey(nl.getId())) network2.addLink(nl);
			}
		
	});
        new NetworkWriter(network2).write("../../../shared-svn/projects/bmw_carsharing/data/parkplaetze-net.xml");

        
	}
	
}
