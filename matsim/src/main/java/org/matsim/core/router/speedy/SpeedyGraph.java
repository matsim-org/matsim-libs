package org.matsim.core.router.speedy;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.turnRestrictions.TurnRestrictionsContext;

import java.util.Optional;

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
 * <h3>Spatial node ordering</h3>
 * <p>When built via {@link SpeedyGraphBuilder}, nodes are internally renumbered using a
 * Z-order (Morton) curve so that spatially nearby nodes receive adjacent internal indices.
 * This dramatically improves CPU cache locality during Dijkstra / A* traversal because the
 * per-node arrays ({@code data[]}, {@code iterationIds[]}, etc.) in the routing algorithms
 * are accessed in a pattern that follows the graph's spatial structure.  The reordering is
 * applied in both the normal and turn-restriction build paths; colored (virtual) nodes
 * inherit the coordinate of their parent real node and are placed nearby in the ordering.
 * <p>
 * Use {@link #getNodeIndex(Node)} to translate a MATSim {@link Node} to its internal index.
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

    final static int NODE_SIZE = 2;
    final static int LINK_SIZE = 6;

    final int nodeCount;
    final int linkCount;
    private final int[] nodeData;
    private final int[] linkData;
    private final Link[] links;
    private final Node[] nodes;
    private final TurnRestrictionsContext turnRestrictions;

    // Maps external MATSim Id.index() → internal (spatially ordered) node index.
    // null means identity mapping (internal index == Id.index()).
    private final int[] nodeReorder;

	SpeedyGraph(int[] nodeData, int[] linkData, Node[] nodes, Link[] links,
				TurnRestrictionsContext turnRestrictions, int[] nodeReorder) {
		this.nodeData = nodeData;
		this.linkData = linkData;
		this.nodes = nodes;
		this.links = links;
		this.nodeCount = this.nodes.length;
		this.linkCount = this.links.length;
		this.turnRestrictions = turnRestrictions;
		this.nodeReorder = nodeReorder;
	}

    /** Returns the number of nodes in this graph. */
    public int getNodeCount() {
        return this.nodeCount;
    }

    /**
     * Returns the internal (spatially ordered) node index for the given MATSim node.
     * All routing algorithms must use this method instead of {@code node.getId().index()}.
     *
     * @param node a MATSim network node
     * @return the internal node index, or -1 if the node is not part of this graph
     */
    public int getNodeIndex(Node node) {
        if (nodeReorder != null) {
            int externalIdx = node.getId().index();
            if (externalIdx < 0 || externalIdx >= nodeReorder.length) {
                return -1;
            }
            return nodeReorder[externalIdx];
        }
        return node.getId().index();
    }

    /**
     * Returns the internal (spatially ordered) node index for a raw external index
     * (e.g. from a {@link org.matsim.core.network.turnRestrictions.TurnRestrictionsContext.ColoredNode}).
     *
     * @param externalIdx an external node index (as returned by {@code Id.index()} or {@code ColoredNode.index()})
     * @return the internal node index, or -1 if unmapped
     */
    public int getInternalIndex(int externalIdx) {
        if (nodeReorder != null) {
            if (externalIdx < 0 || externalIdx >= nodeReorder.length) {
                return -1;
            }
            return nodeReorder[externalIdx];
        }
        return externalIdx;
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

    Optional<TurnRestrictionsContext> getTurnRestrictions() {
			return Optional.ofNullable(this.turnRestrictions);
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

    // -----------------------------------------------------------------------
    // Default linked-list iterators (original implementation)
    // -----------------------------------------------------------------------

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
