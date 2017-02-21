package playground.clruch.netdata;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;

/**
 * Created by Claudio on 2/8/2017.
 */
public class VirtualNetwork {

    private final List<VirtualNode> virtualNodes = new ArrayList<>();
    private final List<VirtualLink> virtualLinks = new ArrayList<>();
    private final Map<Link, VirtualNode> linkVNodeMap = new HashMap<>();

    /* package */ VirtualNetwork() {
    }

    // get the collection of virtual nodes
    public List<VirtualNode> getVirtualNodes() {
        return Collections.unmodifiableList(virtualNodes);
    }

    // get the collection of virtual links
    public List<VirtualLink> getVirtualLinks() {
        return Collections.unmodifiableList(virtualLinks);
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

    /* package */ VirtualNode addVirtualNode(String idIn, Set<Link> linksIn) {
        VirtualNode virtualNode = new VirtualNode(virtualNodes.size(), idIn, linksIn);
        virtualNodes.add(virtualNode);
        for (Link link : virtualNode.getLinks())
            linkVNodeMap.put(link, virtualNode);
        return virtualNode;
    }

    /* package */ void addVirtualLink(String idIn, VirtualNode fromIn, VirtualNode toIn) {
        VirtualLink virtualLink = new VirtualLink(virtualLinks.size(), idIn, fromIn, toIn);
        virtualLinks.add(virtualLink);
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
