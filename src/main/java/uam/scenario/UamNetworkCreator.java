/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2019 by the members listed in the COPYING,        *
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
 * *********************************************************************** *
 */

package uam.scenario;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.accessibility.utils.MergeNetworks;
import org.matsim.contrib.util.distance.DistanceUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

/**
 * @author Michal Maciejewski (michalm)
 */
public class UamNetworkCreator {
	static final String UAM_MODE = "uam";

	static final ImmutableList<Id<Link>> SELECTED_LINK_IDS = ImmutableList.of(Id.createLinkId(187463),
			Id.createLinkId(174307), Id.createLinkId(356905), Id.createLinkId(224927), Id.createLinkId(67436),
			Id.createLinkId(341408));

	static final String HUB_LINK_ID_PREFIX = "HUB_";

	private static final double UAV_EFFECTIVE_CELL_SIZE = 7.5;//[m]; see DEFAULT_EFFECTIVE_CELL_SIZE

	//XXX could be split into EGRESS and ACCESS links, each 2 min long
	private static final double HUB_LINK_TRAVEL_TIME = 5 * 60;//[s]; 1 / 5-min frequency per port
	private static final double HUB_LINK_LENGTH = UAV_EFFECTIVE_CELL_SIZE;//[m] only one UAV per lane at a time
	private static final double HUB_LINK_NUM_LANES = 5;//[lanes] = number of ports at the hub
	private static final double HUB_LINK_SPEED = HUB_LINK_LENGTH / HUB_LINK_TRAVEL_TIME;// [m/s]
	private static final double HUB_LINK_FLOW_CAPACITY = 3600. * HUB_LINK_NUM_LANES / HUB_LINK_TRAVEL_TIME;// 60 [veh/h]

	private static final double RED_LINK_TRAVEL_TIME = 30;//[s]
	private static final double RED_LINK_LENGTH = 150;//[m]
	private static final double RED_LINK_NUM_LANES = UAV_EFFECTIVE_CELL_SIZE
			/ RED_LINK_LENGTH;//[lanes]; only 1 UAV at a time
	private static final double RED_LINK_SPEED = RED_LINK_LENGTH / RED_LINK_TRAVEL_TIME;//5 [m/s]
	private static final double RED_LINK_FLOW_CAPACITY = 3600. / RED_LINK_TRAVEL_TIME;// 120 [veh/h]

	private static final double GREEN_LINK_SPEED = 108 / 3.6;// [m/s]; 108 km/h
	private static final double GREEN_LINK_FLOW_CAPACITY = 60;//[veh/h]

	public static void main(String[] args) {
		//read Hanover network (CRS: ETRS89_UTM_zone_32N)
		Network hanoverNetwork = NetworkUtils.createNetwork();
		new MatsimNetworkReader(hanoverNetwork).readFile("input/uam/network.xml.gz");

		Network uamNetwork = NetworkUtils.createNetwork();
		List<Node> skyNodes = new ArrayList<>();
		Set<String> uamLinkModes = ImmutableSet.of(UAM_MODE);
		for (int i = 0; i < SELECTED_LINK_IDS.size(); i++) {
			// TODO currently they are in the middle of each link
			Coord hubCoord = hanoverNetwork.getLinks().get(SELECTED_LINK_IDS.get(i)).getCoord();

			// create HUB landing and takeoff nodes
			Node hubLandingNode = NetworkUtils.createAndAddNode(uamNetwork, Id.createNodeId("HUB_LANDING_" + i),
					new Coord(hubCoord.getX() - 0.5 * HUB_LINK_LENGTH, hubCoord.getY()));
			Node hubTakeoffNode = NetworkUtils.createAndAddNode(uamNetwork, Id.createNodeId("HUB_TAKEOFF" + i),
					new Coord(hubCoord.getX() + 0.5 * HUB_LINK_LENGTH, hubCoord.getY()));

			//create SKY node that corresponds to the UAM flying level
			//XXX for better visibility in VIA, use increase Y by RED_LINK_LENGTH (instead of Z)
			Coord skyCoord = new Coord(hubCoord.getX(), hubCoord.getY() + RED_LINK_LENGTH - 0.5 * HUB_LINK_LENGTH);
			Node skyNode = NetworkUtils.createAndAddNode(uamNetwork, Id.createNodeId("UAM_SKY_" + i), skyCoord);
			skyNodes.add(skyNode);

			// create RED links
			NetworkUtils.createAndAddLink(uamNetwork, Id.createLinkId("RED_LANDING_" + i), skyNode, hubLandingNode,
					RED_LINK_LENGTH, RED_LINK_SPEED, RED_LINK_FLOW_CAPACITY, RED_LINK_NUM_LANES)
					.setAllowedModes(uamLinkModes);
			NetworkUtils.createAndAddLink(uamNetwork, Id.createLinkId("RED_TAKEOFF_" + i), hubTakeoffNode, skyNode,
					RED_LINK_LENGTH, RED_LINK_SPEED, RED_LINK_FLOW_CAPACITY, RED_LINK_NUM_LANES)
					.setAllowedModes(uamLinkModes);

			// create access/egress link
			NetworkUtils.createAndAddLink(uamNetwork, Id.createLinkId(HUB_LINK_ID_PREFIX + i), hubLandingNode,
					hubTakeoffNode, HUB_LINK_LENGTH, HUB_LINK_SPEED, HUB_LINK_FLOW_CAPACITY, HUB_LINK_NUM_LANES)
					.setAllowedModes(uamLinkModes);
		}

		for (int i = 0; i < skyNodes.size(); i++) {
			Node fromNode = skyNodes.get(i);
			for (int j = 0; j < skyNodes.size(); j++) {
				if (i != j) {
					Node toNode = skyNodes.get(j);
					double length = DistanceUtils.calculateDistance(fromNode, toNode);
					NetworkUtils.createAndAddLink(uamNetwork, Id.createLinkId("GREEN_" + i + "_" + j), fromNode, toNode,
							length, GREEN_LINK_SPEED, GREEN_LINK_FLOW_CAPACITY, 1).setAllowedModes(uamLinkModes);
				}
			}
		}

		new NetworkWriter(uamNetwork).write("output/uam/uam_only_network.xml");
		MergeNetworks.merge(hanoverNetwork, "", uamNetwork);
		new NetworkWriter(hanoverNetwork).write("output/uam/network_with_uam.xml.gz");
	}
}
