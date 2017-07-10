// code by clruch
package playground.clruch.dispatcher.utils;

import java.util.List;

import org.matsim.api.core.v01.network.Link;

import org.matsim.api.core.v01.network.Node;
import playground.clruch.netdata.VirtualNode;

public abstract class AbstractVirtualNodeDest {
    public abstract List<Link> selectLinkSet(VirtualNode virtualNode, int size);

    /**
     *
     * @param vNode
     * @param endNode
     * @return a single node according to the strategy of selectLinkSet
     */
    public Node virtualToReal(VirtualNode vNode, boolean endNode) {
        if (endNode)
            return this.selectLinkSet(vNode, 1).get(0).getToNode();
        return this.selectLinkSet(vNode, 1).get(0).getFromNode();
    }
    
    // maybe insert option to treat case of local=within node behaviour
}
