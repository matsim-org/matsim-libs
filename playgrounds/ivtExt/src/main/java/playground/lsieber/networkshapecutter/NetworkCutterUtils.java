package playground.lsieber.networkshapecutter;

import java.util.Objects;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkUtils;

public enum NetworkCutterUtils {
    ;

    // TODO @Lukas directely filter original Network without return value -> save way....
    public static Network modeFilter(Network originalNetwork, LinkModes modes) {
        if (modes.allModesAllowed) {
            System.out.println("No modes filtered. Network was not modified");
            return originalNetwork;
        } else {
            // Filter out modes
            Network modesFilteredNetwork = NetworkUtils.createNetwork();
            for (Node node : originalNetwork.getNodes().values()) {
                modesFilteredNetwork.addNode(modesFilteredNetwork.getFactory().createNode(node.getId(), node.getCoord()));
            }
            for (Link link : originalNetwork.getLinks().values()) {
                Node filteredFromNode = modesFilteredNetwork.getNodes().get(link.getFromNode().getId());
                Node filteredToNode = modesFilteredNetwork.getNodes().get(link.getToNode().getId());
                if (Objects.nonNull(filteredFromNode) && Objects.nonNull(filteredToNode)) {
                    boolean allowedMode = modes.getModesSet().stream().anyMatch(link.getAllowedModes()::contains);
                    if (allowedMode) {
                        Link newLink = modesFilteredNetwork.getFactory().createLink(link.getId(), filteredFromNode, filteredToNode);

                        newLink.setAllowedModes(link.getAllowedModes());
                        newLink.setLength(link.getLength());
                        newLink.setCapacity(link.getCapacity());
                        newLink.setFreespeed(link.getFreespeed());
                        // newLink.setNumberOfLanes(link.getNumberOfLanes());

                        modesFilteredNetwork.addLink(newLink);
                    }
                }

            }

            // TODO sysout of all modes which are kept. Maybe with Stream?!
            String output = modes.getModesSet().toString();
            System.out.println("The following modes are kept in the network: " + output);

            return modesFilteredNetwork;
        }
    }

}
