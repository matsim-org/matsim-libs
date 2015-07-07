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
import playground.smeintjes.datastructures.SymProp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import playground.smeintjes.datastructures.NodeSet;
import playground.smeintjes.datastructures.PriorityObject;
import playground.smeintjes.datastructures.PriorityQueueMap;
import playground.smeintjes.datastructures.SymGraph;
import java.util.List;
import java.util.Map;
import playground.smeintjes.motifs.Motif;
import playground.smeintjes.motifs.MotifLink;
import playground.smeintjes.network.Node;

/**
 * This class is responsible for analysing the motif and providing the
 * constraints to the MotifFinder class
 */
class SymmetryHandler {

    int nrOrbits = 0;
    int[] orbits;
    Set<Integer> mappedPositions;
    Map<Integer, Set<Integer>> smaller;
    Map<Integer, Set<Integer>> larger;
    NodeIterator[] mapping;
    Node[] mappedNodes;
    PriorityQueueMap PQmap;
    private Motif motif;

    /**
     * Constructs a SymmetryHandler object to deal with the specified motif
     *
     * @param mapping handle to NodeIterators containing constraining neighbour
     * lists
     * @param motif motif to be analysed
     * @param mappedNodes handle to partial node mapping
     */
    public SymmetryHandler(NodeIterator[] mapping, Motif motif, Node[] mappedNodes) {
        PQmap = new PriorityQueueMap(mapping.length);
        this.mapping = mapping;
        this.motif = motif;
        mappedPositions = new HashSet<Integer>(mappedNodes.length);
        this.mappedNodes = mappedNodes;
        smaller = new HashMap<Integer, Set<Integer>>();
        larger = new HashMap<Integer, Set<Integer>>();
        SymProp sp = analyseMotif(motif);
    }

    /**
     * Determines the next motif nodes and candidates to be mapped
     *
     * @param unmappedMotifNodes unmapped motif nodes from which to select the
     * next node
     * @return next motif node and graph node candidates
     */
    NodeIterator getNextBestIterator(Set<Integer> unmappedMotifNodes) {
        //get next node to be mapped by polling priority map
        PriorityObject poll = PQmap.poll(unmappedMotifNodes);
        int motifNodeID = poll.getTo();
        NodeIterator r = mapping[motifNodeID];
        //determine lower bound for graph node candidates
        Set<Integer> minset = larger.get(motifNodeID);
        Node minNode = null;
        int min = Integer.MIN_VALUE;
        if (minset != null) {
            for (Integer integer : minset) {
                if (mappedPositions.contains(integer) && min < mappedNodes[integer].ID) {
                    min = mappedNodes[integer].ID;
                    minNode = mappedNodes[integer];
                }
            }
        }
        //determine upper bound for graph node candidates
        Set<Integer> maxset = smaller.get(motifNodeID);
        Node maxNode = null;
        int max = Integer.MAX_VALUE;
        if (maxset != null) {
            for (Integer integer : maxset) {
                if (mappedPositions.contains(integer) && max > mappedNodes[integer].ID) {
                    max = mappedNodes[integer].ID;
                    maxNode = mappedNodes[integer];
                    //abort when bounds conflict
                    if (min > max) {
                        return null;
                    }
                }
            }
        }
        //determine nodes by intersecting using the bounds
        NodeIterator intersect = r.intersect(minNode, maxNode);
        return intersect;
    }

    /**
     * Maps a graph node to a motif node and updates the neighbour lists used
     * for intersecting
     *
     * @param motifNode motif node to be mapped on
     * @param n graph node to be mapped
     * @return returns true if node is suitable for mapping on the motif node
     */
    boolean mapNode(int motifNode, Node n) {
        int[] connections = motif.getConnectionsOfMotifNode(motifNode);
        MotifLink[] restrictions = motif.getLinksOfMotifNode(motifNode);
        int nrConnections = connections.length;
        for (int j = 0; j < nrConnections; j++) {
            int i = connections[j];
            if (mappedNodes[i] != null) {
                continue;
            }
            MotifLink motifLink = restrictions[j];
            NodeSet ln = n.neighboursPerType[motifLink.motifLinkID];
            if (ln == null) {
                return false;
            } else {
                mapping[i].addRestrictionList(ln, n);
                int size = ln.size();
                PQmap.add(new PriorityObject(n, motifNode, i, size));
            }
        }
        return true;
    }

    /**
     * Un-maps a graph node previously mapped to a motif node, ensuring
     * consistency in constraining neighbour lists
     *
     * @param motifNode motif node mapped to
     * @param graphNode graph node mapped to motif node
     */
    void removeNodeMapping(int motifNode, Node graphNode) {
        int[] neighbours = motif.getConnectionsOfMotifNode(motifNode);
        for (int i : neighbours) {
            mapping[i].removeRestrictionList(graphNode);
            PQmap.remove(motifNode, i);
        }
    }

    /**
     * Updates the orbit partitioning by merging the orbit partition cells of
     * the specified motif nodes
     *
     * @param a first cell to be merged
     * @param b second cell to be merged
     * @param orbits current orbit partition
     */
    private void mergeOrbits(int a, int b, int[] orbits) {
        int orbita = orbits[a];
        int orbitb = orbits[b];
        if (orbitb == -1 && orbita == -1) {
            orbits[a] = ++nrOrbits;
            orbits[b] = nrOrbits;
        } else if (orbitb == -1) {
            orbits[b] = orbita;
        } else if (orbita == -1) {
            orbits[a] = orbitb;
        } else {
            for (int i = 0; i < orbits.length; i++) {
                if (orbits[i] == orbita) {
                    orbits[i] = orbitb;
                }
            }
        }
    }

    /**
     * Initialises and returns full motif analysis
     *
     * @param motif motif to be analysed
     * @return
     */
    private SymProp analyseMotif(Motif motif) {
        int nrMotifNodes = motif.getNrMotifNodes();
        SymGraph sym = new SymGraph(motif);
        SymProp sp = new SymProp(nrMotifNodes, smaller, larger);
        orbits = new int[nrMotifNodes];
        for (int i = 0; i < orbits.length; i++) {
            orbits[i] = -1;
        }
        mapNodes(sp, orbits, sym, true);
        return sp;
    }

    /**
     * Recursive motif analysis
     *
     * @param symmetricProperties stores all permutations and symmetry-breaking
     * constraints for the motif
     * @param orbits orbit partitioning of the motif nodes
     * @param symGraph current state in motif analysis
     * @param main true if all previously coupled motif nodes are coupled to
     * themselves
     */
    private void mapNodes(SymProp symmetricProperties, int[] orbits, SymGraph symGraph, boolean main) {
        boolean allone = true;
        int splitcolor = -1;
        int lowestUnassignedMotifNode = Integer.MAX_VALUE;
        //determining next motif node to map
        outfor:
        for (int i = 0; i < symGraph.colorToTopMotifnode.size(); i++) {
            List<Integer> listi = symGraph.colorToTopMotifnode.get(i);
            int sizei = listi.size();
            if (sizei != 1) {
                allone = false;
                for (Integer motifNodeID : listi) {
                    if (motifNodeID < lowestUnassignedMotifNode) {
                        splitcolor = i;
                        lowestUnassignedMotifNode = motifNodeID;
                        continue outfor;
                    }
                }
            }
        }
        //if all nodes are mapped, export permutation
        if (allone) {
            int[] perm = new int[symGraph.motif.getNrMotifNodes()];
            for (int j = 0; j < perm.length; j++) {
                int bottomcolor = symGraph.colorToBottomMotifnode.get(j).get(0);
                int topcolor = symGraph.colorToTopMotifnode.get(j).get(0);
                perm[topcolor] = bottomcolor;
                mergeOrbits(bottomcolor, topcolor, orbits);
            }
            symmetricProperties.addPermutation(perm);
            return;
        }
        //map lowest uncoupled node in the upper partition on all motif nodes in the lower partition
        List<Integer> topsplit = symGraph.colorToTopMotifnode.get(splitcolor);
        int top = lowestUnassignedMotifNode;
        List<Integer> bottomsplit = symGraph.colorToBottomMotifnode.get(splitcolor);
        SymGraph newSymGraph = symGraph;
        //deal with the initial OPP and the cases for which the OPP has identical subsets
        if (topsplit.size() != symGraph.motif.getNrMotifNodes() && topsplit.containsAll(bottomsplit)) {
            int[] perm = new int[symGraph.motif.getNrMotifNodes()];
            boolean identityPermutation = true;
            for (int k = 0; k < newSymGraph.colorToBottomMotifnode.size(); k++) {
                List<Integer> bottomnodes = newSymGraph.colorToBottomMotifnode.get(k);
                List<Integer> topnodes = newSymGraph.colorToTopMotifnode.get(k);
                int bottomnode = bottomnodes.get(0);
                int topnode = topnodes.get(0);
                if (bottomnode != topnode) {
                    mergeOrbits(bottomnode, topnode, orbits);
                }
            }
            int j = 0;

            start:
            for (; j < newSymGraph.colorToBottomMotifnode.size(); j++) {
                List<Integer> bottomnodes = newSymGraph.colorToBottomMotifnode.get(j);
                List<Integer> topnodes = newSymGraph.colorToTopMotifnode.get(j);
                if (bottomnodes.size() > 1) {
                    int b = bottomnodes.get(0);
                    int t = topnodes.get(0);
                    newSymGraph = newSymGraph.map(t, b, j);
                    if (newSymGraph.colorToBottomMotifnode.size() != symGraph.motif.getNrMotifNodes()) {
                        j = -1;
                        continue start;
                    } else {
                        break;
                    }
                }
            }
            for (j = 0; j < newSymGraph.colorToBottomMotifnode.size(); j++) {
                List<Integer> bottomnodes = newSymGraph.colorToBottomMotifnode.get(j);
                List<Integer> topnodes = newSymGraph.colorToTopMotifnode.get(j);
                int bottomnode = bottomnodes.get(0);
                int topnode = topnodes.get(0);
                perm[topnode] = bottomnode;
                if (bottomnode != topnode) {
                    identityPermutation = false;
                }
            }

            if (!identityPermutation) {
                symmetricProperties.addPermutation(perm);
                return;
            }
        }
        //iterate over all possible couplings
        for (int j = 0; j < bottomsplit.size(); j++) {
            int m = bottomsplit.get(j);
            if (orbits[top] != -1 && orbits[top] == orbits[m]) {
                continue;
            }
            SymGraph symm = symGraph.map(top, m, splitcolor);
            //keep track of couplings and if the first are coupled to themselves
            boolean nm = main && (m == top);
            if (symm != null) {
                mapNodes(symmetricProperties, orbits, symm, nm);
            }
        }
        //export partial orbit cells as symmetry-breaking constraints
        if (main) {
            symmetricProperties.fix(top, orbits);
        }
    }
}
