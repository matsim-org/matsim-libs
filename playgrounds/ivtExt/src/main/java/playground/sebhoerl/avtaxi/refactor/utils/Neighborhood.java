package playground.sebhoerl.avtaxi.refactor.utils;

import org.matsim.api.core.v01.network.Link;

import java.util.Set;

public class Neighborhood {
    final Set<Link> links;

    public Neighborhood(Set<Link> links) {
        this.links = links;
    }

    public boolean covers(Link link) {
        return links.contains(link);
    }

    public Set<Link> getLinks() {
        return links;
    }
}
