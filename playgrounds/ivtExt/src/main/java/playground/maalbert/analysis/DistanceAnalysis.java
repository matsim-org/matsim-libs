package playground.maalbert.analysis;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.Tensors;
import ch.ethz.idsc.tensor.alg.Join;
import ch.ethz.idsc.tensor.sca.InvertUnlessZero;
import playground.clruch.net.SimulationObject;
import playground.clruch.net.StorageSupplier;
import playground.clruch.net.VehicleContainer;
import playground.clruch.net.VehicleStatistic;

/**
 * Created by Joel on 05.04.2017.
 */
class DistanceAnalysis {
    StorageSupplier storageSupplier;
    int size;
    Tensor summary = Tensors.empty();

    DistanceAnalysis(StorageSupplier storageSupplierIn) {
        storageSupplier = storageSupplierIn;
        size = storageSupplier.size();
    }

    public void analzye() throws Exception {

        SimulationObject init = storageSupplier.getSimulationObject(1);
        final int numVehicles = init.vehicles.size();
        System.out.println("found vehicles: " + numVehicles);

        List<VehicleStatistic> list = new ArrayList<>();
        IntStream.range(0, numVehicles).forEach(i -> list.add(new VehicleStatistic(size)));

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
        Tensor table3 = table1.map(InvertUnlessZero.FUNCTION).pmul(table2);
        summary = Join.of(1, table1, table2, table3);
        {
            AnalyzeMarc.saveFile(table1, "distanceTotal");
            AnalyzeMarc.saveFile(table2, "distanceWithCustomer");
            AnalyzeMarc.saveFile(table3, "distanceRatio");
        }
    }
}
