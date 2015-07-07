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
package playground.smeintjes.motifs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import playground.smeintjes.network.LinkType;

/**
 * Represents a motif to be searched in the graph
 */
public class Motif {

    private int nrMotifNodes;
    Set<int[]> permutations;
    private MotifLink[][] links;
    private Map<Integer, ArrayList<Integer>> initialConnections;
    private int[][] finalConnections;

    /**
     * Creates a new motif without any edges
     *
     * @param nrMotifNodes
     */
    public Motif(int nrMotifNodes) {
        links = new MotifLink[nrMotifNodes][nrMotifNodes];
        initialConnections = new HashMap<Integer, ArrayList<Integer>>(nrMotifNodes);
        this.nrMotifNodes = nrMotifNodes;
        permutations = new HashSet<int[]>();
    }

    public void addMotifLink(int startMotifNode, int endMotifNode, LinkType l) {
        links[startMotifNode][endMotifNode] = l.getMotifLink();
        links[endMotifNode][startMotifNode] = l.getInverseMotifLink();
        ArrayList<Integer> s1 = initialConnections.get(startMotifNode);
        if (s1 == null) {
            s1 = new ArrayList<Integer>();
            initialConnections.put(startMotifNode, s1);
        }
        s1.add(endMotifNode);
        ArrayList<Integer> s2 = initialConnections.get(endMotifNode);
        if (s2 == null) {
            s2 = new ArrayList<Integer>();
            initialConnections.put(endMotifNode, s2);
        }
        s2.add(startMotifNode);
    }

    void addPermutation(int[] p) {
        permutations.add(p);
    }

    public int getNrMotifNodes() {
        return nrMotifNodes;
    }

    /**
     * Finalises motif construction by optimising internal data structures for
     * further use in the algorithm
     */
    public void finaliseMotif() {
        finalConnections = new int[nrMotifNodes][];
        for (int i = 0; i < nrMotifNodes; i++) {
            ArrayList<Integer> a = initialConnections.get(i);
            finalConnections[i] = new int[a.size()];
            MotifLink[] finalLinks = new MotifLink[a.size()];
            for (int j = 0; j < a.size(); j++) {
                finalConnections[i][j] = a.get(j);
                finalLinks[j] = links[i][a.get(j)];
            }
            links[i] = finalLinks;
        }
        initialConnections = null;
    }

    public int[] getConnectionsOfMotifNode(int motifNodeID) {
        return finalConnections[motifNodeID];
    }

    public MotifLink[] getLinksOfMotifNode(int motifNodeID) {
        return links[motifNodeID];
    }
}
