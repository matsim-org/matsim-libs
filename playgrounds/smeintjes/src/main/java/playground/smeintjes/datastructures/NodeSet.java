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
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import playground.smeintjes.network.Node;

/**
 * Represents the list of candidate nodes
 */
public class NodeSet {

    public ArrayList<Node> nodes;

    /**
     * Create a NodeSet with list of nodes specified
     *
     * @param nodes list of candidate nodes
     */
    public NodeSet(ArrayList<Node> nodes) {
        this.nodes = nodes;
    }

    /**
     * Create a NodeSet with an empty list of candidate nodes
     */
    public NodeSet() {
        nodes = new ArrayList<Node>();
    }

    /**
     * Returns the size of the NodeSet
     *
     * @return number of candidate graph nodes
     */
    public int size() {
        return nodes.size();
    }

    public boolean isEmpty() {
        return nodes.isEmpty();
    }

    public Iterable<Node> getNodes() {
        return nodes;
    }

    /**
     * Checks for membership of a node to the NodeSet. Checking is done with
     * binary search on the candidates, assuming an ordered list.
     *
     * @param node node to be found
     * @return true if node is in the candidate set
     */
    public boolean contains(Node node) {
        int low = 0;
        int high = nodes.size() - 1;

        while (low <= high) {
            int mid = (low + high) >>> 1;
            Node midVal = nodes.get(mid);
            if (midVal.ID < node.ID) {
                low = mid + 1;
            } else if (midVal.ID > node.ID) {
                high = mid - 1;
            } else {
                return true;
            }
        }
        return false;
    }

    public void add(Node node) {
        nodes.add(node);
    }

    public Iterator iterator() {
        return nodes.iterator();
    }

    public void sort() {
        Collections.sort(nodes);
    }

    public void trimToSize() {
        nodes.trimToSize();
    }
}
