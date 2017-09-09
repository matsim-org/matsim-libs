// code by clruch
package playground.clruch.dispatcher.utils.virtualnodedestselector;

import java.util.List;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;

import ch.ethz.idsc.queuey.core.networks.VirtualNode;

public abstract class AbstractVirtualNodeDest {
    public abstract List<Link> selectLinkSet(VirtualNode<Link> virtualNode, int size);

    /** @param vNode
     * @param endNode
     * @return a single node according to the strategy of selectLinkSet */
    public final Node virtualToReal(VirtualNode<Link> vNode, boolean endNode) {
        if (endNode)
            return selectLinkSet(vNode, 1).get(0).getToNode();
        return selectLinkSet(vNode, 1).get(0).getFromNode();
    }

    // maybe insert option to treat case of local=within node behaviour
}
