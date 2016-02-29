package playground.dziemke.accessibility.ptmatrix.TransitLeastCostPathRouting;

import java.util.HashMap;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;

/**
 * Copied from a class in the Matsim core code named CustomDataManager to have access to the methods.
 * Original class description: *
 *
 * "A helper class to store custom data for TransitTravelDisutility which can be used
 * to e.g. keep track of paid fares during the routing process.
 * The stored data will be invalidated for each new routing request."
 *
 * "@author mrieser / senozon"
 *
 * @author gthunig
 */
public class CustomDataManager {

    private final HashMap<Node, Object> data = new HashMap<>();
    private Node fromNode = null;
    private Node toNode = null;

    private Object tmpToNodeData = null;

    public void setToNodeCustomData(final Object data) {
        this.tmpToNodeData = data;
    }

    /**
     * @param Node fromNode
     * @return the stored data for the given node, or <code>null</code> if there is no data stored yet.
     */
    public Object getFromNodeCustomData() {
        return this.data.get(this.fromNode);
    }

    /*package*/ void initForLink(final Link link) {
        this.fromNode = link.getFromNode();
        this.toNode = link.getToNode();
        this.tmpToNodeData = null;
    }

    /*package*/ void storeTmpData() {
        if (this.tmpToNodeData != null) {
            this.data.put(this.toNode, this.tmpToNodeData);
        }
    }

    /*package*/ void reset() {
        this.data.clear();
        this.fromNode = null;
        this.toNode = null;
        this.tmpToNodeData = null;
    }

}
