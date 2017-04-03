package playground.clruch.demo;

import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.io.CsvFormat;
import ch.ethz.idsc.tensor.io.MathematicaFormat;
import playground.clruch.net.SimulationObject;
import playground.clruch.net.StorageSupplier;
import playground.clruch.net.VehicleContainer;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

class DistanceAnalysis {
    StorageSupplier storageSupplier;
    int size;

    DistanceAnalysis(StorageSupplier storageSupplierIn) {
        storageSupplier = storageSupplierIn;
        size = storageSupplier.size();
    }

    public void analzye() throws Exception {

        SimulationObject init = storageSupplier.getSimulationObject(1);
        final int numVehicles = init.vehicles.size();
        System.out.println("found vehicles: " + numVehicles);

        List<VehicleStatistic> list = new ArrayList<>();
        IntStream.range(0, numVehicles).forEach(i -> list.add(new VehicleStatistic(size - 1)));

        for (int index = 0; index < size - 1; ++index) {
            SimulationObject s = storageSupplier.getSimulationObject(1 + index);
            for (VehicleContainer vc : s.vehicles)
                list.get(vc.vehicleIndex).register(index, vc);

            if (s.now % 1000 == 0)
                System.out.println(s.now);

        }

        list.forEach(VehicleStatistic::consolidate);

        {
            Tensor table = list.stream().map(vs -> vs.distanceTotal).reduce(Tensor::add).get();
            Files.write(Paths.get("output/data/distanceTotal.csv"), (Iterable<String>) CsvFormat.of(table)::iterator);
            Files.write(Paths.get("output/data/distanceTotal.mathematica"), (Iterable<String>) MathematicaFormat.of(table)::iterator);
        }
        {
            Tensor table = list.stream().map(vs -> vs.distanceWithCustomer).reduce(Tensor::add).get();
            Files.write(Paths.get("output/data/distanceWithCustomer.csv"), (Iterable<String>) CsvFormat.of(table)::iterator);
            Files.write(Paths.get("output/data/distanceWithCustomer.mathematica"), (Iterable<String>) MathematicaFormat.of(table)::iterator);
        }
    }
}
