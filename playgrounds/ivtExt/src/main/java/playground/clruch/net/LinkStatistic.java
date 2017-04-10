package playground.clruch.net;

import ch.ethz.idsc.tensor.RealScalar;
import ch.ethz.idsc.tensor.Scalar;
import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.alg.Array;

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

    public void register(int tics, RequestContainer rc) {
        requestCount.set(s -> s.add(RealScalar.ONE), tics);
        Scalar v = RealScalar.of(tics).subtract(RealScalar.of(rc.submissionTime));
        maxWaitTime.set(s -> RealScalar.max((RealScalar) s, (RealScalar) v), tics);
    }

    public void consolidate() {
    }

}
