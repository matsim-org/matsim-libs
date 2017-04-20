package playground.clruch.gfx;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import ch.ethz.idsc.tensor.RealScalar;
import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.alg.Array;
import playground.clruch.export.AVStatus;
import playground.clruch.net.MatsimStaticDatabase;
import playground.clruch.net.SimulationObject;
import playground.clruch.net.VehicleContainer;

class LinkStats {

    static AVStatus[] INTERP = new AVStatus[] { //
            AVStatus.DRIVEWITHCUSTOMER, AVStatus.DRIVETOCUSTMER, AVStatus.REBALANCEDRIVE };

    final MatsimStaticDatabase db;
    final Tensor count;

    final Set<Integer> linkIndex = new HashSet<>();

    public LinkStats(int width) {
        db = MatsimStaticDatabase.INSTANCE;
        count = Array.zeros(db.getOsmLinksSize(), width, 2);
    }

    public void feed(SimulationObject ref, int ofs) {
        Map<Integer, List<VehicleContainer>> map = ref.vehicles.stream() //
                .collect(Collectors.groupingBy(VehicleContainer::getLinkId));

        for (Entry<Integer, List<VehicleContainer>> entry : map.entrySet()) {
            final int index = entry.getKey();
            List<VehicleContainer> list = entry.getValue();
            final long total = list.stream().filter(vc -> !vc.avStatus.equals(AVStatus.STAY)).count();
            if (0 < total) {
                linkIndex.add(index);
                Map<AVStatus, List<VehicleContainer>> classify = //
                        list.stream().collect(Collectors.groupingBy(vc -> vc.avStatus));
                int[] counts = new int[3];
                for (AVStatus avStatus : INTERP)
                    counts[avStatus.ordinal()] = classify.containsKey(avStatus) ? classify.get(avStatus).size() : 0;
                final int customers = counts[0];
                final int carsEmpty = counts[1] + counts[2];
                count.set(RealScalar.of(customers), index, ofs, 0);
                count.set(RealScalar.of(carsEmpty), index, ofs, 1);
            }
        }
    }

}
