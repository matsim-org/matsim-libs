package playground.clruch.demo;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import ch.ethz.idsc.tensor.RealScalar;
import ch.ethz.idsc.tensor.Scalar;
import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.Tensors;
import ch.ethz.idsc.tensor.alg.Array;
import ch.ethz.idsc.tensor.alg.Join;
import ch.ethz.idsc.tensor.red.Mean;
import ch.ethz.idsc.tensor.red.Quantile;
import playground.clruch.export.AVStatus;
import playground.clruch.net.SimulationObject;
import playground.clruch.net.StorageSupplier;
import playground.clruch.net.VehicleContainer;

class CoreAnalysis {
    StorageSupplier storageSupplier;
    int size;
    String dataPath;

    CoreAnalysis(StorageSupplier storageSupplierIn, String datapath) {
        storageSupplier = storageSupplierIn;
        size = storageSupplier.size();
        dataPath = datapath;
    }

    public void analyze(String directory) throws Exception {

        Tensor table = Tensors.empty();

        for (int index = 0; index < size; ++index) {

            SimulationObject s = storageSupplier.getSimulationObject(index);

            final long now = s.now;
            Scalar time = RealScalar.of(s.now);

            // number of requests
            Scalar requestsSize = RealScalar.of(s.requests.size());

            // wait time Quantiles and mean
            Tensor waitTimeQuantile;
            Tensor waitTimeMean;
            {
                Tensor submission = Tensor.of(s.requests.stream().map(rc -> RealScalar.of(now - rc.submissionTime)));
                if (3 < submission.length()) {
                    waitTimeQuantile = Quantile.of(submission, Tensors.vectorDouble(.1, .5, .95));
                    waitTimeMean = Mean.of(submission);
                } else {
                    waitTimeQuantile = Array.zeros(3);
                    waitTimeMean = Mean.of(Array.zeros(1));
                }
            }

            // status of AVs and occupancy ratio
            Tensor numStatus = Array.zeros(AVStatus.values().length);
            Scalar occupancyRatio = RealScalar.of(0.0);
            Integer totVeh = 0;
            {
                Map<AVStatus, List<VehicleContainer>> map = //
                        s.vehicles.stream().collect(Collectors.groupingBy(vc -> vc.avStatus));
                for (Entry<AVStatus, List<VehicleContainer>> entry : map.entrySet()) {
                    numStatus.set(RealScalar.of(entry.getValue().size()), entry.getKey().ordinal());
                    totVeh += entry.getValue().size();
                }
                if (map.containsKey(AVStatus.DRIVEWITHCUSTOMER)) {
                    occupancyRatio = RealScalar.of(map.get(AVStatus.DRIVEWITHCUSTOMER).size() / (double) totVeh);
                }
            }

            // Distance ratio
            Tensor row = Join.of( //
                    Tensors.of(time, requestsSize), //
                    waitTimeQuantile, //
                    waitTimeMean, //
                    numStatus, //
                    occupancyRatio);

            table.append(row);

            if (s.now % 1000 == 0)
                System.out.println(s.now);

        }

        // AnalyzeAll.saveFile(table, "basicDemo");

    }
}
