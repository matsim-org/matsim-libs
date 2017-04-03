package playground.clruch.demo;

import ch.ethz.idsc.tensor.RealScalar;
import ch.ethz.idsc.tensor.Scalar;
import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.Tensors;
import ch.ethz.idsc.tensor.alg.Array;
import ch.ethz.idsc.tensor.alg.Join;
import ch.ethz.idsc.tensor.io.CsvFormat;
import ch.ethz.idsc.tensor.io.MathematicaFormat;
import ch.ethz.idsc.tensor.red.Mean;
import ch.ethz.idsc.tensor.red.Quantile;
import playground.clruch.export.AVStatus;
import playground.clruch.net.SimulationObject;
import playground.clruch.net.StorageSupplier;
import playground.clruch.net.VehicleContainer;
import playground.joel.data.DiagramCreator;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.stream.Collectors;

class CoreAnalysis {
    StorageSupplier storageSupplier;
    int size;
    NavigableMap<Long, Double> quantile50 = new TreeMap<>();
    NavigableMap<Long, Double> quantile95= new TreeMap<>();
    NavigableMap<Long, Double> mean= new TreeMap<>();
    NavigableMap<Long, Double> occupancy= new TreeMap<>();
    double maxWait = 5000;

    CoreAnalysis(StorageSupplier storageSupplierIn){
        storageSupplier = storageSupplierIn;
        size = storageSupplier.size();
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
                    waitTimeMean = Array.zeros(1);
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

            // create maps for diagram creation
            quantile50.put(s.now, 1000.0); // waitTimeQuantile.get(1));
            quantile95.put(s.now, 1500.0); // waitTimeQuantile.get(2));
            mean.put(s.now, 2000.0); // waitTimeMean);
            occupancy.put(s.now, 0.5); // occupancyRatio);

            if (s.now % 1000 == 0)
                System.out.println(s.now);

        }


        Files.write(Paths.get("output/data/basicdemo.csv"), (Iterable<String>) CsvFormat.of(table)::iterator);
        Files.write(Paths.get("output/data/basicdemo.mathematica"), (Iterable<String>) MathematicaFormat.of(table)::iterator);

        DiagramCreator diagram = new DiagramCreator();
        try{
            File dir = new File(directory);
            diagram.createDiagram(dir, "binnedWaitingTimes", "waiting times", quantile50, quantile95, mean, maxWait);
            diagram.createDiagram(dir, "binnedTimeRatios", "occupancy ratio", occupancy);
        }catch (Exception e){
            System.out.println("Error creating the diagrams");
        }

    }
}

