package playground.clruch.demo;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.matsim.api.core.v01.network.Link;

import ch.ethz.idsc.tensor.RealScalar;
import ch.ethz.idsc.tensor.Scalar;
import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.alg.Array;
import playground.clruch.export.AVStatus;
import playground.clruch.net.MatsimStaticDatabase;
import playground.clruch.net.VehicleContainer;

public class VehicleStatistic {

    final Tensor distanceTotal;
    final Tensor distanceWithCustomer;

    public VehicleStatistic(int tics) {
        distanceTotal = Array.zeros(tics);
        distanceWithCustomer = Array.zeros(tics);
    }

    private int lastIndex = -1;

    int offset = -1;
    List<VehicleContainer> list = new LinkedList<>();

    public void register(int index, VehicleContainer vehicleContainer) {
        if (vehicleContainer.linkIndex != lastIndex) {
            consolidate();
            list.clear();
            offset = index;
        }
        list.add(vehicleContainer);
    }

    public void consolidate() {
        if (!list.isEmpty()) {
            final int linkId = list.get(0).linkIndex;
            Map<AVStatus, List<VehicleContainer>> map = //
                    list.stream().collect(Collectors.groupingBy(vc -> vc.avStatus));
            Link currentLink = MatsimStaticDatabase.INSTANCE.getOsmLink(linkId).link;
            double distance = currentLink.getLength();

            AVStatus[] driven = new AVStatus[] { //
                    AVStatus.DRIVEWITHCUSTOMER, //
                    AVStatus.DRIVETOCUSTMER, //
                    AVStatus.REBALANCEDRIVE //
            };
            int part = 0;
            for (AVStatus avs : driven)
                if (map.containsKey(avs))
                    part += map.get(avs).size();

            Scalar contrib = RealScalar.of(distance / part);
            int count = 0;
            for (VehicleContainer vehicleContainer : list) {
                final int index = offset + count;
                switch (vehicleContainer.avStatus) {
                case DRIVEWITHCUSTOMER:
                    distanceWithCustomer.set(contrib, index);
                case DRIVETOCUSTMER:
                case REBALANCEDRIVE:
                    distanceTotal.set(contrib, index); // applies to all three
                    break;
                default:
                    break;
                }
                ++count;
            }
        }
    }

}
