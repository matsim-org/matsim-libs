package org.matsim.contrib.noise;

import com.google.inject.Inject;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.utils.collections.Tuple;

import static org.matsim.contrib.noise.RLS90VehicleType.car;
import static org.matsim.contrib.noise.RLS90VehicleType.hgv;

class RLS90NoiseComputation implements NoiseEmissionStrategy {

    private final static Logger log = Logger.getLogger(RLS90NoiseComputation.class);

    private final NoiseConfigGroup noiseParams;
    private final Network network;

    @Inject
    RLS90NoiseComputation(Scenario scenario) {
        noiseParams = ConfigUtils.addOrGetModule(scenario.getConfig(), NoiseConfigGroup.class);
        network = scenario.getNetwork();
    }

    @Override
    public void calculateEmission(NoiseLink noiseLink) {

        Id<Link> linkId = noiseLink.getId();
        Tuple<Double, Double> vCarVHdv = getV(linkId, noiseLink);
        double vCar = vCarVHdv.getFirst();
        double vHdv = vCarVHdv.getSecond();

        double noiseEmission = 0.;
        double noiseEmissionPlusOneCar = 0.;
        double noiseEmissionPlusOneHgv = 0.;

        int n_car= noiseLink.getAgentsEntering(car.getId());
        int n_hgv =  noiseLink.getAgentsEntering(RLS90VehicleType.hgv.getId());

        int n = n_car + n_hgv;

        double p = 0.;
        if(!(n == 0)) {
            p = n_hgv / ((double) n);
        }

        int nPlusOneCarOrHGV = n + 1;

        double pPlusOneHgv = (n_hgv + 1.) / ((double) nPlusOneCarOrHGV);
        double pPlusOneCar = n_hgv / ((double) nPlusOneCarOrHGV);

        // correction for a sample, multiplicate the scale factor
        n = (int) (n * (noiseParams.getScaleFactor()));

        // correction for intervals unequal to 3600 seconds (= one hour)
        n = (int) (n * (3600. / noiseParams.getTimeBinSizeNoiseComputation()));

        // correction for a sample, multiplicate the scale factor
        nPlusOneCarOrHGV = (int) (nPlusOneCarOrHGV * (noiseParams.getScaleFactor()));

        // correction for intervals unequal to 3600 seconds (= one hour)
        nPlusOneCarOrHGV = (int) (nPlusOneCarOrHGV * (3600. / noiseParams.getTimeBinSizeNoiseComputation()));

        if(!(n == 0)) {
            double mittelungspegel = calculateMittelungspegelLm(n, p);
            double Dv = calculateGeschwindigkeitskorrekturDv(vCar, vHdv, p);
            noiseEmission = mittelungspegel + Dv;
        }

        double mittelungspegelPlusOneCar = calculateMittelungspegelLm(nPlusOneCarOrHGV, pPlusOneCar);
        double DvPlusOneCar = calculateGeschwindigkeitskorrekturDv(vCar, vHdv, pPlusOneCar);
        noiseEmissionPlusOneCar = mittelungspegelPlusOneCar + DvPlusOneCar;

        double mittelungspegelPlusOneHgv = calculateMittelungspegelLm(nPlusOneCarOrHGV, pPlusOneHgv);
        double DvPlusOneHgv = calculateGeschwindigkeitskorrekturDv(vCar, vHdv, pPlusOneHgv);
        noiseEmissionPlusOneHgv = mittelungspegelPlusOneHgv + DvPlusOneHgv;

        if (noiseEmissionPlusOneCar < noiseEmission || noiseEmissionPlusOneHgv < noiseEmission) {
            log.warn("vCar: " + vCar + " - vHGV: " + vHdv + " - p: " + p + " - n_car: " + n_car + " - n_hgv: " + n_hgv + " - n: " + n + " - pPlusOneCar: " + pPlusOneCar + " - pPlusOneHgv: " + pPlusOneHgv + " - noise emission: " + noiseEmission + " - noise emission plus one car: " + noiseEmissionPlusOneCar + " - noise emission plus one hgv: " + noiseEmissionPlusOneHgv + ". This should not happen. Aborting...");
        }

        noiseLink.setEmission(noiseEmission);
        noiseLink.setEmissionPlusOneVehicle(car.getId(), noiseEmissionPlusOneCar);
        noiseLink.setEmissionPlusOneVehicle(RLS90VehicleType.hgv.getId(), noiseEmissionPlusOneHgv);
    }



    private double calculateMittelungspegelLm(int n, double p) {

        //	Der Beurteilungspegel L_r ist bei Straßenverkehrsgeraeuschen gleich dem Mittelungspegel L_m.
        //  L_r = L_m = 10 * lg(( 1 / T_r ) * (Integral)_T_r(10^(0,1*1(t))dt))
        //	L_m,e ist der Mittelungspegel im Abstand von 25m von der Achse der Schallausbreitung

        // 	M ... traffic volume
        // 	p ... share of hdv in %

        if (p > 1) {
            throw new RuntimeException("p has to be <= 1. For an HGV share of 1%, p should be 0.01. Aborting...");
        }

        double pInPercentagePoints = p * 100.;
        double mittelungspegel = 37.3 + 10* Math.log10(n * (1 + (0.082 * pInPercentagePoints)));

        return mittelungspegel;
    }

    private double calculateGeschwindigkeitskorrekturDv (double vCar , double vHdv , double p) {

        //  v ... speed in kilometers per hour
        // 	p ... share of hdv, in percentage points

        if (p > 1) {
            throw new RuntimeException("p has to be <= 1. For an HGV share of 1%, p should be 0.01. Aborting...");
        }

        double pInPercentagePoints = p * 100.;

        double lCar = calculateLCar(vCar);
        double lHdv = calculateLHdv(vHdv);

        double d = lHdv - lCar;
        double geschwindigkeitskorrekturDv = lCar - 37.3 + 10* Math.log10( (100.0 + (Math.pow(10.0, (0.1 * d)) - 1) * pInPercentagePoints ) / (100 + 8.23 * pInPercentagePoints));

        return geschwindigkeitskorrekturDv;
    }

    static double calculateLCar(double vCar) {

        double lCar = 27.7 + (10 * Math.log10(1.0 + Math.pow(0.02 * vCar, 3.0)));
        return lCar;
    }

    static double calculateLHdv(double vHdv) {
        double lHdv = 23.1 + (12.5 * Math.log10(vHdv));
        return lHdv;
    }

    private Tuple<Double, Double> getV(Id<Link> linkId, NoiseLink noiseLink) {
        Link link = network.getLinks().get(linkId);

        double vCar = (link.getFreespeed()) * 3.6;
        double vHdv = vCar;

        double freespeedCar = vCar;

        if (noiseParams.isUseActualSpeedLevel()) {

            // use the actual speed level if possible
            if (noiseLink != null) {

                // Car
                if (noiseLink.getTravelTime_sec(car.getId()) == 0.
                        || noiseLink.getAgentsLeaving(car.getId()) == 0) {
                    // use the maximum speed level

                } else {
                    double averageTravelTimeCar_sec =
                            noiseLink.getTravelTime_sec(car.getId()) / noiseLink.getAgentsLeaving(car.getId());
                    vCar = 3.6 * (link.getLength() / averageTravelTimeCar_sec );
                }

                // HGV
                if (noiseLink.getTravelTime_sec(hgv.getId()) == 0. || noiseLink.getAgentsLeaving(hgv.getId()) == 0) {
                    // use the actual car speed level
                    vHdv = vCar;

                } else {
                    double averageTravelTimeHGV_sec = noiseLink.getTravelTime_sec(hgv.getId()) / noiseLink.getAgentsLeaving(hgv.getId());
                    vHdv = 3.6 * (link.getLength() / averageTravelTimeHGV_sec );
                }
            }
        }

        if (vCar > freespeedCar) {
            throw new RuntimeException(vCar + " > " + freespeedCar + ". This should not be possible. Aborting...");
        }

        if (!noiseParams.isAllowForSpeedsOutsideTheValidRange()) {

            // shifting the speed into the allowed range defined by the RLS-90 computation approach

            if (vCar < 30.) {
                vCar = 30.;
            }

            if (vHdv < 30.) {
                vHdv = 30.;
            }

            if (vCar > 130.) {
                vCar = 130.;
            }

            if (vHdv > 80.) {
                vHdv = 80.;
            }
        }
        return new Tuple<>(vCar, vHdv);
    }

}
