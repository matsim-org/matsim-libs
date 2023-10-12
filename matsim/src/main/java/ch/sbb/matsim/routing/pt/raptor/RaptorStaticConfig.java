/* *********************************************************************** *
 * project: org.matsim.* 												   *
 *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2023 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */
package ch.sbb.matsim.routing.pt.raptor;

import ch.sbb.matsim.config.SwissRailRaptorConfigGroup;

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
    private double transferWalkMargin = 5;

    private boolean useModeMappingForPassengers = false;
    private final Map<String, String> passengerModeMappings = new HashMap<>();

    private boolean useCapacityConstraints = false;

    private RaptorOptimization optimization = RaptorOptimization.OneToOneRouting;

	private SwissRailRaptorConfigGroup.IntermodalLegOnlyHandling intermodalLegOnlyHandling = SwissRailRaptorConfigGroup.IntermodalLegOnlyHandling.forbid;

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

    public double getTransferWalkMargin() {
        return transferWalkMargin;
    }

    public void setTransferWalkMargin(double transferWalkMargin) {
        this.transferWalkMargin = transferWalkMargin;
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

    public boolean isUseCapacityConstraints() {
        return this.useCapacityConstraints;
    }

    public void setUseCapacityConstraints(boolean useCapacityConstraints) {
        this.useCapacityConstraints = useCapacityConstraints;
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

	public SwissRailRaptorConfigGroup.IntermodalLegOnlyHandling getIntermodalLegOnlyHandling() {
		return intermodalLegOnlyHandling;
	}

	public void setIntermodalLegOnlyHandling(SwissRailRaptorConfigGroup.IntermodalLegOnlyHandling intermodalLegOnlyHandling) {
		this.intermodalLegOnlyHandling = intermodalLegOnlyHandling;
	}
}
