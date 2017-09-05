// code by jph
package playground.clruch.net;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.matsim.api.core.v01.network.Link;

import ch.ethz.idsc.tensor.RealScalar;
import ch.ethz.idsc.tensor.Scalar;
import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.Tensors;
import ch.ethz.idsc.tensor.alg.Array;
import ch.ethz.idsc.tensor.red.Total;
import playground.clruch.dispatcher.core.AVStatus;

// TODO group files that have "statistics" and "analysis" in the name
public class VehicleStatistic {

    public final Tensor distanceTotal;
    public final Tensor distanceWithCustomer;
    public final Tensor distancePickup;
    public final Tensor distanceRebalance;

    private int lastIndex = -1;
    private int offset = -1;
    // this is used as a buffer and is periodically empty
    private final List<VehicleContainer> list = new LinkedList<>();

    public VehicleStatistic(int tics_max) {
        distanceTotal = Array.zeros(tics_max);
        distanceWithCustomer = Array.zeros(tics_max);
        distancePickup = Array.zeros(tics_max);
        distanceRebalance = Array.zeros(tics_max);
    }

    public void register(int tics, VehicleContainer vehicleContainer) {
        if (vehicleContainer.linkIndex != lastIndex) {
            consolidate();
            list.clear();
            offset = tics;
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
                    distanceTotal.set(contrib, index); // applies to all three
                    break;
                case DRIVETOCUSTMER:
                    distancePickup.set(contrib, index);
                    distanceTotal.set(contrib, index); // applies to all three
                    break;
                case REBALANCEDRIVE:
                    distanceRebalance.set(contrib, index);
                    distanceTotal.set(contrib, index); // applies to all three
                    break;
                default:
                    break;
                }
                ++count;
            }
        }
    }

    public Tensor totalDistances() {
        Tensor distances = Tensors.empty();
        distances.append(Total.of(distanceTotal));
        distances.append(Total.of(distanceWithCustomer));
        distances.append(Total.of(distancePickup));
        distances.append(Total.of(distanceRebalance));
        return distances;
    }

}
