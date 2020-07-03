package org.matsim.contrib.noise;

import org.matsim.api.core.v01.Id;

enum RLS19VehicleType implements NoiseVehicleType {

    /**
     * Personenkraftwagen, Personenkraftwagen mit Anhaenger und Lieferwagen ( m < 3.5t).
     *
     * <p> Passenger cars, passenger cars with trailers and vans ( m < 3.5t).
     */
    pkw(88, 20, 3.06),
    /**
     * Lastkraftwagen ohne Anhaenger mit einer zulaessigen Masse ueber 3.5t und Busse.
     *
     * <p> Trucks without trailers with a permissible mass exceeding 3.5t and buses.
     */
    lkw1(100.3, 40, 4.33),
    /**
     * Lastkraftwagen mit Anhaenger bzw. Sattelkraftfahrzeuge (Zugmaschinen mit Auflieger) mit
     * einer zulaessigen Gesamtmasse ueber 3.5t
     *
     * <p> Lorries with trailers or articulated vehicles (trucks with trailers)
     * with a permissible mass exceeding 3.5t
     */
    lkw2(105.4, 50, 4.88);

    private final double emissionParameterA;
    private final double emissionParameterB;
    private final double emissionParameterC;

    private Id<NoiseVehicleType> id = Id.create(this.name(), NoiseVehicleType.class);

    RLS19VehicleType(double emissionParameterA, double emissionParameterB, double emissionParameterC) {
        this.emissionParameterA = emissionParameterA;
        this.emissionParameterB = emissionParameterB;
        this.emissionParameterC = emissionParameterC;
    }

    double getEmissionParameterA() {
        return emissionParameterA;
    }
    double getEmissionParameterB() {
        return emissionParameterB;
    }
    double getEmissionParameterC() {
        return emissionParameterC;
    }

    @Override
    public Id<NoiseVehicleType> getId() {
        return id;
    }
}
