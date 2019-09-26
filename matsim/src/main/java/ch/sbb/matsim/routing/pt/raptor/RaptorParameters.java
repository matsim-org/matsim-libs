/*
 * Copyright (C) Schweizerische Bundesbahnen SBB, 2017.
 */

package ch.sbb.matsim.routing.pt.raptor;

import ch.sbb.matsim.config.SwissRailRaptorConfigGroup;

import java.util.HashMap;
import java.util.Map;

/**
 * @author mrieser / SBB
 */
public class RaptorParameters {

    /**
     * The distance in meters in which stop facilities should be searched for
     * around the start and end coordinate.
     */
    private double searchRadius = 1000.0;

    /**
     * If no stop facility is found around start or end coordinate (see
     * {@link #searchRadius}), the nearest stop location is searched for
     * and the distance from start/end coordinate to this location is
     * extended by the given amount.<br />
     * If only one stop facility is found within {@link #searchRadius},
     * the radius is also extended in the hope to find more stop
     * facilities (e.g. in the opposite direction of the already found
     * stop).
     */
    private double extensionRadius = 200.0;

    /**
     * Factor with which direct walk generalized cost is multiplied before 
     * it is compared to the pt generalized cost. 
     * Set to a very high value to reduce direct walk results.
     */
	private double directWalkFactor = 1.0;

    private double beelineWalkSpeed; // meter / second

    private final Map<String, Double> marginalUtilityOfTravelTime_utl_s = new HashMap<>();

    private double marginalUtilityOfWaitingPt_utl_s;

    private double transferPenaltyFixCostPerTransfer = 0.0;
    private double transferPenaltyPerTravelTimeHour = 0.0;
    private double transferPenaltyMinimum = Double.NEGATIVE_INFINITY;
    private double transferPenaltyMaximum = Double.POSITIVE_INFINITY;

    private final SwissRailRaptorConfigGroup config;

    public RaptorParameters(SwissRailRaptorConfigGroup config) {
        this.config = config;
    }

    public SwissRailRaptorConfigGroup getConfig() {
        return config;
    }

    public double getSearchRadius() {
        return this.searchRadius;
    }

    public void setSearchRadius(double searchRadius) {
        this.searchRadius = searchRadius;
    }

    public double getExtensionRadius() {
        return this.extensionRadius;
    }

    public void setExtensionRadius(double extensionRadius) {
        this.extensionRadius = extensionRadius;
    }
    
    public double getDirectWalkFactor() {
        return this.directWalkFactor;
    }

    public void setDirectWalkFactor(double directWalkFactor) {
        this.directWalkFactor = directWalkFactor;
    }

    public double getBeelineWalkSpeed() {
        return this.beelineWalkSpeed;
    }

    public void setBeelineWalkSpeed(double beelineWalkSpeed) {
        this.beelineWalkSpeed = beelineWalkSpeed;
    }

    public double getMarginalUtilityOfTravelTime_utl_s(String mode) {
        Double marginalUtility = this.marginalUtilityOfTravelTime_utl_s.get(mode);
        if (marginalUtility == null) {
            throw new NullPointerException("Marginal utility of travel time is missing for mode: " + mode);
        }
        return marginalUtility;
    }

    public void setMarginalUtilityOfTravelTime_utl_s(String mode, double marginalUtilityOfTravelTime_utl_s) {
        this.marginalUtilityOfTravelTime_utl_s.put(mode, marginalUtilityOfTravelTime_utl_s);
    }

    public double getMarginalUtilityOfWaitingPt_utl_s() {
        return this.marginalUtilityOfWaitingPt_utl_s;
    }

    public void setMarginalUtilityOfWaitingPt_utl_s(double marginalUtilityOfWaitingPt_utl_s) {
        this.marginalUtilityOfWaitingPt_utl_s = marginalUtilityOfWaitingPt_utl_s;
    }

    public double getTransferPenaltyFixCostPerTransfer() {
        return transferPenaltyFixCostPerTransfer;
    }

    public void setTransferPenaltyFixCostPerTransfer(double transferPenaltyFixCostPerTransfer) {
        this.transferPenaltyFixCostPerTransfer = transferPenaltyFixCostPerTransfer;
    }

    public double getTransferPenaltyPerTravelTimeHour() {
        return this.transferPenaltyPerTravelTimeHour;
    }

    public void setTransferPenaltyPerTravelTimeHour(double transferPenaltyPerTravelTimeHour) {
        this.transferPenaltyPerTravelTimeHour = transferPenaltyPerTravelTimeHour;
    }

    public double getTransferPenaltyMinimum() {
        return this.transferPenaltyMinimum;
    }

    public void setTransferPenaltyMinimum(double transferPenaltyMinimum) {
        this.transferPenaltyMinimum = transferPenaltyMinimum;
    }

    public double getTransferPenaltyMaximum() {
        return this.transferPenaltyMaximum;
    }

    public void setTransferPenaltyMaximum(double transferPenaltyMaximum) {
        this.transferPenaltyMaximum = transferPenaltyMaximum;
    }

}
