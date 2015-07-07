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
package playground.smeintjes.datastructures;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import playground.smeintjes.motifs.Motif;
import playground.smeintjes.motifs.MotifLink;
import playground.smeintjes.network.LinkType;

/**
 * Keeps track of the OPP state during the motif analysis
 */
public class SymGraph {

    public int[] topMotifnodeToColor;
    public Map<Integer, List<Integer>> colorToTopMotifnode;
    public Map<Integer, List<Integer>> colorToBottomMotifnode;
    public Motif motif;
    Set<Integer> colorsToRecheck;

    /**
     * Create an initial OPP
     *
     * @param motif motif to be analysed
     */
    public SymGraph(Motif motif) {
        this.motif = motif;
        colorsToRecheck = new HashSet<Integer>();
        topMotifnodeToColor = new int[motif.getNrMotifNodes()];
        colorToBottomMotifnode = new HashMap<Integer, List<Integer>>();
        colorToTopMotifnode = new HashMap<Integer, List<Integer>>();
        ArrayList<Integer> list1 = new ArrayList<Integer>();
        ArrayList<Integer> list2 = new ArrayList<Integer>();
        for (int i = 0; i < topMotifnodeToColor.length; i++) {
            list1.add(i);
            list2.add(i);
        }
        colorToBottomMotifnode.put(0, list2);
        colorToTopMotifnode.put(0, list1);
    }

    /**
     * Performs a motif refinement, starting with a specific motif partition
     * cell/color that needs refinement
     *
     * @param color starting cell for refinement
     * @return false if resulting OPP is invalid
     */
    public boolean refineColors(int color) {
        boolean ok = refine(color);
        while (ok && !colorsToRecheck.isEmpty()) {
            int colorToCheck = colorsToRecheck.iterator().next();
            ok = refine(colorToCheck);
            colorsToRecheck.remove((Integer) colorToCheck);
        }
        return ok;
    }

    /**
     * Performs one step of the refinement procedure
     *
     * @param color cell to be refined
     * @return true if refinement was successful
     */
    private boolean refine(int color) {
        int nrMotifNodes = motif.getNrMotifNodes();
        int[][] degreesTop = new int[nrMotifNodes][];
        List<Integer> topnodes = colorToTopMotifnode.get(color);
        int[][] degreesBottom = new int[nrMotifNodes][];
        List<Integer> bottomnodes = colorToBottomMotifnode.get(color);
        int nrTypes=LinkType.getNrLinkTypes();
        for (int i = 0; i < nrMotifNodes; i++) {
            degreesBottom[i] = new int[ nrTypes* 2];
            degreesTop[i] = new int[nrTypes * 2];
        }
        HashSet<Integer> reachedColors = new HashSet<Integer>();
        for (Integer integer : topnodes) {
            MotifLink[] links = motif.getLinksOfMotifNode(integer);
            int[] linksd = motif.getConnectionsOfMotifNode(integer);
            for (int j = 0; j < links.length; j++) {
                int i = linksd[j];
                MotifLink motifLink = links[j];
                if (motifLink.linkType.getMotifLink() == motifLink) {
                    degreesTop[i][motifLink.linkType.getLinkTypeID()]++;
                    degreesTop[integer][nrTypes + motifLink.linkType.getLinkTypeID()]++;
                } else {
                    degreesTop[integer][motifLink.linkType.getLinkTypeID()]++;
                    degreesTop[i][nrTypes + motifLink.linkType.getLinkTypeID()]++;
                }
                reachedColors.add(topMotifnodeToColor[i]);
            }
        }
        for (Integer integer : bottomnodes) {
            MotifLink[] links = motif.getLinksOfMotifNode(integer);
            int[] linksd = motif.getConnectionsOfMotifNode(integer);
            for (int j = 0; j < links.length; j++) {
                int i = linksd[j];
                MotifLink motifLink = links[j];
                if (motifLink.linkType.getMotifLink() == motifLink) {
                    degreesBottom[i][motifLink.linkType.getLinkTypeID()]++;
                    degreesBottom[integer][nrTypes + motifLink.linkType.getLinkTypeID()]++;
                } else {
                    degreesBottom[integer][motifLink.linkType.getLinkTypeID()]++;
                    degreesBottom[i][nrTypes + motifLink.linkType.getLinkTypeID()]++;
                }
            }
        }

        for (Integer integer : reachedColors) {
            List<Integer> nodesInColor = colorToTopMotifnode.get(integer);
            HashMap<Integer, int[]> currentColorMapping = new HashMap<Integer, int[]>();
            currentColorMapping.put(integer, degreesTop[nodesInColor.get(0)]);
            ArrayList<Integer> startset = new ArrayList<Integer>();
            startset.add(nodesInColor.get(0));
            colorToTopMotifnode.put(integer, startset);
            for (int i = 1; i < nodesInColor.size(); i++) {
                int node = nodesInColor.get(i);
                int[] is = degreesTop[node];
                boolean added = false;
                for (Map.Entry<Integer, int[]> entry : currentColorMapping.entrySet()) {
                    int[] connectionsColor = entry.getValue();
                    if (compareRows(connectionsColor, is) == true) {
                        colorToTopMotifnode.get(entry.getKey()).add(node);
                        topMotifnodeToColor[node] = entry.getKey();
                        added = true;
                        break;
                    }
                }
                if (!added) {
                    int newColor = getFreeColor();
                    colorsToRecheck.add(newColor);
                    colorsToRecheck.add(color);
                    ArrayList<Integer> newSet = new ArrayList<Integer>();
                    newSet.add(node);
                    currentColorMapping.put(newColor, is);
                    colorToTopMotifnode.put(newColor, newSet);
                    topMotifnodeToColor[node] = newColor;
                }
            }
            List<Integer> nodesInBottomColor = colorToBottomMotifnode.get(integer);
            colorToBottomMotifnode.remove(integer);
            out:
            for (int i = 0; i < nodesInBottomColor.size(); i++) {
                int nodeID = nodesInBottomColor.get(i);
                int[] is = degreesBottom[nodeID];
                for (Map.Entry<Integer, int[]> entry : currentColorMapping.entrySet()) {
                    if (compareRows(entry.getValue(), is) == true) {
                        int colorForBottom = entry.getKey();
                        List<Integer> get = colorToBottomMotifnode.get(colorForBottom);
                        if (get == null) {
                            get = new ArrayList<Integer>();
                            colorToBottomMotifnode.put(colorForBottom, get);
                        }
                        get.add(nodeID);
                        continue out;
                    }
                }
            }
            for (Integer integer1 : colorToTopMotifnode.keySet()) {
                List<Integer> bottomset = colorToBottomMotifnode.get(integer1);
                List<Integer> topset = colorToTopMotifnode.get(integer1);
                if (bottomset == null || topset.size() != bottomset.size()) {
                    return false;
                }
            }
        }
        return true;
    }

    private int getFreeColor() {
        return colorToTopMotifnode.size();
    }

    private boolean compareRows(int[] a, int[] b) {
        for (int i = 0; i < b.length; i++) {
            if (a[i] != b[i]) {
                return false;
            }
        }
        return true;
    }

    /**
     * Maps a specific node in the top partition to a node in the bottom
     * partition
     *
     * @param topID motif node in top partition
     * @param bottomID motif node in bottom partition
     * @param splitcolor ID of partition cell
     * @return new OPP state
     */
    public SymGraph map(int topID, int bottomID, int splitcolor) {
        SymGraph newSym = new SymGraph(motif);

        System.arraycopy(topMotifnodeToColor, 0, newSym.topMotifnodeToColor, 0, topMotifnodeToColor.length);
        for (Map.Entry<Integer, List<Integer>> entry : colorToBottomMotifnode.entrySet()) {
            ArrayList<Integer> n = new ArrayList<Integer>(entry.getValue());
            newSym.colorToBottomMotifnode.put(entry.getKey(), n);
        }
        for (Map.Entry<Integer, List<Integer>> entry : colorToTopMotifnode.entrySet()) {
            ArrayList<Integer> n = new ArrayList<Integer>(entry.getValue());
            newSym.colorToTopMotifnode.put(entry.getKey(), n);
        }
        newSym.colorToBottomMotifnode.get(splitcolor).remove((Integer) bottomID);
        newSym.colorToTopMotifnode.get(splitcolor).remove((Integer) topID);
        ArrayList<Integer> listtop = new ArrayList<Integer>(1);
        ArrayList<Integer> listbottom = new ArrayList<Integer>(1);
        int newColor = getFreeColor();
        newSym.colorToBottomMotifnode.put(newColor, listbottom);
        newSym.colorToTopMotifnode.put(newColor, listtop);
        listtop.add(topID);
        listbottom.add(bottomID);
        boolean ok = newSym.refineColors(newColor);
        if (ok) {
            return newSym;
        } else {
            return null;
        }
    }
}
