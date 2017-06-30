package playground.joel.analysis;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import ch.ethz.idsc.tensor.*;
import ch.ethz.idsc.tensor.alg.Join;
import ch.ethz.idsc.tensor.red.Total;
import ch.ethz.idsc.tensor.sca.InvertUnlessZero;
import playground.clruch.net.SimulationObject;
import playground.clruch.net.StorageSupplier;
import playground.clruch.net.VehicleContainer;
import playground.clruch.net.VehicleStatistic;

/**
 * Created by Joel on 05.04.2017.
 */
public class DistanceAnalysis {
    StorageSupplier storageSupplier;
    int size;
    public Tensor totalDistancesPerVehicle = Tensors.empty();
    public Tensor distancesWCPerVehicle = Tensors.empty();
    public Tensor tdBinCounter = Tensors.empty();
    public Tensor dwcBinCounter = Tensors.empty();
    public Tensor summary = Tensors.empty();
    public int numVehicles;

    DistanceAnalysis(StorageSupplier storageSupplierIn) {
        storageSupplier = storageSupplierIn;
        size = storageSupplier.size();
    }

    public void analzye() throws Exception {

        SimulationObject init = storageSupplier.getSimulationObject(1);
        numVehicles = init.vehicles.size();

        System.out.println("Found vehicles: " + numVehicles);

        List<VehicleStatistic> list = new ArrayList<>();
        IntStream.range(0, numVehicles).forEach(i -> list.add(new VehicleStatistic(size)));

        for (int index = 0; index < size - 1; ++index) {
            SimulationObject s = storageSupplier.getSimulationObject(1 + index);
            for (VehicleContainer vc : s.vehicles)
                list.get(vc.vehicleIndex).register(index, vc);

            if (s.now % 10000 == 0)
                System.out.println(s.now);

        }


        totalDistancesPerVehicle = Tensor.of(list.stream().map(vs ->
                        Total.of(vs.distanceTotal))).multiply(RealScalar.of(0.001));
        distancesWCPerVehicle = Tensor.of(list.stream().map(vs ->
                Total.of(vs.distanceWithCustomer))).multiply(RealScalar.of(0.001));

        tdBinCounter = AnalysisUtils.binCount(totalDistancesPerVehicle, AnalyzeAll.distanceBinSize);
        dwcBinCounter = AnalysisUtils.binCount(distancesWCPerVehicle, AnalyzeAll.distanceBinSize);

        list.forEach(VehicleStatistic::consolidate);

        Tensor table1 = list.stream().map(vs -> vs.distanceTotal).reduce(Tensor::add).get(); // summary 0 (11)
        Tensor table2 = list.stream().map(vs -> vs.distanceWithCustomer).reduce(Tensor::add).get(); // summary 1 (12)
        Tensor table3 = list.stream().map(vs -> vs.distancePickup).reduce(Tensor::add).get(); // summary 2 (13)
        Tensor table4 = list.stream().map(vs -> vs.distanceRebalance).reduce(Tensor::add).get(); // summary 3 (14)
        Tensor table5 = table1.map(InvertUnlessZero.function).pmul(table2); // summary 4 (15)
        summary = Join.of(1, table1, table2, table3, table4, table5);
        /*
        {
            AnalyzeAll.saveFile(table1, "distanceTotal");
            AnalyzeAll.saveFile(table2, "distanceWithCustomer");
            AnalyzeAll.saveFile(table3, "distancePickup");
            AnalyzeAll.saveFile(table4, "distanceRebalance");
            AnalyzeAll.saveFile(table5, "distanceRatio");
        }
        */
    }
}
