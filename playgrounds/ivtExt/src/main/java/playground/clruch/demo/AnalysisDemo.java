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
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import playground.clruch.export.AVStatus;
import playground.clruch.gfx.MatsimStaticDatabase;
import playground.clruch.gfx.helper.SiouxFallstoWGS84;
import playground.clruch.net.SimulationObject;
import playground.clruch.net.StorageSupplier;
import playground.clruch.net.VehicleContainer;
import playground.sebhoerl.avtaxi.framework.AVConfigGroup;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

class AnalysisDemo {

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
        //ct = new CH1903LV03PlustoWGS84(); // <- switzerland
        ct = new SiouxFallstoWGS84(); // <- sioux falls
        MatsimStaticDatabase.initializeSingletonInstance(network, ct);


        // load simulation data
        StorageSupplier storageSupplier = StorageSupplier.getDefault();

        int size = storageSupplier.size();
        System.out.println("found files: " + size);

        Tensor table = Tensors.empty();


        // compute distance ratios
        // TODO make more elegant
        Tensor distAtTime = Tensors.empty();
        Tensor custdistAtTime = Tensors.empty();
        Scalar numVehicles = RealScalar.of(0.0);
        for (int i = 0; i < size; ++i) {
            distAtTime.append(RealScalar.of(0.0));
            custdistAtTime.append(RealScalar.of(0.0));
        }


        Scalar totDistance = RealScalar.of(0.0);
        {
            SimulationObject init = storageSupplier.getSimulationObject(1);
            numVehicles = RealScalar.of(init.vehicles.size());

            for (VehicleContainer vehicle : init.vehicles) {
                Integer vehicleID = vehicle.vehicleIndex;

                // record the vehicle location and status at all times
                Tensor vehicleLocAtTime = Tensors.empty();//.zeros(storageSupplier.getSimulationObject(0).vehicles.size());
                vehicleLocAtTime.append(RealScalar.of(0.0));

                Tensor vehicleCustatTime = Tensors.empty();//.zeros(storageSupplier.getSimulationObject(0).vehicles.size());
                vehicleCustatTime.append(RealScalar.of(0.0));

                for (int index = 1; index < size; ++index) {
                    SimulationObject s = storageSupplier.getSimulationObject(index);
                    VehicleContainer vehicleNow = s.vehicles.stream().filter(v -> v.vehicleIndex == vehicleID).findAny().get();
                    //vehicle location
                    vehicleLocAtTime.append(RealScalar.of(vehicleNow.getLinkId()));
                    if (vehicleNow.avStatus.equals(AVStatus.DRIVEWITHCUSTOMER)) {
                        vehicleCustatTime.append(RealScalar.of(1.0));
                    } else {
                        vehicleCustatTime.append(RealScalar.of(0.0));
                    }

                    if (s.now % 1000 == 0)
                        System.out.println(s.now);
                }


                // transform into distances
                Integer currentLinkID = (Integer) vehicleLocAtTime.Get(0).number();
                Tensor vehicleDistAtTime = Tensors.empty();  //Array.empty();//Tensors.empty();Array.zeros(storageSupplier.getSimulationObject(0).vehicles.size());
                int numStepSameLink = 1;
                for (int index = 0; index < size; ++index) {
                    Integer newLinkId = (Integer) vehicleLocAtTime.Get(index).number();
                    if (!(newLinkId).equals(currentLinkID) || index == size - 1) {

                        // compute Link length
                        Link currentLink = MatsimStaticDatabase.INSTANCE.getOsmLink(currentLinkID).link;
                        double distance = currentLink.getLength();

                        // compute length per timestep
                        double distperTimestep = distance / numStepSameLink;

                        // assign to past requests
                        for (int i = 0; i < numStepSameLink; ++i) {
                            vehicleDistAtTime.append(RealScalar.of(distperTimestep));
                        }

                        currentLinkID = (Integer) vehicleLocAtTime.Get(index).number();
                        numStepSameLink = 1;
                    } else {
                        numStepSameLink += 1;
                    }
                }

                // compute the distance ratio using AV status
                // TODO make more elegant
                Tensor custvehicleDistAtTime = Tensors.empty();
                Tensor distTot = Tensors.empty();
//                Tensor distanceRatioVeh = Tensors.empty();

                for (int i = 0; i < vehicleDistAtTime.length(); ++i) {
                    custvehicleDistAtTime.append(vehicleDistAtTime.Get(i).multiply(vehicleCustatTime.Get(i)));
                    totDistance = totDistance.add(vehicleDistAtTime.Get(i));
                }

                distAtTime = distAtTime.add(vehicleDistAtTime);
                custdistAtTime = custdistAtTime.add(custvehicleDistAtTime);
            }
        }




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
                    occupancyRatio,
                    distAtTime.Get(index),
                    custdistAtTime.Get(index));


            table.append(row);

            if (s.now % 1000 == 0)
                System.out.println(s.now);

        }



        Files.write(Paths.get("basicdemo.csv"), (Iterable<String>) CsvFormat.of(table)::iterator);
        Files.write(Paths.get("basicdemo.mathematica"), (Iterable<String>) MathematicaFormat.of(table)::iterator);

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

class TimeDistObj {
    public Scalar time;
    public double distance;
}


