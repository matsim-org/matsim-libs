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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import playground.agarwalamit.utils.LoadMyScenarios;

/**
 * Created by amit on 24/09/16.
 */


public class BikeTrackConnectionIdentifier {

    private final Network net ;
    private final Map<Id<Link>,Link> connectedLinks = new HashMap<>();
    private final Network bikeTrackNetwork;

    BikeTrackConnectionIdentifier(final String initialNetwork, final String bikeTrack){
        net = LoadMyScenarios.loadScenarioFromNetwork(initialNetwork).getNetwork();

        // remove unused nodes
        List<Node> nodes2remove = new ArrayList<>();
        for(Node n: net.getNodes().values()) {
            if( n.getInLinks().size() == 0 && n.getOutLinks().size() == 0) nodes2remove.add(n);
        }

        for(Node n : nodes2remove) {net.removeNode(n.getId());}

        bikeTrackNetwork = LoadMyScenarios.loadScenarioFromNetwork(bikeTrack).getNetwork();
    }

    void run(){
        for (Node bikeNode: bikeTrackNetwork.getNodes().values()){
            Coord cord = bikeNode.getCoord();

            Node n = NetworkUtils.getNearestNode(net, cord);
            createAndAddLinkToList(net,new Node [] { bikeNode, n } );
        }
    }

    private void createAndAddLinkToList(final Network network, final Node [] nodes) {
        double dist = CoordUtils.calcEuclideanDistance(nodes[0].getCoord(), nodes[1].getCoord());
        {
            String id = "connecter_link_"+ connectedLinks.size();
            Id<Link> linkId = Id.createLinkId(id);
            Link l = network.getFactory().createLink(linkId, nodes[0], nodes[1]);
            l.setLength(dist);
            l.setCapacity(900); // half of the one lane capacity
            l.setFreespeed(60/3.6);
            l.setNumberOfLanes(1);
            connectedLinks.put(l.getId(),l);
        }
        {
            String id = "connecter_link_"+ connectedLinks.size();
            Id<Link> linkId = Id.createLinkId(id);
            Link l = network.getFactory().createLink(linkId, nodes[1], nodes[0]);
            l.setLength(dist);
            l.setCapacity(900); // half of the one lane capacity
            l.setFreespeed(60/3.6);
            l.setNumberOfLanes(1);
            connectedLinks.put(l.getId(),l);
        }
    }

    public Map<Id<Link>,Link> getConnectedLinks() {
        return connectedLinks;
    }

    public Network getBikeTrackNetwork(){
        return bikeTrackNetwork;
    }
}
