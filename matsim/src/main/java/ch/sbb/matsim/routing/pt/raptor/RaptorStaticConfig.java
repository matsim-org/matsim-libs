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
import org.matsim.api.core.v01.population.Person;
import org.matsim.facilities.Facility;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.utils.objectattributes.attributable.Attributes;

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
         * (see {@link SwissRailRaptor#calcRoute(Facility, Facility, double, double, double, Person, Attributes, RaptorRouteSelector)}
         */
        OneToOneRouting,
        /**
         * Use this option if you plan to calculate one-to-all least-cost-path-trees
         * (see {@link SwissRailRaptor#calcTree(TransitStopFacility, double, RaptorParameters, Person)} ).
         */
        OneToAllRouting }

	public enum RaptorTransferCalculation {
		/**
		 * Use this option if you want the algorithm to calculate all possible transfers
		 * up-front, which will allow for rapid lookup during routing, but may come with
		 * significant simulation startup time.
		 */
		Initial,

		/**
		 * Use this option if you want the algorithm to calculate transfers adaptively on demand,
		 * which avoids any simulation start-up time but may increase the routing time
		 * itself.
		 */
		Adaptive,

        /**
         * Use this option if you want the algorithm to calculate transfers on demand,
         * but not cache them. This allows for a large reduction in memory use, but may
         * drastically increase processing times.
         */
        Online
	}


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
    private final Map<String, Map<String,Double>> modeToModeTransferPenalties = new HashMap<>();

    private boolean useCapacityConstraints = false;

    private RaptorOptimization optimization = RaptorOptimization.OneToOneRouting;
    private RaptorTransferCalculation transferCalculation = RaptorTransferCalculation.Initial;

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
	public void addModeToModeTransferPenalty(String fromMode, String toMode, double transferPenalty) {
		this.modeToModeTransferPenalties.computeIfAbsent(fromMode,s->new HashMap<>()).put(toMode,transferPenalty);
	}
	public double getModeToModeTransferPenalty(String fromMode, String toMode){
		var fromModeSet = this.modeToModeTransferPenalties.get(fromMode);
		if (fromModeSet!=null){
			return fromModeSet.getOrDefault(toMode,0.0);
		}
		else return 0.0;
	}

	public boolean isUseModeToModeTransferPenalty(){
		return !this.modeToModeTransferPenalties.isEmpty();
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

    public RaptorTransferCalculation getTransferCalculation() {
        return this.transferCalculation;
    }

    public void setTransferCalculation(RaptorTransferCalculation transferCalculation) {
        this.transferCalculation = transferCalculation;
    }
}
