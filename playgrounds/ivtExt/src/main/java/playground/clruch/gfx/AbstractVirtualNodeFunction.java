// code by jph
package playground.clruch.gfx;

import org.matsim.api.core.v01.network.Link;

import ch.ethz.idsc.queuey.core.networks.VirtualNetwork;
import playground.clruch.net.MatsimStaticDatabase;

abstract class AbstractVirtualNodeFunction implements VirtualNodeFunction {
    final MatsimStaticDatabase db;
    final VirtualNetwork<Link> virtualNetwork;

    public AbstractVirtualNodeFunction(MatsimStaticDatabase db, VirtualNetwork<Link> virtualNetwork) {
        this.db = db;
        this.virtualNetwork = virtualNetwork;
    }
}
