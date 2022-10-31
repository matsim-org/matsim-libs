package org.matsim.contrib.noise;

import com.google.common.collect.Range;
import com.google.inject.Inject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.ConfigUtils;

import static org.matsim.contrib.noise.RLS90VehicleType.car;
import static org.matsim.contrib.noise.RLS90VehicleType.hgv;

class RLS90NoiseEmission implements NoiseEmission {

    private final static Logger log = LogManager.getLogger(RLS90NoiseEmission.class);

    private final NoiseConfigGroup noiseParams;
    private final Network network;

    @Inject
    RLS90NoiseEmission(Scenario scenario) {
        noiseParams = ConfigUtils.addOrGetModule(scenario.getConfig(), NoiseConfigGroup.class);
        network = scenario.getNetwork();
    }

    @Override
    public void calculateEmission(NoiseLink noiseLink) {

        double vCar = getV(noiseLink, car);
        double vHdv = getV(noiseLink, hgv);

        double noiseEmission = 0.;
        double noiseEmissionPlusOneCar = 0.;
        double noiseEmissionPlusOneHgv = 0.;

        int n_car= noiseLink.getAgentsEntering(car);
        int n_hgv =  noiseLink.getAgentsEntering(RLS90VehicleType.hgv);

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
        noiseLink.setEmissionPlusOneVehicle(car, noiseEmissionPlusOneCar);
        noiseLink.setEmissionPlusOneVehicle(RLS90VehicleType.hgv, noiseEmissionPlusOneHgv);
    }

    @Override
    public double calculateSingleVehicleLevel(NoiseVehicleType type, NoiseLink noiseLink) {
        double v = getV(noiseLink, type);
        switch ((RLS90VehicleType) type) {
            case car:
                return calculateLCar(v);
            case hgv:
                return calculateLHdv(v);
            default:
                throw new IllegalStateException("Unexpected value: " + type);
        }
    }

    static double calculateMittelungspegelLm(int n, double p) {

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

    static double calculateGeschwindigkeitskorrekturDv (double vCar , double vHdv , double p) {

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
        return 27.7 + (10 * Math.log10(1.0 + Math.pow(0.02 * vCar, 3.0)));
    }

    static double calculateLHdv(double vHdv) {
        return 23.1 + (12.5 * Math.log10(vHdv));
    }

    private double getV(NoiseLink noiseLink, NoiseVehicleType type) {
        Link link = network.getLinks().get(noiseLink.getId());

        double velocity = (link.getFreespeed()) * 3.6;

        double freespeedCar = velocity;

        if (noiseParams.isUseActualSpeedLevel()) {

            // use the actual speed level if possible
            if (noiseLink != null) {

                if (noiseLink.getTravelTime_sec(type) == 0.
                        || noiseLink.getAgentsLeaving(type) == 0) {
                    // use the maximum speed level

                } else {
                    double averageTravelTimeCar_sec =
                            noiseLink.getTravelTime_sec(type) / noiseLink.getAgentsLeaving(type);
                    velocity = 3.6 * (link.getLength() / averageTravelTimeCar_sec );
                }
            }
        }

        if (velocity > freespeedCar) {
            throw new RuntimeException(velocity + " > " + freespeedCar + ". This should not be possible. Aborting...");
        }

        if (!noiseParams.isAllowForSpeedsOutsideTheValidRange()) {
            // shifting the speed into the allowed range defined by the RLS-90 computation approach
            final Range<Double> validSpeedRange = type.getValidSpeedRange();
            if (!validSpeedRange.contains(velocity)) {
                velocity = Math.min(Math.max(validSpeedRange.lowerEndpoint(), velocity), validSpeedRange.upperEndpoint());
            }
        }
        return velocity;
    }
}
