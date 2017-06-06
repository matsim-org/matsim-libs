// code by jph
package playground.clruch.net;

import java.util.function.Function;

import ch.ethz.idsc.tensor.RealScalar;
import ch.ethz.idsc.tensor.Scalar;
import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.alg.Array;
import ch.ethz.idsc.tensor.red.Max;

// TODO this is only used in CongestionAnalysis
public class LinkStatistic {
    public final Tensor vehicleCount;
    public final Tensor requestCount;
    public final Tensor maxWaitTime;

    public LinkStatistic(int tics_max) {
        vehicleCount = Array.zeros(tics_max);
        requestCount = Array.zeros(tics_max);
        maxWaitTime = Array.zeros(tics_max);
    }

    public void register(int tics, VehicleContainer vehicleContainer) {
        vehicleCount.set(s -> s.add(RealScalar.ONE), tics);
    }

    static Function<Tensor, Tensor> max(Scalar s) {
        return v -> Max.of(s, v);
    }

    public void register(int tics, long now, RequestContainer rc) {
        requestCount.set(s -> s.add(RealScalar.ONE), tics);
        Scalar v = RealScalar.of(now).subtract(RealScalar.of(rc.submissionTime));
        maxWaitTime.set(max(v), tics);
    }

}
