package playground.clruch.demo;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import org.matsim.api.core.v01.network.Network;

import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.Tensors;
import ch.ethz.idsc.tensor.alg.Transpose;
import ch.ethz.idsc.tensor.io.CsvFormat;
import ch.ethz.idsc.tensor.io.ObjectFormat;
import ch.ethz.idsc.tensor.io.Pretty;
import ch.ethz.idsc.tensor.red.Max;
import playground.clruch.data.ReferenceFrame;
import playground.clruch.net.LinkStatistic;
import playground.clruch.net.MatsimStaticDatabase;
import playground.clruch.net.RequestContainer;
import playground.clruch.net.SimulationObject;
import playground.clruch.net.StorageSupplier;
import playground.clruch.net.VehicleContainer;
import playground.clruch.utils.NetworkLoader;

/**
 * THIS FILE IS A CONCISE DEMO OF FUNCTIONALITY
 * 
 * DO NOT MODIFY THIS FILE (unless you are the primary author),
 * BUT DO NOT RELY ON THIS FILE NOT BEING CHANGED
 * 
 * IF YOU WANT TO MAKE A SIMILAR CLASS OR REPLY ON THIS IMPLEMENTATION
 * THEN DUPLICATE THIS FILE AND MAKE THE CHANGES IN THE NEW FILE
 */
class CongestionAnalysis {
    final StorageSupplier storageSupplier;
    final int size;

    CongestionAnalysis(StorageSupplier storageSupplier) {
        this.storageSupplier = storageSupplier;
        size = storageSupplier.size();
    }

    public void analzye() throws Exception {
        final int numLinks = MatsimStaticDatabase.INSTANCE.getOsmLinksSize();
        System.out.println("found links: " + numLinks);

        List<LinkStatistic> list = new ArrayList<>();
        IntStream.range(0, numLinks).forEach(i -> list.add(new LinkStatistic(size)));

        for (int tics = 0; tics < size; ++tics) {
            SimulationObject s = storageSupplier.getSimulationObject(tics);
            for (VehicleContainer vc : s.vehicles)
                list.get(vc.linkIndex).register(tics, vc);
            for (RequestContainer rc : s.requests)
                list.get(rc.fromLinkIndex).register(tics, s.now, rc);

            if (s.now % 10000 == 0)
                System.out.println(s.now);
        }

        // compute max
        Tensor vector1 = Tensor.of(list.stream().map(ls -> ls.maxWaitTime.flatten(0).reduce(Max::of).get()));
        Tensor vector2 = Tensor.of(list.stream().map(ls -> ls.vehicleCount.flatten(0).reduce(Max::of).get()));
        Tensor vector3 = Tensor.of(list.stream().map(ls -> ls.requestCount.flatten(0).reduce(Max::of).get()));

        Tensor matrix = Transpose.of(Tensors.of(vector1, vector2, vector3));
        System.out.println(Pretty.of(matrix));
        
        Files.write(Paths.get("output/linkstats.obj"), ObjectFormat.of(matrix));

        Files.write(Paths.get("output/linkstats.csv"), (Iterable<String>) CsvFormat.of(matrix)::iterator);
    }

    public static void main(String[] args) {
        Network network = NetworkLoader.loadNetwork(new File(args[0]));

        // load coordinate system
        MatsimStaticDatabase.initializeSingletonInstance(network, ReferenceFrame.SIOUXFALLS);

        CongestionAnalysis ca = new CongestionAnalysis(StorageSupplier.getDefault());
        try {
            ca.analzye();
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }
}
