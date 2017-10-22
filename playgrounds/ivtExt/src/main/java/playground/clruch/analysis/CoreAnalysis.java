package playground.clruch.analysis;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import ch.ethz.idsc.queuey.math.AnalysisUtils;
import ch.ethz.idsc.queuey.util.GlobalAssert;
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
public class CoreAnalysis {
    private final StorageSupplier storageSupplier;
    private final int size;
    private Tensor summary = Tensors.empty();
    public Tensor waitBinCounter = Tensors.empty(); //TODO access, TODO can this be deleted?
    public Tensor totalWaitTimeQuantile = Tensors.empty(); // TODO access
    public Tensor totalWaitTimeMean = Tensors.empty(); // TODO access
    public double maximumWaitTime; // TODO access
    public int numRequests; // TODO access
    private AnalyzeAll analyzeAll;

    public CoreAnalysis(StorageSupplier storageSupplierIn, AnalyzeAll analyzeAll) {
        storageSupplier = storageSupplierIn;
        size = storageSupplier.size();
        this.analyzeAll = analyzeAll;
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
                waitTimeQuantile = TensorUtils.quantiles(submission);
                waitTimeMean = TensorUtils.means(submission);
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
            GlobalAssert.that(!waitTimeQuantile.isScalar());
            GlobalAssert.that(!numStatus.isScalar());
            Tensor row = Join.of( //
                    Tensors.of(time, requestsSize), // 0,1
                    waitTimeQuantile, // 2,3,4 (.1, .5, .95)
                    Tensors.of(waitTimeMean), // 5
                    numStatus, // 6,7,8,9
                    Tensors.of(occupancyRatio)); // 10

            table.append(row);

            if (s.now % 10000 == 0)
                System.out.println(s.now);

        }

        // AnalyzeAll.saveFile(table, "basicDemo");

        Tensor uniqueSubmissions = Tensor.of(requestWaitTimes.values().stream().map(RealScalar::of));
        numRequests = uniqueSubmissions.length();
        maximumWaitTime = AnalysisUtils.maximum(uniqueSubmissions).number().doubleValue();
        analyzeAll.setwaitBinSize(AnalysisUtils.adaptBinSize(uniqueSubmissions, analyzeAll.getwaitbinSize(), RealScalar.of(5.0)));
        waitBinCounter = AnalysisUtils.binCount(uniqueSubmissions, analyzeAll.getwaitbinSize());

        System.out.println("Found requests: " + numRequests);

        totalWaitTimeQuantile = TensorUtils.quantiles(uniqueSubmissions);
        System.out.println("Q = " + totalWaitTimeQuantile);
        totalWaitTimeMean = TensorUtils.means(uniqueSubmissions);
        System.out.println("mean = " + totalWaitTimeMean);

        summary = table;

    }
    
    public Tensor getSummary(){
        return summary.copy();
    }
}
