// code by jph
package playground.clruch.gfx;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import ch.ethz.idsc.tensor.RealScalar;
import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.alg.Array;
import playground.clruch.dispatcher.core.AVStatus;
import playground.clruch.net.MatsimStaticDatabase;
import playground.clruch.net.SimulationObject;
import playground.clruch.net.VehicleContainer;

// sioux falls has ~3k roads
// zurich scenario has ~200k roads
/* package */ class LinkStats {

    static AVStatus[] INTERP = new AVStatus[] { //
            AVStatus.DRIVEWITHCUSTOMER, AVStatus.DRIVETOCUSTMER, AVStatus.REBALANCEDRIVE };

    final MatsimStaticDatabase db;
    final int width;

    final Map<Integer, Tensor> linkTensor = new HashMap<>();

    public LinkStats(int width) {
        db = MatsimStaticDatabase.INSTANCE;
        this.width = width;
    }

    public void feed(SimulationObject ref, int ofs) {
        Map<Integer, List<VehicleContainer>> map = ref.vehicles.stream() //
                .collect(Collectors.groupingBy(VehicleContainer::getLinkId));

        for (Entry<Integer, List<VehicleContainer>> entry : map.entrySet()) {
            final int index = entry.getKey();
            List<VehicleContainer> list = entry.getValue();
            final long total = list.stream().filter(vc -> !vc.avStatus.equals(AVStatus.STAY)).count();
            if (0 < total) {
                final Tensor array;
                if (linkTensor.containsKey(index))
                    array = linkTensor.get(index);
                else {
                    array = Array.zeros(width, 2);
                    linkTensor.put(index, array);
                }
                Map<AVStatus, List<VehicleContainer>> classify = //
                        list.stream().collect(Collectors.groupingBy(vc -> vc.avStatus));
                int[] counts = new int[3];
                for (AVStatus avStatus : INTERP)
                    counts[avStatus.ordinal()] = classify.containsKey(avStatus) ? classify.get(avStatus).size() : 0;
                final int customers = counts[0];
                final int carsEmpty = counts[1] + counts[2];
                array.set(RealScalar.of(customers), ofs, 0);
                array.set(RealScalar.of(carsEmpty), ofs, 1);
            }
        }
    }

}
