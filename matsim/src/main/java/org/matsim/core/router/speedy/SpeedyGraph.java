package org.matsim.core.router.speedy;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;

import java.util.Arrays;

/**
 * Implements a highly optimized data structure for representing a MATSim network. Optimized to use as little memory as possible, and thus to fit as much memory as possible into CPU caches for high
 * performance.
 * <p>
 * Inspired by GraphHopper's Graph data structure as described in https://github.com/graphhopper/graphhopper/blob/master/docs/core/technical.md. GraphHopper uses bi-directional links, while MATSim
 * uses uni-directional links. Thus, instead of having nodeA and nodeB, we always have from- and to-node. This implies that one `node-row` requires two link-ids: one for the first in-link, and one for
 * the first out-link.
 * <p>
 * We use simple int-arrays (int[]) to store the data. This should provide fast and thread-safe read-only access, but limits the number of nodes and links in the network to (Integer.MAX_VALUE/2 =
 * 1.073.741.823) nodes and (Integer.MAX_VALUE/6 = 357.913.941) links. I hope that for the foreseeable future, these limits are high enough.
 * <p>
 * This class is thread-safe, allowing a single graph to be used by multiple threads.
 *
 * @author mrieser
 */
public class SpeedyGraph {

    /*
     * memory consumption:
     * - nodeData:
     *   - 1 int: next out-link index
     *   - 1 int: next in-link index
     *   = total 2 int = 8 bytes per node
     * - linkData
     *   - 1 int: next out-link index for from-node
     *   - 1 int: next in-link index for to-node
     *   - 1 int: from-node index
     *   - 1 int: to-node index
     *   - 1 int: link length * 100
     *   - 1 int: freespeed-traveltime * 100
     *   = total 6 int per link = 24 bytes per link
     * - links
     *   - 1 object-pointer per link
     *   = 1 int or 1 long per link (depending on 32 or 64bit JVM) = 4 or 8 bytes per link
     *
     *   So, a network-graph with 1 Mio nodes and 2 Mio links should consume between 64 and 72 MB RAM only.
     */

    private final static int NODE_SIZE = 2;
    private final static int LINK_SIZE = 6;

    final int nodeCount;
    final int linkCount;
    private final int[] nodeData;
    private final int[] linkData;
    private final Link[] links;
    private final Node[] nodes;

    public SpeedyGraph(Network network) {
        this.nodeCount = Id.getNumberOfIds(Node.class);
        this.linkCount = Id.getNumberOfIds(Link.class);

        this.nodeData = new int[nodeCount * NODE_SIZE];
        this.linkData = new int[linkCount * LINK_SIZE];
        this.links = new Link[linkCount];
        this.nodes = new Node[nodeCount];

        Arrays.fill(this.nodeData, -1);
        Arrays.fill(this.linkData, -1);

        for (Node node : network.getNodes().values()) {
            this.nodes[node.getId().index()] = node;
        }
        for (Link link : network.getLinks().values()) {
            addLink(link);
        }
    }

    private void addLink(Link link) {
        int fromNodeIdx = link.getFromNode().getId().index();
        int toNodeIdx = link.getToNode().getId().index();
        int linkIdx = link.getId().index();

        int base = linkIdx * LINK_SIZE;
        this.linkData[base + 2] = fromNodeIdx;
        this.linkData[base + 3] = toNodeIdx;
        this.linkData[base + 4] = (int) Math.round(link.getLength() * 100.0);
        this.linkData[base + 5] = (int) Math.round(link.getLength() / link.getFreespeed() * 100.0);

        setOutLink(fromNodeIdx, linkIdx);
        setInLink(toNodeIdx, linkIdx);

        this.links[linkIdx] = link;
    }

    private void setOutLink(int fromNodeIdx, int linkIdx) {
        final int nodeI = fromNodeIdx * NODE_SIZE;
        int outLinkIdx = this.nodeData[nodeI];
        if (outLinkIdx < 0) {
            this.nodeData[nodeI] = linkIdx;
            return;
        }
        int lastLinkIdx;
        do {
            lastLinkIdx = outLinkIdx;
            outLinkIdx = this.linkData[lastLinkIdx * LINK_SIZE];
        } while (outLinkIdx >= 0);
        this.linkData[lastLinkIdx * LINK_SIZE] = linkIdx;
    }

    private void setInLink(int toNodeIdx, int linkIdx) {
        final int nodeI = toNodeIdx * NODE_SIZE + 1;
        int inLinkIdx = this.nodeData[nodeI];
        if (inLinkIdx < 0) {
            this.nodeData[nodeI] = linkIdx;
            return;
        }
        int lastLinkIdx;
        do {
            lastLinkIdx = inLinkIdx;
            inLinkIdx = this.linkData[lastLinkIdx * LINK_SIZE + 1];
        } while (inLinkIdx >= 0);
        this.linkData[lastLinkIdx * LINK_SIZE + 1] = linkIdx;
    }

    public LinkIterator getOutLinkIterator() {
        return new OutLinkIterator(this);
    }

    public LinkIterator getInLinkIterator() {
        return new InLinkIterator(this);
    }

    Link getLink(int index) {
        return this.links[index];
    }

    Node getNode(int index) {
        return this.nodes[index];
    }

    public interface LinkIterator {

        void reset(int nodeIdx);

        boolean next();

        int getLinkIndex();

        int getToNodeIndex();

        int getFromNodeIndex();

        double getLength();

        double getFreespeedTravelTime();
    }

    private static abstract class AbstractLinkIterator implements LinkIterator {

        final SpeedyGraph graph;
        int nodeIdx = -1;
        int linkIdx = -1;

        AbstractLinkIterator(SpeedyGraph graph) {
            this.graph = graph;
        }

        @Override
        final public void reset(int nodeIdx) {
            this.nodeIdx = nodeIdx;
            this.linkIdx = -1;
        }

        @Override
        abstract public boolean next();

        @Override
        final public int getLinkIndex() {
            return this.linkIdx;
        }

        @Override
        final public int getFromNodeIndex() {
            return this.graph.linkData[this.linkIdx * LINK_SIZE + 2];
        }

        @Override
        final public int getToNodeIndex() {
            return this.graph.linkData[this.linkIdx * LINK_SIZE + 3];
        }

        @Override
        final public double getLength() {
            return this.graph.linkData[this.linkIdx * LINK_SIZE + 4] / 100.0;
        }

        @Override
        final public double getFreespeedTravelTime() {
            return this.graph.linkData[this.linkIdx * LINK_SIZE + 5] / 100.0;
        }
    }

    private static class OutLinkIterator extends AbstractLinkIterator {

        OutLinkIterator(SpeedyGraph graph) {
            super(graph);
        }

        @Override
        public boolean next() {
            if (this.nodeIdx < 0) {
                return false;
            }
            if (this.linkIdx < 0) {
                this.linkIdx = graph.nodeData[this.nodeIdx * NODE_SIZE];
            } else {
                this.linkIdx = graph.linkData[this.linkIdx * LINK_SIZE];
            }
            if (this.linkIdx < 0) {
                this.nodeIdx = -1;
                return false;
            }
            return true;
        }
    }

    private static class InLinkIterator extends AbstractLinkIterator {

        InLinkIterator(SpeedyGraph graph) {
            super(graph);
        }

        @Override
        public boolean next() {
            if (this.nodeIdx < 0) {
                return false;
            }
            if (this.linkIdx < 0) {
                this.linkIdx = graph.nodeData[this.nodeIdx * NODE_SIZE + 1];
            } else {
                this.linkIdx = graph.linkData[this.linkIdx * LINK_SIZE + 1];
            }
            if (this.linkIdx < 0) {
                this.nodeIdx = -1;
                return false;
            }
            return true;
        }
    }

}
