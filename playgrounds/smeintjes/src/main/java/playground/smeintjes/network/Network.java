/* 
 * Copyright (C) 2013 Maarten Houbraken
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Software available at https://github.com/mhoubraken/ISMAGS
 * Author : Maarten Houbraken (maarten.houbraken@intec.ugent.be)
 */
package playground.smeintjes.network;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import playground.smeintjes.motifs.MotifLink;
import playground.smeintjes.datastructures.NodeSet;
import java.util.List;
import java.util.Map;

/**
 *
 * @author mhoubraken
 */
public class Network {
//    Map<Integer,Link> links;
//    Map<Integer,Node> nodesByID;

    Map<String, Node> nodesByDescription;
    int nrLinks = 0;
    Map<MotifLink, Set<Node>> nodeSetsDepartingFromLink;
    Map<MotifLink, ArrayList<Node>> nodesWithLink;
    
    /**
     * Creates a new network without edges or nodes
     */
    public Network() {
//        nodesByID=new HashMap<Integer,Node>();
        nodesByDescription = new HashMap<String, Node>();
//        links=new HashMap<Integer,Link>();
        nodeSetsDepartingFromLink = new HashMap<MotifLink, Set<Node>>();

    }

    public void addNode(Node n) {
//        nodesByID.put(n.getID(), n);
        nodesByDescription.put(n.getDescription(), n);
    }

    public Node getNodeByDescription(String s) {
        return nodesByDescription.get(s);
    }

    /**
     * Adds a link to the network and updates the sets of nodes with edges of
     * the related type
     *
     * @param link
     */
    public void addLink(Link link) {
//        links.put(l.getID(), l);
        nrLinks++;
        int typeID = link.type.motifLink.getMotifLinkID();
        Set<Node> setOfLink = getSetOfType(link.type.motifLink);
        setOfLink.add(link.start);

        if (link.type.directed) {
            Set<Node> reverseSet = getSetOfType(link.type.inverseMotifLink);
            reverseSet.add(link.end);
        } else {
            setOfLink.add(link.end);
        }
        //adding to nodes
        NodeSet nodeList = link.start.neighboursPerType[typeID];
        if (nodeList == null) {
            nodeList = new NodeSet();
            link.start.neighboursPerType[typeID] = nodeList;
        }
        if (!nodeList.contains(link.end)) {
            nodeList.add(link.end);
            link.start.nrNeighboursPerType[typeID]++;
        }
        if (link.type.directed) {
            typeID = link.type.inverseMotifLink.getMotifLinkID();
        }
        nodeList = link.end.neighboursPerType[typeID];
        if (nodeList == null) {
            nodeList = new NodeSet();
            link.end.neighboursPerType[typeID] = nodeList;
        }
        if (!nodeList.contains(link.start)) {
            nodeList.add(link.start);
            link.end.nrNeighboursPerType[typeID]++;
        }
    }

    private Set<Node> getSetOfType(MotifLink type) {
        Set<Node> set = nodeSetsDepartingFromLink.get(type);
        if (set == null) {
            set = new HashSet<Node>();
            nodeSetsDepartingFromLink.put(type, set);
        }
        return set;
    }
    /**
     * Optimises network structure for further processing
     */
    public void finalizeNetworkConstruction() {
        Set<MotifLink> keySet = nodeSetsDepartingFromLink.keySet();
        nodesWithLink = new HashMap<MotifLink, ArrayList<Node>>(keySet.size());
        for (MotifLink motifLink : keySet) {
            Set<Node> nodes = nodeSetsDepartingFromLink.get(motifLink);
            ArrayList<Node> n = new ArrayList<Node>(nodes);
            Collections.sort(n);
            nodesWithLink.put(motifLink, n);
        }
        nodeSetsDepartingFromLink = null;
    }

    public ArrayList<Node> getNodesOfType(MotifLink m) {
        return nodesWithLink.get(m);
    }
}
