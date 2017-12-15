/**
 * 
 */
package playground.lsieber.networkshapecutter.networkcuttershape;

import java.util.Objects;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;

/** @author Claudio Ruch */
public class AddContainedLinks {

    private final Network originalNetwork;

    /* package */ static AddContainedLinks of(Network originalNetwork) {
        return new AddContainedLinks(originalNetwork);
    }

    private AddContainedLinks(Network originalNetwork) {
        this.originalNetwork = originalNetwork;

    }

    /* package */ final void to(Network modifiedNetwork) {
        for (Link link : originalNetwork.getLinks().values()) {
            Node filteredFromNode = modifiedNetwork.getNodes().get(link.getFromNode().getId());
            Node filteredToNode = modifiedNetwork.getNodes().get(link.getToNode().getId());

            if (!Objects.isNull(filteredFromNode) && !Objects.isNull(filteredToNode)) {
                Link newLink = copyOf(link, modifiedNetwork.getFactory(), filteredFromNode, filteredToNode);
                modifiedNetwork.addLink(newLink);
            }
        }
    }

    private Link copyOf(Link link, NetworkFactory networkFactory, //
            Node fromNode, Node toNode) {
        Link newLink = networkFactory.createLink(link.getId(), fromNode, toNode);

        newLink.setAllowedModes(link.getAllowedModes());
        newLink.setLength(link.getLength());
        newLink.setCapacity(link.getCapacity());
        newLink.setFreespeed(link.getFreespeed());
        return newLink;
    }
}
