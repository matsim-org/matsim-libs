package playground.clruch.demo;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import org.matsim.api.core.v01.network.Network;

import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.Tensors;
import ch.ethz.idsc.tensor.alg.Range;
import ch.ethz.idsc.tensor.alg.Transpose;
import ch.ethz.idsc.tensor.io.Pretty;
import playground.clruch.data.ReferenceFrame;
import playground.clruch.net.MatsimStaticDatabase;
import playground.clruch.net.SimulationObject;
import playground.clruch.net.StorageSupplier;
import playground.clruch.net.VehicleContainer;
import playground.clruch.net.VehicleStatistic;
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
class DistanceAnalysis {
    StorageSupplier storageSupplier;
    int size;
    String dataPath;

    DistanceAnalysis(StorageSupplier storageSupplierIn, String datapath) {
        storageSupplier = storageSupplierIn;
        size = storageSupplier.size();
        dataPath = datapath;
    }

    public void analzye() throws Exception {

        SimulationObject init = storageSupplier.getSimulationObject(0);
        final int numVehicles = init.vehicles.size();
        System.out.println("found vehicles: " + numVehicles);

        List<VehicleStatistic> list = new ArrayList<>();
        IntStream.range(0, numVehicles).forEach(i -> list.add(new VehicleStatistic(size)));

        for (int tics = 0; tics < size; ++tics) {
            SimulationObject s = storageSupplier.getSimulationObject(tics);
            for (VehicleContainer vc : s.vehicles)
                list.get(vc.vehicleIndex).register(tics, vc);

            if (s.now % 10000 == 0)
                System.out.println(s.now);

        }

        list.forEach(VehicleStatistic::consolidate);

        Tensor table1 = list.stream().map(vs -> vs.distanceWithCustomer).reduce(Tensor::add).get();
        Tensor table2 = list.stream().map(vs -> vs.distanceTotal).reduce(Tensor::add).get();

        // System.out.println(Dimensions.of(Range.of(table1.length())));
        // System.out.println(Dimensions.of(table1));
        // System.out.println(Dimensions.of(table2));

        Tensor matrix = Transpose.of(Tensors.of(Range.of(0, table1.length()), table1, table2));
        System.out.println(Pretty.of(matrix.extract(2000, 4000)));
    }

    public static void main(String[] args) {
        Network network = NetworkLoader.loadNetwork(new File(args[0]));

        // load coordinate system
        MatsimStaticDatabase.initializeSingletonInstance(network, ReferenceFrame.SIOUXFALLS);

        DistanceAnalysis da = new DistanceAnalysis(StorageSupplier.getDefault(), args[0]);
        try {
            da.analzye();
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }
}
