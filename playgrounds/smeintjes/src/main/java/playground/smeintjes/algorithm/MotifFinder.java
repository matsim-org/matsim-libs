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
package playground.smeintjes.algorithm;

import playground.smeintjes.datastructures.NodeIterator;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import playground.smeintjes.datastructures.NodeSet;
import java.util.List;
import playground.smeintjes.motifs.Motif;
import playground.smeintjes.motifs.MotifInstance;
import playground.smeintjes.motifs.MotifLink;
import playground.smeintjes.network.Network;
import playground.smeintjes.network.Node;

/**
 * This class contains the main logic for finding all motif instances
 */
public class MotifFinder {

    private Network network;
    private SymmetryHandler symmetryHandler;
    private Set<Integer> unmappedNodes;

    /**
     * Default constructor for MotifFinder
     *
     * @param network network to be searched
     */
    public MotifFinder(Network network) {
        this.network = network;
    }

    /**
     * Finds and returns all instances of specified motif in the network
     *
     * @param motif Motif of which instances need to be found
     * @return All occurrences of the motif in the network
     */
    public Set<MotifInstance> findMotif(Motif motif) {
        //initially, no nodes are mapped
        unmappedNodes = new HashSet<Integer>();
        for (int i = 0; i < motif.getNrMotifNodes(); i++) {
            unmappedNodes.add(i);
        }
        int nrMotifNodes = motif.getNrMotifNodes();

        //determining first motif node to be investigated based on number of edges in network
        NodeIterator[] mapping = new NodeIterator[nrMotifNodes];
        int bestMN = -1;
        int sizeOfListOfBestNode = Integer.MAX_VALUE;
        for (int i = 0; i < nrMotifNodes; i++) {
            //determine nodes mapable on node i
            int[] nrLinks = new int[MotifLink.getNrLinkIDs()];
            MotifLink[] linksFromi = motif.getLinksOfMotifNode(i);
            int[] nodesConnectedToi = motif.getConnectionsOfMotifNode(i);
            int nrConnections = nodesConnectedToi.length;
            NodeIterator nodeIterator = new NodeIterator(i);
            int sizeOfSmallestListOfNodei = Integer.MAX_VALUE;
            //for each outgoing link, add the list of nodes in the network having that edge type
            for (int k = 0; k < nrConnections; k++) {
                MotifLink link = linksFromi[k];
                nrLinks[link.motifLinkID]++;
                if (nrLinks[link.motifLinkID] == 1) {
                    ArrayList<Node> nodesOfType = this.network.getNodesOfType(link);
                    nodeIterator.addRestrictionList(nodesOfType);
                    if (sizeOfSmallestListOfNodei > nodesOfType.size()) {
                        sizeOfSmallestListOfNodei = nodesOfType.size();
                    }
                }
            }
            //First node to be mapped is the node with the smallest candidate sublist
            if (sizeOfSmallestListOfNodei < sizeOfListOfBestNode) {
                sizeOfListOfBestNode = sizeOfSmallestListOfNodei;
                bestMN = i;
            }
            mapping[i] = nodeIterator;
        }
        Set<MotifInstance> instances = new HashSet<MotifInstance>();
        Node[] mappedNodes = new Node[nrMotifNodes];
        //Initialise symmetry handler to analyse motif
        symmetryHandler = new SymmetryHandler(mapping, motif, mappedNodes);
        mapNext(motif, instances, bestMN, mappedNodes, 0);
        return instances;
    }

    /**
     * Recursively called to map graph nodes to the next motif node
     *
     * @param motif subgraph to be searched for
     * @param instances set to store motif instances in
     * @param motifNode next node to be mapped
     * @param mappedNodes current partial node mapping
     * @param nrMapped number of nodes already in the partial mapping
     */
    private void mapNext(Motif motif, Set<MotifInstance> instances, int motifNode, Node[] mappedNodes, int nrMapped) {
        //get set of possible nodes to map on motifNode
        NodeSet nodes = symmetryHandler.mapping[motifNode].getNodeSet();
        //if the current node mapping will complete the mapping, export the instances
        if (nrMapped == motif.getNrMotifNodes() - 1) {
            for (Node node : nodes.getNodes()) {
                mappedNodes[motifNode] = node;
                instances.add(new MotifInstance(mappedNodes));
            }
            mappedNodes[motifNode] = null;
        } else {
            //for each possible node, map
            Iterator<Node> nodeIterator = nodes.iterator();
            symmetryHandler.mappedPositions.add(motifNode);
            unmappedNodes.remove(motifNode);
            while (nodeIterator.hasNext()) {
                Node n = nodeIterator.next();
                mappedNodes[motifNode] = n;
                n.used = true;
                //map graph node to motif node, early termination if graph node does not support all edges of motif node
                boolean succesMapping = symmetryHandler.mapNode(motifNode, n);
                if (succesMapping) {
                    //determine next node to be mapped
                    NodeIterator nextIterator = symmetryHandler.getNextBestIterator(unmappedNodes);
                    if (nextIterator != null) {// && nextIterator.nodes.size() > 0) {
                        symmetryHandler.mapping[nextIterator.motifNodeID] = nextIterator;
                        //recursively call mapNext
                        mapNext(motif, instances, nextIterator.motifNodeID, mappedNodes, nrMapped + 1);
                        //backtracking
                        symmetryHandler.mapping[nextIterator.motifNodeID] = nextIterator.parent;
                    }
                }
                //backtracking
                symmetryHandler.removeNodeMapping(motifNode, n);
                n.used = false;
                mappedNodes[motifNode] = null;
            }
            symmetryHandler.mappedPositions.remove(motifNode);
            unmappedNodes.add(motifNode);
        }
    }
}
