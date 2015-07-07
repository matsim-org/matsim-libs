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

import java.lang.reflect.Array;
import playground.smeintjes.motifs.MotifLink;
import playground.smeintjes.datastructures.NodeSet;

/**
 * Class representing node in graph
 */
public class Node implements Comparable<Node> {

    public boolean used = false;
    public int ID;
    int[] nrNeighboursPerType;
    public NodeSet[] neighboursPerType;
    String description;

    public Node(int ID, String description) {
        this.ID = ID;
        this.description = description;
        int nrMotifLinkTypes = MotifLink.getNrLinkIDs();
        nrNeighboursPerType = new int[nrMotifLinkTypes];
        neighboursPerType = (NodeSet[]) Array.newInstance(NodeSet.class, nrMotifLinkTypes);
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return description;
    }

    public int compareTo(Node o) {
        return ID - o.ID;
    }
}
