/*
 * Copyright (C) Schweizerische Bundesbahnen SBB, 2018.
 */

package ch.sbb.matsim.routing.pt.raptor;

import java.util.HashMap;
import java.util.Map;

/**
 * Static configuration of SwissRailRaptor used to initialize it.
 * These values are only used initially to build the necessary dataset for SwissRailRaptor
 * (see {@link SwissRailRaptorData}. Once initialized, changes to values in this class
 * will have no effect.
 *
 * @author mrieser / SBB
 */
public class RaptorStaticConfig {

    public enum RaptorOptimization {
        /**
         * Use this option if you plan to calculate simple from-to routes
         * (see {@link SwissRailRaptor#calcRoute(org.matsim.facilities.Facility, org.matsim.facilities.Facility, double, org.matsim.api.core.v01.population.Person)}).
         */
        OneToOneRouting,
        /**
         * Use this option if you plan to calculate one-to-all least-cost-path-trees
         * (see {@link SwissRailRaptor#calcTree(org.matsim.pt.transitSchedule.api.TransitStopFacility, double, RaptorParameters)}).
         */
        OneToAllRouting }

    /**
     * The distance in meters that agents can walk to get from one stop to
     * another stop of a nearby transit line.
     */
    private double beelineWalkConnectionDistance = 200.0;
    private double beelineWalkSpeed; // meter / second
    private double beelineWalkDistanceFactor = 1.0;

    private double minimalTransferTime = 60;

    private boolean useModeMappingForPassengers = false;
    private final Map<String, String> passengerModeMappings = new HashMap<>();

    private RaptorOptimization optimization = RaptorOptimization.OneToOneRouting;

    public double getBeelineWalkConnectionDistance() {
        return this.beelineWalkConnectionDistance;
    }

    public void setBeelineWalkConnectionDistance(double beelineWalkConnectionDistance) {
        this.beelineWalkConnectionDistance = beelineWalkConnectionDistance;
    }

    public double getBeelineWalkSpeed() {
        return this.beelineWalkSpeed;
    }

    public void setBeelineWalkSpeed(double beelineWalkSpeed) {
        this.beelineWalkSpeed = beelineWalkSpeed;
    }

    public double getBeelineWalkDistanceFactor() {
        return this.beelineWalkDistanceFactor;
    }

    public void setBeelineWalkDistanceFactor(double beelineWalkDistanceFactor) {
        this.beelineWalkDistanceFactor = beelineWalkDistanceFactor;
    }

    public double getMinimalTransferTime() {
        return this.minimalTransferTime;
    }

    public void setMinimalTransferTime(double minimalTransferTime) {
        this.minimalTransferTime = minimalTransferTime;
    }

    public boolean isUseModeMappingForPassengers() {
        return this.useModeMappingForPassengers;
    }

    public void setUseModeMappingForPassengers(boolean useModeMappingForPassengers) {
        this.useModeMappingForPassengers = useModeMappingForPassengers;
    }

    public void addModeMappingForPassengers(String routeMode, String passengerMode) {
        this.passengerModeMappings.put(routeMode, passengerMode);
    }

    public String getPassengerMode(String routeMode) {
        return this.passengerModeMappings.get(routeMode);
    }

    public RaptorOptimization getOptimization() {
        return this.optimization;
    }

    public void setOptimization(RaptorOptimization optimization) {
        this.optimization = optimization;
    }
}
