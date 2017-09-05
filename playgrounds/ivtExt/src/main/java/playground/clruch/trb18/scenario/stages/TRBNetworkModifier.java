package playground.clruch.trb18.scenario.stages;

import java.util.HashSet;
import java.util.Set;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;

public class TRBNetworkModifier {
    /**
     * Annotates all the links in the full network that are present in the filtered AV network with the allowed mode "av"
     */
    public void modify(Network network, Network filteredNetwork) {
        for (Link link : filteredNetwork.getLinks().values()) {
            Set<String> allowedModes = new HashSet<>(link.getAllowedModes());
            allowedModes.add("av");

            network.getLinks().get(link.getId()).setAllowedModes(allowedModes);
        }
    }
}
