/**
 * 
 */
package playground.lsieber.networkshapecutter;

import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;

/** @author Claudio Ruch */
public final class RemoveShortLinks {

    public static RemoveShortLinks of(Network network) {
        return new RemoveShortLinks(network);
    }

    private final Network network;

    private RemoveShortLinks(Network network) {
        this.network = network;
    }

    public Network fromLength(double minLength) {

        Set<Id<Link>> toRemove = new HashSet();

        for (Entry<Id<Link>, ? extends Link> entry : network.getLinks().entrySet()) {
            if (entry.getValue().getLength() < minLength) {
                toRemove.add(entry.getKey());
            }
        }
        toRemove.stream().forEach(id -> network.removeLink(id));
        
        return network;

    }

}
