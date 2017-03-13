package playground.clruch.netdata;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;

import playground.clruch.utils.GlobalAssert;

/**
 * Created by Claudio on 2/8/2017.
 */
public class VirtualNetwork {

    private final List<VirtualNode> virtualNodes = new ArrayList<>();
    private final List<VirtualLink> virtualLinks = new ArrayList<>();
    // the map is for checking that all links in the network are assigned to one vNode
    private final Map<Link, VirtualNode> linkVNodeMap = new HashMap<>();

    /* package */ VirtualNetwork() {
    }

    // get the collection of virtual nodes
    public Collection<VirtualNode> getVirtualNodes() {
        return Collections.unmodifiableList(virtualNodes);
    }

    // get the collection of virtual links
    public Collection<VirtualLink> getVirtualLinks() {
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

    /**
     *
     * @param index
     * @return the virtualLink belonging to a certain index.
     */
    public final VirtualLink getVirtualLink(int index){
        return  this.getVirtualLinks().stream().filter(v->v.getIndex() == index).findAny().get();
    }



    /* package */ VirtualNode addVirtualNode(String idIn, Set<Link> linksIn) {
        VirtualNode virtualNode = new VirtualNode(virtualNodes.size(), idIn, linksIn);
        virtualNodes.add(virtualNode);
        for (Link link : virtualNode.getLinks())
            linkVNodeMap.put(link, virtualNode);
        return virtualNode;
    }

    /* package */ void addVirtualLink(String idIn, VirtualNode fromIn, VirtualNode toIn) {
        GlobalAssert.that(Objects.nonNull(fromIn));
        GlobalAssert.that(Objects.nonNull(toIn));
        VirtualLink virtualLink = new VirtualLink(virtualLinks.size(), idIn, fromIn, toIn);
        virtualLinks.add(virtualLink);
    }

    // TODO don't delete this function but move outside into class e.g. VirtualNetworkHelper
    public void printForTesting(Network network) {
        if (!linkVNodeMap.keySet().containsAll(network.getLinks().values()))
            throw new RuntimeException("not all Links are assigned a VirtualNode");

        for (VirtualNode virtualNode : getVirtualNodes()) {
            System.out.println("vNode " + virtualNode.getId() + " contains " + virtualNode.getLinks().size() + " links:");
            for (Link link : virtualNode.getLinks())
                System.out.println(" " + link.getId());
        }
        // check the virtualLinkList
        for (VirtualLink virtualLink : getVirtualLinks()) {
            System.out.println("vLink " + virtualLink.getId() + " " + //
                    virtualLink.getFrom().getId() + " to " + //
                    virtualLink.getTo().getId());
        }
        System.out.println("total: #vNodes=" + getVirtualNodes().size() + ", #vLinks=" + getVirtualLinks().size());
    }

}
