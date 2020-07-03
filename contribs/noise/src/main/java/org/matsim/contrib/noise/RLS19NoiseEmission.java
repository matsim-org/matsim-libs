package org.matsim.contrib.noise;

class RLS19NoiseEmission implements NoiseEmission {

    enum RLS19IntersectionType {

        signalized(3),
        roundabout(2),
        other(0);

        private final double correction;

        RLS19IntersectionType(double correction) {
            this.correction = correction;
        }

        public double getCorrection() {
            return correction;
        }
    }

    @Override
    public void calculateEmission(NoiseLink link) {
        return;
    }

    /**
     * laengenbezogener Schalleistungspegel LW einer Quelllinie in dB(A)
     * <p> Length-related sound level of a line source
     *
     * @return emission in dB(A)
     */
    public double calculateLinkNoise() {

        double pLkw1 = 0;
        double pLkw2 = 0;
        final double pPkw = 100 - pLkw1 - pLkw2;

        double vPkw = 0;
        double vLkw1 = 0;
        double vLkw2 = 0;

        double m = 0;

        double g = 0;
        RLS19IntersectionType intersectionType = RLS19IntersectionType.other;
        double intersectionDistance = 0;

        double singlePkwEmission
                = calculateSingleVehicleEmission(RLS19VehicleType.pkw, vPkw, g, intersectionType, intersectionDistance);
        double singleLkw1Emission
                = calculateSingleVehicleEmission(RLS19VehicleType.lkw1, vLkw1, g, intersectionType, intersectionDistance);
        double singleLkw2Emission
                = calculateSingleVehicleEmission(RLS19VehicleType.lkw2, vLkw2, g, intersectionType, intersectionDistance);

        double partPkw = calculateVehicleTypeNoise(pPkw, vPkw, singlePkwEmission);
        double partLkw1 = calculateVehicleTypeNoise(pLkw1, vLkw1, singleLkw1Emission);
        double partLkw2 = calculateVehicleTypeNoise(pLkw2, vLkw2, singleLkw2Emission);

        double emission = 10 * Math.log10(m) + 10 * Math.log10( partPkw + partLkw1 + partLkw2) - 30;
        return emission;
    }

    private double calculateVehicleTypeNoise(double p, double v, double singleVehicleEmission) {
        double part = ( p / 100) * (Math.pow(10, 0.1 * singleVehicleEmission) / v);
        return part;
    }

    /**
     * Schallleistungspegel eines Fahrzeuges der Fahrzeuggruppe {@link RLS19VehicleType} mit Geschwindigkeit v
     *
     * <p> Sound level of a vehicle of type {@link RLS19VehicleType} with velocity v
     *
     * @return emission in dB(A)
     */
    private double calculateSingleVehicleEmission(RLS19VehicleType vehicleType, double v, double g,
                                                  RLS19IntersectionType intersectionType, double intersectionDistance) {
        double baseValue = calculateBaseVehicleTypeEmission(vehicleType, v);
        double surfaceCorrection = calculateSurfaceCorrection();
        double gradientCorrection = calculateGradientCorrection(g, v, vehicleType);
        double intersectionCorrection = calculateIntersectionCorrection(intersectionType, intersectionDistance);
        double reflectionCorrection = calculateReflectionCorrection();

        double emission = baseValue + surfaceCorrection + gradientCorrection + intersectionCorrection + reflectionCorrection;
        return emission;
    }

    /**
     * Die Stoerwirkung durch das Anfahren und Bremsen der Fahrzeuge an Knotenpunkten wird in Abhaengigkeit
     * vom Knotenpunkttyp {@link RLS19IntersectionType} und von der Entfernung zum Schnittpunkt von sich
     * kreuzenden oder zusammentreffenden Quellinien bestimmt (=nodes)
     *
     * <p> The disturbance caused by the starting and braking of vehicles at junctions is determined
     * according to the type of junction {@link RLS19IntersectionType} and the distance to the point of
     * intersection of intersecting or converging source lines (=nodes)
     */
    private double calculateIntersectionCorrection(RLS19IntersectionType intersectionType, double distance) {
        double correction = intersectionType.correction * Math.max(1 - (distance / 120.), 0);
        return correction;
    }

    /**
     * Auf Steigungs- und Gefaellestrecken treten erhoehte Schallemissionen auf.
     *
     * <p> On uphill and downhill stretches, increased noise emissions occur.

     * @return gradient correction in dB(A)
     */
    private double calculateGradientCorrection(double g, double v, RLS19VehicleType vehicleType) {
        double correction = 0;
        switch (vehicleType) {
            case pkw:
                if(g < -6) {
                    correction = ((g + 6) / -6) * ((90 - Math.min(v, 70)) / 20);
                } else if (g > 2) {
                    correction = ((g -2) / 10) * ((v + 70) / 100);
                }
                break;
            case lkw1:
                if(g < -4) {
                    correction = ((g + 4) / -8) * ((v - 20) / 10);
                } else if (g > 2) {
                    correction = ((g -2) / 10) * (v / 10);
                }
                break;
            case lkw2:
                if(g < -4) {
                    correction = ((g + 4) / -8) * (v / 10);
                } else if (g > 2) {
                    correction = ((g -2) / 10) * ((v + 10) / 10);
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
    private double calculateBaseVehicleTypeEmission(RLS19VehicleType vehicleType, double v) {
        double emission = vehicleType.getEmissionParameterA()
                + 10 * Math.log10(1 + Math.pow(v / vehicleType.getEmissionParameterB(), vehicleType.getEmissionParameterC()));
        return emission;
    }

    //TODO
    private double calculateReflectionCorrection() {
        return 0;
    }

    //TODO
    private double calculateSurfaceCorrection() {
        return 0;
    }
}
