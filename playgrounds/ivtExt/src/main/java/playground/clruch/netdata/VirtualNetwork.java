package playground.clruch.netdata;

import java.awt.Point;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;

import playground.clruch.utils.GlobalAssert;

/**
 * Created by Claudio on 2/8/2017.
 */
public class VirtualNetwork implements Serializable {

    /**
     * 
     */
    private final long virtualNetworkID; // to ensure that other objects dependant on virtualNetwork are derived of that particular network
    private final List<VirtualNode> virtualNodes = new ArrayList<>();
    private final List<VirtualLink> virtualLinks = new ArrayList<>();
    // the map is for checking that all links in the network are assigned to one vNode
    protected transient Map<Link, VirtualNode> linkVNodeMap = new HashMap<>();
    // is stored but only used to create references LINK
    private final Map<String, VirtualNode> linkVNodeMapRAWVERYPRIVATE = new HashMap<>();
    private final Map<Point, VirtualLink> virtualLinkPairs = new HashMap<>();
    

    /* package */ VirtualNetwork() {
        virtualNetworkID = System.currentTimeMillis();
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
     * @return number of virtual Nodes
     */
    public int getvNodesCount() {
        return virtualNodes.size();
    }

    /**
     * @return number of virtual Links
     */
    public int getvLinksCount() {
        return virtualLinks.size();
    }

    /**
     * Gets the virtual node belonging to a link of the network. The lookup is fast.
     *
     * @param link
     *            of the network
     * @return virtual node belonging to link
     */
    public final VirtualNode getVirtualNode(Link link) {
        GlobalAssert.that(link != null);
        if (!linkVNodeMap.containsKey(link)) {
            System.out.println("link: " + link.getId().toString());
            System.out.println("virtualNode not found ");
        }
        return linkVNodeMap.get(link);
    }

    /**
     * @param index
     * @return the virtualLink belonging to a certain index.
     */
    public final VirtualLink getVirtualLink(int index) {
        return virtualLinks.get(index);
    }

    public final VirtualNode getVirtualNode(int index) {
        return virtualNodes.get(index);
    }

    /* package */ VirtualNode addVirtualNode(String idIn, Set<Link> linksIn, int neighCount, Coord coord) {
        VirtualNode virtualNode = new VirtualNode(virtualNodes.size(), idIn, linksIn, neighCount, coord);
        return addVirtualNode(virtualNode);
    }

    /* package */ VirtualNode addVirtualNode(VirtualNode virtualNode) {
        GlobalAssert.that(virtualNodes.size() == virtualNode.index); // <- NEVER remove this check
        virtualNodes.add(virtualNode);
        for (Link link : virtualNode.getLinks())
            linkVNodeMap.put(link, virtualNode);
        return virtualNode;
    }

    /* package */ void addVirtualLink(String idIn, VirtualNode fromIn, VirtualNode toIn, double travelTime) {
        GlobalAssert.that(Objects.nonNull(fromIn));
        GlobalAssert.that(Objects.nonNull(toIn));
        VirtualLink virtualLink = new VirtualLink(virtualLinks.size(), idIn, fromIn, toIn, travelTime);
        virtualLinks.add(virtualLink);
        virtualLinkPairs.put(nodePair_key(fromIn, toIn), virtualLink);
    }

    private static final Point nodePair_key(VirtualNode fromIn, VirtualNode toIn) {
        // it does not make sense to query links with source == dest:
        GlobalAssert.that(fromIn.index != toIn.index);
        return new Point(fromIn.index, toIn.index);
    }

    /**
     * @param fromIn
     * @param toIn
     * @return VirtualLink object between fromIn and toIn, or null if such a VirtualLink is not
     *         defined
     */
    public VirtualLink getVirtualLink(VirtualNode fromIn, VirtualNode toIn) {
        return virtualLinkPairs.get(nodePair_key(fromIn, toIn));
    }

    /**
     * @param fromIn
     * @param toIn
     * @return true if VirtualLink object between fromIn and toIn is defined
     */
    public boolean containsVirtualLink(VirtualNode fromIn, VirtualNode toIn) {
        return virtualLinkPairs.containsKey(nodePair_key(fromIn, toIn));
    }

    /**
     * Completes the missing references after the virtualNetwork was read from a serialized bitmap.
     * 
     * @param network
     */
    public void fillSerializationInfo(Network network) {
        fillLinkVNodeMap(network);
        virtualNodes.stream().forEach(v -> v.setLinksAfterSerialization(network));
    }

    private void fillLinkVNodeMap(Network network) {

        linkVNodeMap = new HashMap<>();

        for (String linkIDString : linkVNodeMapRAWVERYPRIVATE.keySet()) {
            Id<Link> linkID = Id.createLinkId(linkIDString);
            GlobalAssert.that(network.getLinks().get(linkID) != null);
            Link link = network.getLinks().get(linkID);
            VirtualNode vNode = linkVNodeMapRAWVERYPRIVATE.get(linkIDString);
            linkVNodeMap.put(link, vNode);
        }
        checkConsistency();
    }

    protected void fillVNodeMapRAWVERYPRIVATE() {
        GlobalAssert.that(!linkVNodeMap.isEmpty());
        for (Link link : linkVNodeMap.keySet()) {
            linkVNodeMapRAWVERYPRIVATE.put(link.getId().toString(), linkVNodeMap.get(link));
        }

        GlobalAssert.that(linkVNodeMap.size() == linkVNodeMapRAWVERYPRIVATE.size());
        GlobalAssert.that(!linkVNodeMapRAWVERYPRIVATE.isEmpty());
    }

    public void checkConsistency() {
        GlobalAssert.that(!linkVNodeMapRAWVERYPRIVATE.isEmpty());
        GlobalAssert.that(linkVNodeMap != null);
    }
    
    public long getvNetworkID(){
        return virtualNetworkID;
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
