// code by clruch
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
import java.util.function.Function;

import ch.ethz.idsc.queuey.util.GlobalAssert;

/** Created by Claudio on 2/8/2017. */
public class VirtualNetwork<T> implements Serializable {

    /**
     * 
     */
    private final long virtualNetworkID; // to ensure that other objects dependant on virtualNetwork are derived of that particular network
    private final List<VirtualNode<T>> virtualNodes = new ArrayList<>();
    private final List<VirtualLink<T>> virtualLinks = new ArrayList<>();
    // the map is for checking that all links in the network are assigned to one vNode
    protected transient Map<T, VirtualNode<T>> linkVNodeMap = new HashMap<>();
    // is stored but only used to create references LINK
    private final Map<String, VirtualNode<T>> linkVNodeMapRAWVERYPRIVATE = new HashMap<>();
    private final Map<Point, VirtualLink<T>> virtualLinkPairs = new HashMap<>();

    /* package */ VirtualNetwork() {
        virtualNetworkID = System.currentTimeMillis();
    }

    // get the collection of virtual nodes
    public Collection<VirtualNode<T>> getVirtualNodes() {
        return Collections.unmodifiableList(virtualNodes);
    }

    // get the collection of virtual links
    public Collection<VirtualLink<T>> getVirtualLinks() {
        return Collections.unmodifiableList(virtualLinks);
    }

    /** @return number of virtual Nodes */
    public int getvNodesCount() {
        return virtualNodes.size();
    }

    /** @return number of virtual Links */
    public int getvLinksCount() {
        return virtualLinks.size();
    }

    /** Gets the virtual node belonging to a link of the network. The lookup is fast.
     *
     * @param link
     *            of the network
     * @return virtual node belonging to link */
    public final VirtualNode<T> getVirtualNode(T link) {
        GlobalAssert.that(link != null);
        if (!linkVNodeMap.containsKey(link)) {
            System.out.println("T: " + (String) link);
            System.out.println("virtualNode not found ");
        }
        return linkVNodeMap.get(link);
    }

    /** @param index
     * @return the virtualLink belonging to a certain index. */
    public final VirtualLink<T> getVirtualLink(int index) {
        return virtualLinks.get(index);
    }

    public final VirtualNode<T> getVirtualNode(int index) {
        return virtualNodes.get(index);
    }

    /* package */ VirtualNode<T> addVirtualNode(VirtualNode<T> virtualNode) {
        GlobalAssert.that(virtualNodes.size() == virtualNode.getIndex()); // <- NEVER remove this check
        virtualNodes.add(virtualNode);
        for (T link : virtualNode.getLinks())
            linkVNodeMap.put(link, virtualNode);
        return virtualNode;
    }

    /* package */ void addVirtualLink(String idIn, VirtualNode fromIn, VirtualNode toIn, double distance) {
        GlobalAssert.that(Objects.nonNull(fromIn));
        GlobalAssert.that(Objects.nonNull(toIn));
        VirtualLink virtualLink = new VirtualLink(virtualLinks.size(), idIn, fromIn, toIn, distance);
        virtualLinks.add(virtualLink);
        virtualLinkPairs.put(nodePair_key(fromIn, toIn), virtualLink);
    }

    private static final Point nodePair_key(VirtualNode fromIn, VirtualNode toIn) {
        // it does not make sense to query links with source == dest:
        GlobalAssert.that(fromIn.getIndex() != toIn.getIndex());
        return new Point(fromIn.getIndex(), toIn.getIndex());
    }

    /** @param fromIn
     * @param toIn
     * @return VirtualLink object between fromIn and toIn, or null if such a VirtualLink is not
     *         defined */
    public VirtualLink<T> getVirtualLink(VirtualNode<T> fromIn, VirtualNode<T> toIn) {
        return virtualLinkPairs.get(nodePair_key(fromIn, toIn));
    }

    /** @param fromIn
     * @param toIn
     * @return true if VirtualLink object between fromIn and toIn is defined */
    public boolean containsVirtualLink(VirtualNode<T> fromIn, VirtualNode<T> toIn) {
        return virtualLinkPairs.containsKey(nodePair_key(fromIn, toIn));
    }

    /** @return return virtualNode related HashMaps */
    public <U> Map<VirtualNode<T>, List<U>> createVNodeTypeMap() {
        Map<VirtualNode<T>, List<U>> returnMap = new HashMap<>();
        for (VirtualNode<T> virtualNode : this.getVirtualNodes())
            returnMap.put(virtualNode, new ArrayList<>());
        return returnMap;
    }

    /** @param col {@link Collection} of objects associated to a {@link Link}
     * @param function bijection linking a object from @param col to a {@link Link}
     * @return {@link java.util.Map} where all objects in @param col are sorted at {@link VirtualNode} that the link supplied by @param function belongs to */
    public <U> Map<VirtualNode<T>, List<U>> binToVirtualNode(Collection<U> col, Function<U, T> function) {
        Map<VirtualNode<T>, List<U>> returnMap = createVNodeTypeMap();
        col.stream()//
                .forEach(c -> returnMap.get(getVirtualNode(function.apply(c))).add(c));
        return returnMap;
    }

    private void fillLinkVNodeMap(Map<String, T> map) {

        linkVNodeMap = new HashMap<>();

        for (String linkIDString : linkVNodeMapRAWVERYPRIVATE.keySet()) {
            // Id<Link> linkID = Id.createLinkId(linkIDString);
            // GlobalAssert.that(network.getLinks().get(linkID) != null);
            T link = map.get(linkIDString);
            VirtualNode<T> vNode = linkVNodeMapRAWVERYPRIVATE.get(linkIDString);
            linkVNodeMap.put(link, vNode);
        }
        checkConsistency();
    }

    /** Completes the missing references after the virtualNetwork was read from a serialized bitmap.**
     * 
     * @param network */
    public void fillSerializationInfo(Map<String, T> map) {
        fillLinkVNodeMap(map);

        // Map<String, T> map = new HashMap<>();
        // network.getLinks().entrySet().forEach(e -> map.put(e.getKey().toString(), e.getValue()));
        // virtualNodes.stream().forEach(v -> v.setLinksAfterSerialization2(map));

        virtualNodes.stream().forEach(v -> v.setLinksAfterSerialization2(map));
    }

    protected void fillVNodeMapRAWVERYPRIVATE(Map<T,String> map) {
        GlobalAssert.that(!linkVNodeMap.isEmpty());

        for (T link : linkVNodeMap.keySet()) {
            // linkVNodeMapRAWVERYPRIVATE.put( link.getId().toString(), linkVNodeMap.get(link));
            linkVNodeMapRAWVERYPRIVATE.put(map.get(link), linkVNodeMap.get(link));
        }

        GlobalAssert.that(linkVNodeMap.size() == linkVNodeMapRAWVERYPRIVATE.size());
        GlobalAssert.that(!linkVNodeMapRAWVERYPRIVATE.isEmpty());
    }

    public void checkConsistency() {
        GlobalAssert.that(!linkVNodeMapRAWVERYPRIVATE.isEmpty());
        GlobalAssert.that(linkVNodeMap != null);
    }

    public long getvNetworkID() {
        return virtualNetworkID;
    }

}

/// * package */ VirtualNode<T> addVirtualNode(String idIn, Set<T> linksIn, int neighCount, Tensor coord) {
// Map<String,T> map = new HashMap<>();
// linksIn.stream().forEach(l-> map.put(l.getId().toString(), l));
// VirtualNode<T> virtualNode = new VirtualNode(virtualNodes.size(), idIn, map, neighCount, coord);
// return addVirtualNode(virtualNode);
// }

//// TODO don't delete this function but move outside into class e.g. VirtualNetworkHelper
// public void printForTesting(Network network) {
// if (!linkVNodeMap.keySet().containsAll(network.getLinks().values()))
// throw new RuntimeException("not all Links are assigned a VirtualNode");
//
// for (VirtualNode<T> virtualNode : getVirtualNodes()) {
// System.out.println("vNode " + virtualNode.getId() + " contains " + virtualNode.getLinks().size() + " links:");
// for (T link : virtualNode.getLinks())
// System.out.println(" " + link.getId());
// }
// // check the virtualLinkList
// for (VirtualLink virtualLink : getVirtualLinks()) {
// System.out.println("vLink " + virtualLink.getId() + " " + //
// virtualLink.getFrom().getId() + " to " + //
// virtualLink.getTo().getId());
// }
// System.out.println("total: #vNodes=" + getVirtualNodes().size() + ", #vLinks=" + getVirtualLinks().size());
// }
