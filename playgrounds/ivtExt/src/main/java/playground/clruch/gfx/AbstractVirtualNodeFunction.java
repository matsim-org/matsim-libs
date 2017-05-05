package playground.clruch.gfx;

import playground.clruch.net.MatsimStaticDatabase;
import playground.clruch.netdata.VirtualNetwork;

abstract class AbstractVirtualNodeFunction implements VirtualNodeFunction {
    final MatsimStaticDatabase db;
    final VirtualNetwork virtualNetwork;

    public AbstractVirtualNodeFunction(MatsimStaticDatabase db, VirtualNetwork virtualNetwork) {
        this.db = db;
        this.virtualNetwork = virtualNetwork;
    }
}
