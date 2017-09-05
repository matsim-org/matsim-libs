package playground.maalbert.analysis;

import java.util.HashMap;
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
import playground.clruch.dispatcher.core.AVStatus;
import playground.clruch.net.SimulationObject;
import playground.clruch.net.StorageSupplier;
import playground.clruch.net.VehicleContainer;

/**
 * Created by Joel on 05.04.2017.
 */
class CoreAnalysis {
    StorageSupplier storageSupplier;
    int size;
    Tensor summary = Tensors.empty();
    Tensor totalWaitTimeQuantile = Tensors.empty();
    Tensor totalWaitTimeMean = Tensors.empty();

    CoreAnalysis(StorageSupplier storageSupplierIn) {
        storageSupplier = storageSupplierIn;
        size = storageSupplier.size();
    }


    static Tensor quantiles(Tensor submission) {
        if (submission.length()>0) {
            return Quantile.of(submission, Tensors.vectorDouble(.1, .5, .95));
        } else {
            return Array.zeros(3);
        }
    }

    static Tensor means(Tensor submission) {
        if ( submission.length()>0) {
            return Mean.of(submission);
        } else {
            return Mean.of(Array.zeros(1));
        }
    }

    public void analyze() throws Exception {

        Tensor table = Tensors.empty();
        Tensor allSubmissions = Tensors.empty();

        Map<Integer, Double> requestWaitTimes = new HashMap<>();

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
                waitTimeQuantile = quantiles(submission);
                waitTimeMean = means(submission);
                allSubmissions.append(submission);
            }

            s.requests.stream().forEach(rc -> requestWaitTimes.put(rc.requestIndex, now - rc.submissionTime));

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

            if (s.now % 10000 == 0)
                System.out.println(s.now);

        }

        AnalyzeMarc.saveFile(table, "basicDemo");

        Tensor uniqueSubmissions = Tensor.of(requestWaitTimes.values().stream().map(RealScalar::of));

        totalWaitTimeQuantile = quantiles(uniqueSubmissions);
        System.out.println("Q = " + totalWaitTimeQuantile);
        totalWaitTimeMean = means(uniqueSubmissions);
        System.out.println("mean = " + totalWaitTimeMean);

        summary = table;

    }
}
