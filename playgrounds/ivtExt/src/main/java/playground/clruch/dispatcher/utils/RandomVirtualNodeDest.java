package playground.clruch.dispatcher.utils;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.matsim.api.core.v01.network.Link;

import playground.clruch.netdata.VirtualNode;
import playground.clruch.utils.GlobalAssert;

public class RandomVirtualNodeDest extends AbstractVirtualNodeDest {
    Random random = new Random();
    @Override
    public List<Link> selectLinkSet(VirtualNode virtualNode, int size) {
        List<Link> links = virtualNode.getLinks().stream().collect(Collectors.toList());
        List<Link> ret = new ArrayList<>();
        //IntStream.range(0,size).forEach(i->ret.add(links.get(random.nextInt(links.size()))));
        for (int index = 0; index<size;++index)
            ret.add(links.get(random.nextInt(links.size())));
        GlobalAssert.that(ret.size() == size);
        return ret;
    }

}
