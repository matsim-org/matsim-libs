/**
 * 
 */
package playground.clruch.netdata;

import java.util.Collection;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;

import ch.ethz.idsc.queuey.core.networks.CenterVirtualNetworkCreator;
import ch.ethz.idsc.queuey.core.networks.VirtualNetwork;
import ch.ethz.idsc.tensor.Tensor;
import playground.clruch.dispatcher.utils.PlaneLocation;

/** @author Claudio Ruch creates {@link VirtualNetwork} with a center node and a surrounding node. The center node
 *         is located at the mean location of all {@link Network} {@link Link} and has a radius specified by the user, it is
 *         shifted by centerShift, i.e. centerActual = centerComputed + centerShift */
@Deprecated // TODO remove this, the MatsimCenterVirtualNetworkCreator works based on a supplied shapefile of the center.
public class MatsimCenterVirtualNetworkCreatorOld {

    public VirtualNetwork<Link> creatVirtualNetwork(Network network, double centerRadius, Tensor centerShift) {
        @SuppressWarnings("unchecked")
        Collection<Link> elements = (Collection<Link>) network.getLinks().values();

        CenterVirtualNetworkCreator<Link> cvn = new CenterVirtualNetworkCreator<>(centerRadius, centerShift, //
                elements, PlaneLocation::of, NetworkCreatorUtils::linkToID);
        return cvn.getVirtualNetwork();

    }

}
