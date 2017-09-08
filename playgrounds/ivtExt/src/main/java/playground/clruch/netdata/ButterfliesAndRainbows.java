// code by jph
package playground.clruch.netdata;

import java.awt.Point;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.matsim.api.core.v01.network.Node;

/**
 * class helps to detect adjacency between VirtualNodes based on shared Nodes in the network
 * 
 * two VirtualNodes form a VirtualLink if there is a matsim::Node that is contained by both VirtualNodes
 * 
 * a matsim::Node is contained in a VirtualNode in case there is a Link in the linkSet of the VirtualNode
 * that has given node as fromLink or as toLink.
 */
class ButterfliesAndRainbows {
    private Map<Node, Set<VirtualNode>> map = new HashMap<>();

    public void add(Node node, VirtualNode virtualNode) {
        if (!map.containsKey(node))
            map.put(node, new HashSet<>());
        map.get(node).add(virtualNode);
    }

    public List<Point> allPairs() {
        // some algorithms assume that an edge is adjacent in the list to the opposite edge
        List<Point> list = new LinkedList<>();
        for (Entry<Node, Set<VirtualNode>> entry : map.entrySet()) {
            Set<VirtualNode> set = entry.getValue();
            if (1 < set.size()) {
                if (set.size() != 2)
                    System.out.println("Node shared by " + set.size() + " VirtualNodes");
                List<VirtualNode> some = new ArrayList<>(set);
                for (int i = 0; i < some.size() - 1; ++i)
                    for (int j = i + 1; j < some.size(); ++j) {
                        list.add(new Point(some.get(i).getIndex(), some.get(j).getIndex()));
                        list.add(new Point(some.get(j).getIndex(), some.get(i).getIndex()));
                    }
            }
        }
        return list;
    }
}
