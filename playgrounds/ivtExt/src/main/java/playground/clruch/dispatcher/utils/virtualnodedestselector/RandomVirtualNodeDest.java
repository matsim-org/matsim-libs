package playground.clruch.dispatcher.utils.virtualnodedestselector;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.matsim.api.core.v01.network.Link;
import org.matsim.core.gbl.MatsimRandom;

import ch.ethz.idsc.queuey.core.networks.VirtualNode;
import ch.ethz.idsc.queuey.util.GlobalAssert;

/**
 * Created by Joel on 26.06.2017.
 */
public class RandomVirtualNodeDest extends AbstractVirtualNodeDest {


    @Override
    public List<Link> selectLinkSet(VirtualNode<Link> virtualNode, int size) {

        // if no vehicles to be sent to node, return empty list
        if (size < 1)
            return Collections.emptyList();

        List<Link> ret = new ArrayList<>();
        List<Link> links = virtualNode.getLinks().stream().collect(Collectors.toList());
        while (ret.size() != size) {
            int elemRand = MatsimRandom.getRandom().nextInt(links.size());
            Link randLink = links.stream().skip(elemRand).findFirst().get();
            ret.add(randLink);
        }

        GlobalAssert.that(ret.size() == size);
        return ret;
    }
}
