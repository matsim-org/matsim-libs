/**
 * 
 */
package playground.clruch.dispatcher.utils;

import org.matsim.api.core.v01.network.Link;

import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.Tensors;
import ch.ethz.matsim.av.passenger.AVRequest;
import playground.clruch.dispatcher.core.RoboTaxi;

/** @author Claudio Ruch */
public enum PlaneLocation {
    ;

    public static Tensor of(AVRequest avRequest) {
        return tensorOf(avRequest.getFromLink());
    }

    public static Tensor of(RoboTaxi robotaxi) {
        return tensorOf(robotaxi.getDivertableLocation());
    }

    public static Tensor of(Link link) {
        return tensorOf(link);
    }

    private static Tensor tensorOf(Link link) {
        double dx = link.getCoord().getX();
        double dy = link.getCoord().getY();
        return Tensors.vectorDouble(dx, dy);

    }
}
