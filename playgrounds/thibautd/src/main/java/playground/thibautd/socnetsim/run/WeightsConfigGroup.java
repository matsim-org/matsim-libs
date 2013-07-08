/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package playground.thibautd.socnetsim.run;

import org.matsim.core.config.experimental.ReflectiveModule;

public class WeightsConfigGroup extends ReflectiveModule {
	public final static String GROUP_NAME = "groupReplanningWeights";
	private double reRoute = -1;
	private double timeMutator = -1;
	private double jointTripMutation = -1;
	private double modeMutation = -1;
	private double logitSelection = -1;
	private double vehicleAllocation = -1;
	private double planVehicleAllocation = -1;
	private double optimizedVehicleAllocation = -1;
	private double recomposeJointPlans = -1;
	private boolean jtmOptimizes = true;
	private Synchro doSynchronize = Synchro.dynamic;
	private boolean checkConsistency = false;
	private int graphWriteInterval = 25;
	private int disableInnovationAfterIter = -1;
	private boolean considerVehicleIncompatibilities = true;
	private double initialTimeMutationTemperature = 24;

	public static enum Synchro {
		dynamic, none, all;
	}

	public WeightsConfigGroup() {
		super( GROUP_NAME );
	}

	@StringGetter( "reallocateVehiclePlan" )
	public double getPlanLevelReallocateVehicleWeight() {
		return planVehicleAllocation;
	}

	@StringSetter( "reallocateVehiclePlan" )
	public void setPlanLevelReallocateVehicleWeight(final double v) {
		planVehicleAllocation = v;
	}

	@StringGetter( "reallocateVehicleTour" )
	public double getTourLevelReallocateVehicleWeight() {
		return vehicleAllocation;
	}

	@StringSetter( "reallocateVehicleTour" )
	public void setTourLevelReallocateVehicleWeight(final double v) {
		this.vehicleAllocation = v;
	}

	@StringGetter( "optimizeVehicleTour" )
	public double getTourLevelOptimizeVehicleWeight() {
		return optimizedVehicleAllocation;
	}

	@StringSetter( "optimizeVehicleTour" )
	public void setTourLevelOptimizeVehicleWeight(final double v) {
		this.optimizedVehicleAllocation = v;
	}


	@StringGetter( "reRoute" )
	public double getReRouteWeight() {
		return reRoute;
	}

	@StringSetter( "reRoute" )
	public void setReRouteWeight(final double v) {
		this.reRoute = v;
	}

	@StringGetter( "timeMutator" )
	public double getTimeMutationWeight() {
		return timeMutator;
	}

	@StringSetter( "timeMutator" )
	public void setTimeMutationWeight(final double v) {
		this.timeMutator = v;
	}

	@StringGetter( "jointTripMutation" )
	public double getJointTripMutationWeight() {
		return jointTripMutation;
	}

	@StringSetter( "jointTripMutation" )
	public void setJointTripMutationWeight(final double v) {
		this.jointTripMutation = v;
	}

	@StringGetter( "modeMutation" )
	public double getModeMutationWeight() {
		return modeMutation;
	}

	@StringSetter( "modeMutation" )
	public void setModeMutationWeight(final double v) {
		this.modeMutation = v;
	}

	@StringGetter( "logitSelection" )
	public double getLogitSelectionWeight() {
		return logitSelection;
	}

	@StringSetter( "logitSelection" )
	public void setLogitSelectionWeight(final double v) {
		this.logitSelection = v;
	}

	public void setAllToZero() {
		reRoute = 0;
		timeMutator = 0;
		jointTripMutation = 0;
		modeMutation = 0;
		logitSelection = 0;
	}

	@StringGetter( "jtmOptimizes" )
	public boolean getJtmOptimizes() {
		return jtmOptimizes;
	}

	@StringSetter( "jtmOptimizes" )
	public void setJtmOptimizes(final boolean v) {
		this.jtmOptimizes = v;
	}

	@StringGetter( "recomposeJointPlansRandom" )
	public double getRecomposeJointPlansRandomlyWeight() {
		return recomposeJointPlans;
	}

	@StringSetter( "recomposeJointPlansRandom" )
	public void setRecomposeJointPlansRandomlyWeight(final double v) {
		this.recomposeJointPlans = v;
	}


	@StringGetter( "doSynchronize" )
	public Synchro getSynchronize() {
		return doSynchronize;
	}

	@StringSetter( "doSynchronize" )
	public void setSynchronize(final String v) {
		this.doSynchronize = Synchro.valueOf( v.toLowerCase() );
	}

	@StringGetter( "checkConsistency" )
	public boolean getCheckConsistency() {
		return checkConsistency;
	}

	@StringSetter( "checkConsistency" )
	public void setCheckConsistency(final boolean b) {
		this.checkConsistency = b;
	}

	@StringSetter( "graphWriteInterval" )
	public void setGraphWriteInterval(final int i) {
		this.graphWriteInterval = i;
	}

	@StringGetter( "graphWriteInterval" )
	public int getGraphWriteInterval() {
		return this.graphWriteInterval;
	}

	@StringGetter( "disableInnovationAfterIteration" )
	public int getDisableInnovationAfterIter() {
		return this.disableInnovationAfterIter;
	}

	@StringSetter( "disableInnovationAfterIteration" )
	public void setDisableInnovationAfterIteration(int disableInnovationAfterIter) {
		this.disableInnovationAfterIter = disableInnovationAfterIter;
	}

	@StringGetter( "considerVehicleIncompatibilities" )
	public boolean getConsiderVehicleIncompatibilities() {
		return this.considerVehicleIncompatibilities;
	}

	@StringSetter( "considerVehicleIncompatibilities" )
	public void setConsiderVehicleIncompatibilities(
			final boolean considerVehicleIncompatibilities) {
		this.considerVehicleIncompatibilities = considerVehicleIncompatibilities;
	}

	@StringGetter( "initialTimeMutationTemperature" )
	public double getInitialTimeMutationTemperature() {
		return this.initialTimeMutationTemperature;
	}

	@StringSetter( "initialTimeMutationTemperature" )
	public void setInitialTimeMutationTemperature(
			final double initialTimeMutationTemperature) {
		this.initialTimeMutationTemperature = initialTimeMutationTemperature;
	}
}
