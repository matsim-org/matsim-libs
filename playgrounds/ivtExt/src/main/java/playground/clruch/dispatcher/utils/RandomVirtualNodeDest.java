package playground.clruch.dispatcher.utils;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.matsim.api.core.v01.network.Link;

import playground.clruch.netdata.VirtualNode;

public class RandomVirtualNodeDest extends AbstractVirtualNodeDest {
    @Override
    public Set<Link> selectLinkSet(VirtualNode virtualNode, int size) {
        // TODO implementation is not efficient!!!
        List<Link> links = virtualNode.getLinks().stream().collect(Collectors.toList());
        Collections.shuffle(links);
        return links.stream().limit(size).collect(Collectors.toSet());
    }

}
