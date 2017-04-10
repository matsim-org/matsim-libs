package playground.clruch.demo;

import static playground.clruch.utils.NetworkLoader.loadNetwork;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import org.matsim.api.core.v01.network.Network;

import ch.ethz.idsc.tensor.Tensor;
import playground.clruch.gfx.ReferenceFrame;
import playground.clruch.net.MatsimStaticDatabase;
import playground.clruch.net.SimulationObject;
import playground.clruch.net.StorageSupplier;
import playground.clruch.net.VehicleContainer;
import playground.clruch.net.VehicleStatistic;

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
        IntStream.range(0, numVehicles).forEach(i -> list.add(new VehicleStatistic(size - 1)));

        for (int index = 0; index < size - 1; ++index) {
            SimulationObject s = storageSupplier.getSimulationObject(1 + index);
            for (VehicleContainer vc : s.vehicles)
                list.get(vc.vehicleIndex).register(index, vc);

            if (s.now % 10000 == 0)
                System.out.println(s.now);

        }

        list.forEach(VehicleStatistic::consolidate);

        Tensor table1 = list.stream().map(vs -> vs.distanceTotal).reduce(Tensor::add).get();
        Tensor table2 = list.stream().map(vs -> vs.distanceWithCustomer).reduce(Tensor::add).get();
        System.out.println(table1);
        System.out.println("---");
        System.out.println(table2);
        // {
        // AnalyzeMarc.saveFile(table1, "distanceTotal");
        // AnalyzeMarc.saveFile(table2, "distanceWithCustomer");
        // AnalyzeMarc.saveFile(table3, "distanceRatio");
        // }
    }

    public static void main(String[] args) {
        Network network = loadNetwork(args);

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
