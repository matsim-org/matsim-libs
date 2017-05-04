package playground.clruch.netdata;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.matsim.api.core.v01.network.Node;

/**
 * class helps to detect adjacency between VirtualNodes based on shared Nodes in the network
 */
class ButterfliesAndRainbows {
    private Map<Node, Set<VirtualNode>> map = new HashMap<>();

    public void add(Node node, VirtualNode virtualNode) {
        if (!map.containsKey(node))
            map.put(node, new HashSet<>());
        map.get(node).add(virtualNode);
    }

    public Collection<Point> allPairs() {
        Set<Point> collection = new HashSet<>();
        for (Entry<Node, Set<VirtualNode>> entry : map.entrySet()) {
            Set<VirtualNode> set = entry.getValue();
            if (1 < set.size()) {
                if (set.size() != 2)
                    System.out.println("Node shared by " + set.size() + " VirtualNodes");
                List<VirtualNode> some = new ArrayList<>(set);
                for (int i = 0; i < some.size() - 1; ++i)
                    for (int j = i + 1; j < some.size(); ++j) {
                        collection.add(new Point(some.get(i).index, some.get(j).index));
                        collection.add(new Point(some.get(j).index, some.get(i).index));
                    }
            }
        }
        return collection;
    }
}
