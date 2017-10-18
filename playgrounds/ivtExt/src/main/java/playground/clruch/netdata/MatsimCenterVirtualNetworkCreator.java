/**
 * 
 */
package playground.clruch.netdata;

import java.util.Collection;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;

import ch.ethz.idsc.queuey.core.networks.CenterVirtualNetworkCreator;
import ch.ethz.idsc.queuey.core.networks.VirtualNetwork;
import ch.ethz.idsc.queuey.util.GlobalAssert;
import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.Tensors;
import playground.clruch.dispatcher.utils.PlaneLocation;

/** @author Claudio Ruch creates {@link VirtualNetwork} with a center node and a surrounding node. The center node
 *         is located at the mean location of all {@link Network} {@link Link} and has a radius specified by the user, it is
 *         shifted by centerShift, i.e. centerActual = centerComputed + centerShift */
public class MatsimCenterVirtualNetworkCreator {

    private final double radius;
    private final Coord centerShift;
    private final Network network;
    private final VirtualNetwork<Link> virtualNetwork;

    public MatsimCenterVirtualNetworkCreator(Coord centerShift, double radius, Network network) {
        this.radius = radius;
        this.centerShift = centerShift;
        this.network = network;
        virtualNetwork = creatVirtualNetwork(network);

    }

    private VirtualNetwork<Link> creatVirtualNetwork(Network network) {
        Collection<Link> elements = (Collection<Link>) network.getLinks().values();
        Tensor centerShift =  Tensors.vectorDouble(this.centerShift.getX(), this.centerShift.getY());
        CenterVirtualNetworkCreator<Link> cvn = new CenterVirtualNetworkCreator<>(radius, centerShift, elements, //
                PlaneLocation::of, NetworkCreatorUtils::linkToID);
        return cvn.getVirtualNetwork();
    }

    public VirtualNetwork<Link> getVirtualNetwork() {
        GlobalAssert.that(virtualNetwork != null);
        return virtualNetwork;
    }

}
