package org.matsim.contrib.noise;

import com.google.common.collect.Range;
import com.google.inject.Inject;
import org.geotools.api.geometry.Position;
import org.geotools.api.referencing.FactoryException;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.geometry.Position2D;
import org.geotools.referencing.CRS;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;

import static org.matsim.contrib.noise.RLS19VehicleType.*;

/**
 * @author nkuehnel
 */
class RLS19NoiseEmission implements NoiseEmission {

    public static final String GRADIENT = "GRADIENT";

    private final NoiseConfigGroup noiseParams;
    private final Network network;
    private final RoadSurfaceContext surfaceContext;
    private final DEMContext demContext;
    private CoordinateReferenceSystem crs;

    @Inject
    RLS19NoiseEmission(Scenario scenario, RoadSurfaceContext surfaceContext, DEMContext demContext) {
        Config config = scenario.getConfig();
        try {
            crs = CRS.decode(config.global().getCoordinateSystem());
        } catch (FactoryException e) {
            e.printStackTrace();
        }
        noiseParams = ConfigUtils.addOrGetModule(config, NoiseConfigGroup.class);
        network = scenario.getNetwork();
        this.surfaceContext = surfaceContext;
        this.demContext = demContext;
    }

    /**
     * laengenbezogener Schalleistungspegel LW einer Quelllinie in dB(A)
     * <p> Length-related sound level of a line source
     *
     * @return emission in dB(A)
     */
    @Override
    public void calculateEmission(NoiseLink noiseLink) {

        int nPkw = (int) ((noiseLink.getAgentsEntering(pkw)
                * (noiseParams.getScaleFactor()))
                * (3600. / noiseParams.getTimeBinSizeNoiseComputation()));
        int nLkw1 = (int) ((noiseLink.getAgentsEntering(lkw1)
                * (noiseParams.getScaleFactor()))
                * (3600. / noiseParams.getTimeBinSizeNoiseComputation()));
        int nLkw2 = (int) ((noiseLink.getAgentsEntering(lkw2)
                * (noiseParams.getScaleFactor()))
                * (3600. / noiseParams.getTimeBinSizeNoiseComputation()));

        double vPkw = getV(noiseLink, pkw);
        double vLkw1 = getV(noiseLink, lkw1);
        double vLkw2 = getV(noiseLink, lkw2);

        double emission = 0;
        if(nPkw + nLkw1 + nLkw2 > 0) {
            emission = calculateEmission(noiseLink, vPkw, vLkw1, vLkw2, nPkw, nLkw1, nLkw2);
        }
        noiseLink.setEmission(emission);

        double emissionPlusPkw = calculateEmission(noiseLink, vPkw, vLkw1, vLkw2, nPkw + 1, nLkw1, nLkw2);
        double emissionPlusLkw1 = calculateEmission(noiseLink, vPkw, vLkw1, vLkw2, nPkw, nLkw1 +1, nLkw2);
        double emissionPlusLkw2= calculateEmission(noiseLink, vPkw, vLkw1, vLkw2, nPkw, nLkw1, nLkw2 + 1);

        noiseLink.setEmissionPlusOneVehicle(pkw, emissionPlusPkw);
        noiseLink.setEmissionPlusOneVehicle(lkw1, emissionPlusLkw1);
        noiseLink.setEmissionPlusOneVehicle(lkw2, emissionPlusLkw2);
    }

    @Override
    public double calculateSingleVehicleLevel(NoiseVehicleType type, NoiseLink noiseLink) {
        int nPkw = 0;
        int nLkw1 = 0;
        int nLkw2 = 0;

        switch ((RLS19VehicleType) type) {
            case pkw:
                nPkw =1;
                break;
            case lkw1:
                nLkw1 = 1;
                break;
            case lkw2:
                nLkw2 = 1;
                break;
        }

        return calculateEmission(noiseLink, getV(noiseLink, pkw),getV(noiseLink, lkw1), getV(noiseLink, lkw2),
                nPkw, nLkw1, nLkw2);
    }

    double calculateEmission(NoiseLink noiseLink,
                                     double vPkw, double vLkw1, double vLkw2,
                                     int nPkw, int nLkw1, int nLkw2) {

        int m = nPkw + nLkw1 + nLkw2;
        if(m == 0) {
            return 0;
        }

        double pLkw1 = ((double) nLkw1) / m;
        double pLkw2 = ((double) nLkw2) / m;

        double singlePkwEmission
                = calculateSingleVehicleEmission(noiseLink, pkw, vPkw);
        double singleLkw1Emission
                = calculateSingleVehicleEmission(noiseLink, lkw1, vLkw1);
        double singleLkw2Emission
                = calculateSingleVehicleEmission(noiseLink, lkw2, vLkw2);

        double partPkw = calculateVehicleTypeNoise(1 - pLkw1 - pLkw2, vPkw, singlePkwEmission);
        double partLkw1 = calculateVehicleTypeNoise(pLkw1, vLkw1, singleLkw1Emission);
        double partLkw2 = calculateVehicleTypeNoise(pLkw2, vLkw2, singleLkw2Emission);

        return 10 * Math.log10(m) + 10 * Math.log10(partPkw + partLkw1 + partLkw2) - 30;
    }

    double calculateVehicleTypeNoise(double p, double v, double singleVehicleEmission) {
        return p * (Math.pow(10, 0.1 * singleVehicleEmission) / v);
    }

    /**
     * Schallleistungspegel eines Fahrzeuges der Fahrzeuggruppe {@link RLS19VehicleType} mit Geschwindigkeit v
     *
     * <p> Sound level of a vehicle of type {@link RLS19VehicleType} with velocity v
     *
     * @return emission in dB(A)
     */
    double calculateSingleVehicleEmission(NoiseLink link, RLS19VehicleType vehicleType, double v) {
        double baseValue = calculateBaseVehicleTypeEmission(vehicleType, v);
        double surfaceCorrection = calculateSurfaceCorrection(vehicleType, link, v);
        double gradientCorrection = calculateGradientCorrection(link, v, vehicleType);

        return baseValue + surfaceCorrection + gradientCorrection;
    }


    /**
     * Auf Steigungs- und Gefaellestrecken treten erhoehte Schallemissionen auf.
     *
     * <p> On uphill and downhill stretches, increased noise emissions occur.
     *
     * @return gradient correction in dB(A)
     */
    double calculateGradientCorrection(NoiseLink link, double v, RLS19VehicleType vehicleType) {
        double g = 0;
        Link matsimLink = network.getLinks().get(link.getId());
        if(noiseParams.isUseDEM()) {
            Coord from = matsimLink.getFromNode().getCoord();
            Coord to = matsimLink.getToNode().getCoord();

            //MATSim coord's x/y are inversed to geotools/jts
            Position positionFrom = new Position2D(crs, from.getY(), from.getX());
            Position positionTo = new Position2D(crs, to.getY(), to.getX());

            float elevationFrom = demContext.getElevation(positionFrom);
            float elevationTo = demContext.getElevation(positionTo);

            g = ((elevationTo - elevationFrom) / matsimLink.getLength()) * 100;
        } else {
            final Object gradient = matsimLink.getAttributes().getAttribute(GRADIENT);
            if(gradient != null) {
                g = (Double) gradient;
            }
        }

        double correction = 0;
        switch (vehicleType) {
            case pkw:
                if (g < -6) {
                    correction = ((g + 6) / -6) * ((90 - Math.min(v, 70)) / 20);
                } else if (g > 2) {
                    correction = ((g - 2) / 10) * ((v + 70) / 100);
                }
                break;
            case lkw1:
                if (g < -4) {
                    correction = ((g + 4) / -8) * ((v - 20) / 10);
                } else if (g > 2) {
                    correction = ((g - 2) / 10) * (v / 10);
                }
                break;
            case lkw2:
                if (g < -4) {
                    correction = ((g + 4) / -8) * (v / 10);
                } else if (g > 2) {
                    correction = ((g - 2) / 10) * ((v + 10) / 10);
                }
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + vehicleType);
        }
        return correction;
    }

    /**
     * Grundwert des Schalleistungspegels eines Fahrzeuges der Fahrzeuggruppe {@link RLS19VehicleType}
     * mit Geschwindigkeit v. Beschreibt die Schallemission des Fahrzeuges bei konstanter Geschwindigkeit
     * auf ebener, trockener Fahrbahn.
     *
     * <p> Base sound level of a vehicle of type {@link RLS19VehicleType} with velocity v. Describes sound level
     * of a vehicle driving with constant velocity on a flat and dry surface.
     *
     * @return emission in dB(A)
     */
    double calculateBaseVehicleTypeEmission(RLS19VehicleType vehicleType, double v) {
        double emission = vehicleType.getEmissionParameterA()
                + 10 * Math.log10(1 + Math.pow(v / vehicleType.getEmissionParameterB(), vehicleType.getEmissionParameterC()));
        return emission;
    }

    private double calculateSurfaceCorrection(RLS19VehicleType type, NoiseLink link, double velocity) {
        return surfaceContext.calculateSurfaceCorrection(type, link, velocity);
    }

    double getV(NoiseLink noiseLink, RLS19VehicleType type) {
        Link link = network.getLinks().get(noiseLink.getId());

        double v = (link.getFreespeed()) * 3.6;
        double freespeed = v;

        // use the actual speed level if possible
        if (noiseParams.isUseActualSpeedLevel()) {
            if (noiseLink.getTravelTime_sec(type) == 0.
                    || noiseLink.getAgentsLeaving(type) == 0) {
                // use the maximum speed level

            } else {
                double averageTravelTime_sec =
                        noiseLink.getTravelTime_sec(type) / noiseLink.getAgentsLeaving(type);
                v = 3.6 * (link.getLength() / averageTravelTime_sec);
            }
        }

        if (v > freespeed) {
            throw new RuntimeException(v + " > " + freespeed + ". This should not be possible. Aborting...");
        }

        if (!noiseParams.isAllowForSpeedsOutsideTheValidRange()) {
            // shifting the speed into the allowed range defined by the RLS-19 computation approach
            final Range<Double> validSpeedRange = type.getValidSpeedRange();
            if (!validSpeedRange.contains(v)) {
                v = Math.min(Math.max(validSpeedRange.lowerEndpoint(), v), validSpeedRange.upperEndpoint());
            }
        }
        return v;
    }
}
