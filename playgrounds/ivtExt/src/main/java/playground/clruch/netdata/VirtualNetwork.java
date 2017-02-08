package playground.clruch.netdata;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;

import java.io.File;
import java.util.Map;

/**
 * Created by Claudio on 2/8/2017.
 */
public class VirtualNetwork {
    private Map<Id<Link>,VirtualNode> linkMap;


    // TODO
    public static VirtualNetwork loadFromXML(Network network, File file){
        // takes NetworkPartitioning.xml file and generates VirtualNetwork
        return null;
    }

    // TODO
    public VirtualNode getVirtualNode(Id<Link> linkId){
        // returns the VirtualNode where linkId belongs to
        return null;
    }

    // TODO
    public Id<Link> getTargetLink(VirtualNode virtualNode){
        // returns a network target link for some virtual node
        return null;
    }

}
