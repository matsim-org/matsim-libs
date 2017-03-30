package playground.clruch.demo;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.CH1903LV03PlustoWGS84;

import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.io.CsvFormat;
import ch.ethz.idsc.tensor.io.MathematicaFormat;
import playground.clruch.gfx.MatsimStaticDatabase;
import playground.clruch.net.SimulationObject;
import playground.clruch.net.StorageSupplier;
import playground.clruch.net.VehicleContainer;
import playground.sebhoerl.avtaxi.framework.AVConfigGroup;

class DistanceAnalysis {

    /**
     * run this demo with working directory set to the directory that also contains the scenario config
     * for instance:
     * <p>
     * /media/datahaki/data/ethz/2017_03_13_Sioux_LP_improved
     */
    public static void main(String[] args) throws Exception {
        // load system network
        Network network = loadNetwork(args);

        // TODO later remove hard-coded
        CoordinateTransformation ct;
        ct = new CH1903LV03PlustoWGS84(); // <- switzerland
        // ct = new SiouxFallstoWGS84(); // <- sioux falls
        MatsimStaticDatabase.initializeSingletonInstance(network, ct);

        // load simulation data
        StorageSupplier storageSupplier = StorageSupplier.getDefault();

        final int size = storageSupplier.size();
        System.out.println("found files: " + size);

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
            Files.write(Paths.get("distanceTotal.csv"), (Iterable<String>) CsvFormat.of(table)::iterator);
            Files.write(Paths.get("distanceTotal.mathematica"), (Iterable<String>) MathematicaFormat.of(table)::iterator);
        }
        {
            Tensor table = list.stream().map(vs -> vs.distanceWithCustomer).reduce(Tensor::add).get();
            Files.write(Paths.get("distanceWithCustomer.csv"), (Iterable<String>) CsvFormat.of(table)::iterator);
            Files.write(Paths.get("distanceWithCustomer.mathematica"), (Iterable<String>) MathematicaFormat.of(table)::iterator);
        }
    }

    private static Network loadNetwork(String[] args) {
        // TODO can this be made more nice?
        File configFile = new File(args[0]);
        final File dir = configFile.getParentFile();

        DvrpConfigGroup dvrpConfigGroup = new DvrpConfigGroup();
        dvrpConfigGroup.setTravelTimeEstimationAlpha(0.05);

        Config config = ConfigUtils.loadConfig(configFile.toString(), new AVConfigGroup(), dvrpConfigGroup);
        Scenario scenario = ScenarioUtils.loadScenario(config);

        return scenario.getNetwork();
    }
}
