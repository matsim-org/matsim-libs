package playground.clruch.netdata;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;

/**
 * Created by Claudio on 2/8/2017.
 */
public class VirtualNetwork {
    public final Network network;
    private final Map<String, VirtualNode> virtualNodeList = new HashMap<>(); // has unique references
    private final Map<Link, VirtualNode> linkIdVNodeMap = new HashMap<>();
    private final Map<String, VirtualLink> virtualLinkList = new HashMap<>();

    public VirtualNetwork(Network network) {
        this.network = network;
    }

    // get the collection of virtual nodes
    public Collection<VirtualNode> getVirtualNodes() {
        return virtualNodeList.values();
    }

    // get the collection of virtual links
    public Collection<VirtualLink> getVirtualLinks() {
        return virtualLinkList.values();
    }

    /**
     * Gets the virtual node belonging to a link
     *
     * @param link
     *            of the network
     * @return virtual node belonging to link
     */
    public VirtualNode getVirtualNode(Link link) {
        return linkIdVNodeMap.get(link);
    }

    /* package */ void virtualNodeList_put(String virtualNodeId, VirtualNode virtualNode) {
        virtualNodeList.put(virtualNodeId, virtualNode);
    }

    /* package */ void linkIdVNodeMap_put(Link link, VirtualNode virtualNode) {
        linkIdVNodeMap.put(link, virtualNode);
    }

    /* package */ VirtualNode virtualNodeList_get(String virtualLinkfrom) {
        return virtualNodeList.get(virtualLinkfrom);
    }

    /* package */ void virtualLinkList_put(String virtualLinkId, VirtualLink virtualLink) {
        virtualLinkList.put(virtualLinkId, virtualLink);
    }

    // TODO don't delete this function but move outside into class e.g. VirtualNetworkHelper
    public void printForTesting() {
        // check the virtualNodeList
        for (String vNodeId : virtualNodeList.keySet()) {
            System.out.println("Node " + vNodeId + " belongs to virtual node" + virtualNodeList.get(vNodeId).getId());
            System.out.println("the following links belong to it: ");
            for (Link link : virtualNodeList.get(vNodeId).getLinks()) {
                System.out.println(link.getId().toString());
            }

        }

        // check the virtualLinkList
        for (String vLinkId : virtualLinkList.keySet()) {
            System.out.println("virtual link " + vLinkId + " has the id " + (virtualLinkList.get(vLinkId)).getId() + " and goes from");
            System.out.println("virtual node " + virtualLinkList.get(vLinkId).getFrom().getId() + " to " + virtualLinkList.get(vLinkId).getTo().getId());
        }

        // check the linkIdVNodeMap
        for (Link link : linkIdVNodeMap.keySet()) {
            System.out.println("Link " + link.getId() + " belongs to virtual node " + linkIdVNodeMap.get(link).getId());
        }

    }

}
