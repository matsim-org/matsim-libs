package playground.clruch.demo;

import java.nio.file.Files;
import java.nio.file.Paths;
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
import ch.ethz.idsc.tensor.io.CsvFormat;
import ch.ethz.idsc.tensor.io.MathematicaFormat;
import ch.ethz.idsc.tensor.red.Quantile;
import playground.clruch.dispatcher.core.AVStatus;
import playground.clruch.net.SimulationObject;
import playground.clruch.net.StorageSupplier;
import playground.clruch.net.VehicleContainer;

/**
 * THIS FILE IS A CONCISE DEMO OF FUNCTIONALITY
 * 
 * DO NOT MODIFY THIS FILE (unless you are the primary author),
 * BUT DO NOT RELY ON THIS FILE NOT BEING CHANGED
 * 
 * IF YOU WANT TO MAKE A SIMILAR CLASS OR REPLY ON THIS IMPLEMENTATION
 * THEN DUPLICATE THIS FILE AND MAKE THE CHANGES IN THE NEW FILE
 */
class PostProcessingDemo {

    /**
     * run this demo with working directory set to the directory that also contains the scenario config
     * for instance:
     * 
     * /media/datahaki/data/ethz/2017_03_13_Sioux_LP_improved
     */
    // TODO can this be removed?
    public static void main(String[] args) throws Exception {
        StorageSupplier storageSupplier = StorageSupplier.getDefault();

        int size = storageSupplier.size();
        System.out.println("found files: " + size);

        Tensor table = Tensors.empty();

        for (int index = 0; index < size; ++index) {

            SimulationObject s = storageSupplier.getSimulationObject(index);

            final long now = s.now;
            Scalar time = RealScalar.of(s.now);

            Scalar requestsSize = RealScalar.of(s.requests.size());

            Tensor waitTimeQuantile;
            {
                Tensor submission = Tensor.of(s.requests.stream().map(rc -> RealScalar.of(now - rc.submissionTime)));
                if (3 < submission.length())
                    waitTimeQuantile = Quantile.of(submission, Tensors.vectorDouble(.1, .5, .9));
                else
                    waitTimeQuantile = Array.zeros(3);
            }

            Tensor numStatus = Array.zeros(AVStatus.values().length);
            {
                Map<AVStatus, List<VehicleContainer>> map = //
                        s.vehicles.stream().collect(Collectors.groupingBy(vc -> vc.avStatus));
                for (Entry<AVStatus, List<VehicleContainer>> entry : map.entrySet()) {
                    numStatus.set(RealScalar.of(entry.getValue().size()), entry.getKey().ordinal());
                }
            }

            Tensor row = Join.of( //
                    Tensors.of(time, requestsSize), //
                    waitTimeQuantile, //
                    numStatus);

            table.append(row);

            if (s.now % 1000 == 0)
                System.out.println(s.now);

        }

        Files.write(Paths.get("basicdemo.csv"), (Iterable<String>) CsvFormat.of(table)::iterator);
        Files.write(Paths.get("basicdemo.mathematica"), (Iterable<String>) MathematicaFormat.of(table)::iterator);

    }

}
