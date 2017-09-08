/**
 * 
 */
package playground.clruch.netdata;

import java.util.function.Function;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;

import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.Tensors;
import ch.ethz.idsc.tensor.alg.VectorQ;
import ch.ethz.idsc.tensor.red.Norm;

/** @author Claudio Ruch */
public enum VirtualNetworkUtils {
    ;

    /* package */ static String linkToID(Link link) {
        return link.getId().toString();
    }

    public static Coord fromTensor(Tensor tensor) {
        VectorQ.ofLength(tensor, 2); // ensure that vector of length 2;
        return new Coord(tensor.Get(0).number().doubleValue(), //
                tensor.Get(1).number().doubleValue());

    }

    /* package */ static Tensor fromCoord(Coord coord) {
        return Tensors.vectorDouble(coord.getX(), coord.getY());
    }

    /* package */ static double distance(Tensor t1, Tensor t2) {
        return Norm._2.of(t1.subtract(t2)).number().doubleValue();
    }

    /* package */ String linkToStringID(Link link) {
        return link.getId().toString();
    }

    /* package */ String linkToStringID(String linkID, Network network) {
        Link link = network.getLinks().get(linkID);
        return link.getId().toString();
    }

}
