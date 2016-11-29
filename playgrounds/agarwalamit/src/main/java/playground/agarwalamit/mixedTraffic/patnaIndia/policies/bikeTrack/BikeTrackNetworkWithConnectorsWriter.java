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

package playground.agarwalamit.mixedTraffic.patnaIndia.policies.bikeTrack;

import java.util.*;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.NetworkWriter;
import org.matsim.core.utils.geometry.CoordUtils;
import playground.agarwalamit.mixedTraffic.patnaIndia.utils.PatnaUtils;
import playground.agarwalamit.utils.FileUtils;
import playground.agarwalamit.utils.LoadMyScenarios;

/**
 * This will merge the bike track to regular network.
 * Additionally, it will also add all possible connectors to the network.
 *
 * Created by amit on 24/09/16.
 */


public class BikeTrackNetworkWithConnectorsWriter {

    private final Network net;
    private static final double freeSpeed = 20.0/3.6;
    private static final double capacity = 1500.0;

    public static void main(String[] args) {
        String dir = FileUtils.RUNS_SVN+"/patnaIndia/run108/jointDemand/policies/0.15pcu/";
        String bikeNet = PatnaUtils.INPUT_FILES_DIR + "/simulationInputs/network/shpNetwork/bikeTrack.xml.gz";
        String regularNet = dir+"/input/network.xml.gz";
        String outNetFile = dir+"/input/networkWiBikeTrackAndConnectors.xml.gz";

        BikeTrackNetworkWithConnectorsWriter bikeTrackNetworkWithConnectorsWriter = new BikeTrackNetworkWithConnectorsWriter(regularNet);
        bikeTrackNetworkWithConnectorsWriter.processBikeTrackFile(bikeNet, 5.0, new HashSet<>(Arrays.asList("bike")));
        bikeTrackNetworkWithConnectorsWriter.writeNetwork(outNetFile);
    }

    BikeTrackNetworkWithConnectorsWriter(final String initialNetwork) {
        net = LoadMyScenarios.loadScenarioFromNetwork(initialNetwork).getNetwork();
        // dont want to do anything except removing isolated nodes,
        // that's why, not using core network simplifier or cleaner.
        playground.agarwalamit.utils.NetworkUtils.removeIsolatedNodes(net);
    }

    void processBikeTrackFile(final String bikeTrack, final double reduceLinkLengthByFactor, final Set<String> allowedModes) {
        Network bikeTrackNetwork = LoadMyScenarios.loadScenarioFromNetwork(bikeTrack).getNetwork();

        List<Node[]> nodesPairForConnectors = new ArrayList<>();
        for (Node bikeNode : bikeTrackNetwork.getNodes().values()) {
            Coord cord = bikeNode.getCoord();

            Node n = NetworkUtils.getNearestNode(net, cord);
            nodesPairForConnectors.add(new Node[]{bikeNode, n});
        }

        // add all the nodes to the network.
        bikeTrackNetwork.getNodes().values().forEach(bikeNode ->
                org.matsim.core.network.NetworkUtils.createAndAddNode(net, bikeNode.getId(), bikeNode.getCoord())
        );

        // add all bike track links
        for (Link l : bikeTrackNetwork.getLinks().values()) {
          // link must be re-created so that node objects are same.
            Node fromNode = net.getNodes().get(l.getFromNode().getId());
            Node toNode = net.getNodes().get(l.getToNode().getId());
            String linkId = PatnaUtils.BIKE_TRACK_PREFIX+l.getId().toString();
            Link lNew = org.matsim.core.network.NetworkUtils.createAndAddLink(net, Id.createLinkId(linkId), fromNode, toNode,
                    l.getLength() / reduceLinkLengthByFactor, freeSpeed, capacity, l.getNumberOfLanes());
            lNew.setAllowedModes(new HashSet<>(allowedModes));
        }

        // now add all possible connectors
        nodesPairForConnectors.forEach(pair -> createAndAddLink(pair, reduceLinkLengthByFactor, allowedModes));
    }

    void writeNetwork(final String outNetFile) {
        new NetworkWriter(net).write(outNetFile);
    }

    private void createAndAddLink( final Node[] nodes, final double reduceLinkLengthByFactor, final Set<String> allowedModes) {
        double dist = CoordUtils.calcEuclideanDistance(nodes[0].getCoord(), nodes[1].getCoord());

        nodes[0] = net.getNodes().get(nodes[0].getId());
        nodes[1] = net.getNodes().get(nodes[1].getId());

        {
            String id = PatnaUtils.BIKE_TRACK_CONNECTOR_PREFIX + net.getLinks().size();
            Id<Link> linkId = Id.createLinkId(id);
            Link l = NetworkUtils.createAndAddLink(net, linkId, nodes[0], nodes[1], dist/reduceLinkLengthByFactor,
                    freeSpeed, capacity, 1);
            l.setAllowedModes(allowedModes);
        }
        {
            String id = PatnaUtils.BIKE_TRACK_CONNECTOR_PREFIX + net.getLinks().size();
            Id<Link> linkId = Id.createLinkId(id);
            Link l = NetworkUtils.createAndAddLink(net, linkId, nodes[1], nodes[0], dist/reduceLinkLengthByFactor,
                    freeSpeed, capacity, 1);
            l.setAllowedModes(allowedModes);
        }
    }
}