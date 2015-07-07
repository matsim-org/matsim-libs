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

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;
import java.util.Stack;
import playground.smeintjes.network.Node;

/**
 * Keeps track of all lists that need to be intersected to obtain candidates
 * graph nodes. Initial
 */
public class NodeIterator {

    public int motifNodeID;
    private NodeSet nodes;
    Stack<NodeSet> neighbourLists;
    List<ArrayList<Node>> initialLists;
    int minSetSize;
    public NodeIterator parent;
    Stack<Node> nodeCausingRestriction;

    /**
     * Create a NodeIterator with an initial set of nodes
     *
     * @param nodes node candidates
     * @param motifNodeID motifnode for which the nodes are candidates
     */
    private NodeIterator(NodeSet nodes, NodeIterator parent) {
        this.nodes = nodes;
        this.parent = parent;
        this.motifNodeID = parent.motifNodeID;
        neighbourLists = new Stack<NodeSet>();
        nodeCausingRestriction = new Stack<Node>();
        minSetSize = nodes.size();
    }

    /**
     * Create a NodeIterator without initial set of nodes, used during
     * initialisation
     *
     * @param motifNodeID motifnode for which the NodeIterator will determine
     * candidates
     */
    public NodeIterator(int motifNodeID) {
        this.motifNodeID = motifNodeID;
        initialLists = new ArrayList<ArrayList<Node>>();
        neighbourLists = new Stack<NodeSet>();
        nodeCausingRestriction = new Stack<Node>();
        minSetSize = Integer.MAX_VALUE;
    }

    /**
     * Creates a NodeIterator based on the constraint lists
     *
     * @param min lower bound on the nodes (ID-based)
     * @param max upper bound on the nodes (ID-based)
     * @return child NodeIterator object, null if no candidates were found
     */
    public NodeIterator intersect(Node min, Node max) {
        if (neighbourLists.isEmpty()) {
            return null;
        }
        NodeSet result = new NodeSet();
        int stacksize = neighbourLists.size();
        int smallestSet = stacksize - 1;
        NodeSet nset = neighbourLists.get(smallestSet);
        if (nset.size() > minSetSize) {//minsetsize is lazy updated
            for (int i = 0; i < stacksize - 1; i++) {
                NodeSet nodeSet = neighbourLists.get(i);
                if (nodeSet.size() < minSetSize) {
                    nset = nodeSet;
                    minSetSize = nset.size();
                    smallestSet = i;
                }
            }
        }
        List<Node> nodes = nset.nodes;
        int startIndex = 0;
        if (min != null) {
            int p = Collections.binarySearch(nodes, min);
            if (p >= 0) {
                startIndex = p + 1;
            } else {
                startIndex = -p - 1;
            }
        }
        int endIndex = nodes.size();
        if (max != null) {
            int p = Collections.binarySearch(nodes, max);
            if (p >= 0) {
                endIndex = p;
            } else {
                endIndex = -p - 1;
            }
        }
        ListIterator<Node> listIterator = nodes.listIterator(startIndex);
        f:
        for (int k = startIndex; k < endIndex; k++) {
            Node node = listIterator.next();
            if (node.used == true) {
                continue;
            }

            int i = 0;
            for (; i < neighbourLists.size(); i++) {
                if (i != smallestSet && !neighbourLists.get(i).contains(node)) {
                    continue f;
                }
            }
            result.add(node);
        }
        if (result.isEmpty()) {
            return null;
        }
        NodeIterator newIterator = new NodeIterator(result, this);
        return newIterator;
    }

    /**
     * Adds a new list of candidate nodes for the motif node to the set of
     * constraining lists
     *
     * @param list list of candidate nodes
     * @param node graph node causing the constraint
     */
    public void addRestrictionList(NodeSet list, Node node) {
        if (!neighbourLists.contains(list)) {
            neighbourLists.push(list);
            if (list.size() < minSetSize) {
                minSetSize = list.size();
            }
            nodeCausingRestriction.push(node);
        }
    }

    /**
     * Adds a new list of candidate nodes for the motif node during the
     * initialisation
     *
     * @param list list of candidate nodes
     */
    public void addRestrictionList(ArrayList<Node> list) {
        if (initialLists == null) {
            initialLists = new ArrayList<ArrayList<Node>>();
        }
        if (list.size() < minSetSize) {
            initialLists.add(0, list);
            minSetSize = list.size();
        } else {
            initialLists.add(list);
        }
    }

    /**
     * Removes the constraining list of candidate nodes induced by the graph
     * node
     *
     * @param n graph node inducing the constraining list
     */
    public void removeRestrictionList(Node n) {
        while (!nodeCausingRestriction.empty() && nodeCausingRestriction.peek() == n) {
            neighbourLists.pop();
            nodeCausingRestriction.pop();
        }
    }

    /**
     * Returns the candidate graph nodes. If no set has been calculated
     * (=initially) a linear sweep is used
     *
     * @return list of candidate nodes
     */
    public NodeSet getNodeSet() {
        if (nodes != null) {
            return nodes;
        } else {
            NodeSet ret = new NodeSet();
            int size = initialLists.size();
            if (size == 1) {
                NodeSet ns = new NodeSet(initialLists.get(0));
                return ns;
            }
            ListIterator<Node>[] its = (ListIterator<Node>[]) Array.newInstance(ListIterator.class, size);

            for (int i = 0; i < size; i++) {
                its[i] = initialLists.get(i).listIterator();
            }
            if (!its[0].hasNext()) {
                return null;
            }
            Node ni = its[0].next();
            int j = 1;
            int sim = 1;
            out:
            while (true) {
                if (!its[j].hasNext()) {
                    break out;
                }
                Node nj = its[j].next();
                while (ni.ID > nj.ID) {
                    if (!its[j].hasNext()) {
                        break out;
                    }
                    nj = its[j].next();
                    sim = 1;
                }
                while (ni.ID < nj.ID) {
                    if (!its[0].hasNext()) {
                        break out;
                    }
                    ni = its[0].next();
                    sim = 1;
                }
                if (nj == ni) {
                    sim++;
                    if (sim == size) {
                        ret.add(nj);
                        sim = 1;
                    }
                } else {
                    sim = 1;
                }
                j = ((j) % (size - 1)) + 1;
            }
            return ret;
        }
    }
}
