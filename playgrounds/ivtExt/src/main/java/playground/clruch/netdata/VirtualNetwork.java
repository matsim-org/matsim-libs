package playground.clruch.netdata;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;

/**
 * Created by Claudio on 2/8/2017.
 */
public class VirtualNetwork {

    private final Set<VirtualNode> virtualNodeSet = new LinkedHashSet<>();
    private final Set<VirtualLink> virtualLinkSet = new LinkedHashSet<>();
    private final Map<Link, VirtualNode> linkVNodeMap = new HashMap<>();

    /* package */ VirtualNetwork() {
    }

    // get the collection of virtual nodes
    public Collection<VirtualNode> getVirtualNodes() {
        return Collections.unmodifiableCollection(virtualNodeSet);
    }

    // get the collection of virtual links
    public Collection<VirtualLink> getVirtualLinks() {
        return Collections.unmodifiableCollection(virtualLinkSet);
    }

    /**
     * Gets the virtual node belonging to a link of the network
     *
     * @param link
     *            of the network
     * @return virtual node belonging to link
     */
    public final VirtualNode getVirtualNode(Link link) {
        return linkVNodeMap.get(link);
    }

    /* package */ void addVirtualNode(VirtualNode virtualNode) {
        virtualNodeSet.add(virtualNode);
        for (Link link : virtualNode.getLinks())
            linkVNodeMap.put(link, virtualNode);
    }

    /* package */ void addVirtualLink(VirtualLink virtualLink) {
        virtualLinkSet.add(virtualLink);
    }

    // TODO don't delete this function but move outside into class e.g. VirtualNetworkHelper
    public void printForTesting(Network network) {
        if (!linkVNodeMap.keySet().containsAll(network.getLinks().values()))
            throw new RuntimeException("not all Links are assigned a VirtualNode");

        for (VirtualNode virtualNode : getVirtualNodes()) {
            System.out.println("vNode " + virtualNode.getId() + " contains " + virtualNode.getLinks().size() + " links");
        }
        // check the virtualNodeList
        // for (String vNodeId : virtualNodeList.keySet()) {
        // System.out.println("Node " + vNodeId + " belongs to virtual node" + virtualNodeList.get(vNodeId).getId());
        // System.out.println("the following links belong to it: ");
        // for (Link link : virtualNodeList.get(vNodeId).getLinks()) {
        // System.out.println(link.getId().toString());
        // }
        //
        // }
        //
        // // check the virtualLinkList
        for (VirtualLink virtualLink : getVirtualLinks()) {
            System.out.println("vLink " + virtualLink.getId() + " " + //
                    virtualLink.getFrom().getId() + " to " + // 
                    virtualLink.getTo().getId());
            // System.out.println("virtual link " + vLinkId + " has the id " + (virtualLinkList.get(vLinkId)).getId() + " and goes from");
            // System.out.println("virtual node " + virtualLinkList.get(vLinkId).getFrom().getId() + " to " + virtualLinkList.get(vLinkId).getTo().getId());
        }

        // check the linkIdVNodeMap
        // for (Link link : linkVNodeMap.keySet()) {
        // System.out.println("Link " + link.getId() + " belongs to virtual node " + linkVNodeMap.get(link).getId());
        // }

    }

}
