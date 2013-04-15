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

import playground.thibautd.utils.ReflectiveModule;

public class WeightsConfigGroup extends ReflectiveModule {
	public final static String GROUP_NAME = "groupReplanningWeights";
	private double reRoute = -1;
	private double timeMutator = -1;
	private double jointTripMutation = -1;
	private double modeMutation = -1;
	private double logitSelection = -1;
	private double vehicleAllocation = -1;
	private double planVehicleAllocation = -1;
	private boolean jtmOptimizes = true;
	private Synchro doSynchronize = Synchro.dynamic;

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

	@StringGetter( "doSynchronize" )
	public Synchro getSynchronize() {
		return doSynchronize;
	}

	@StringSetter( "doSynchronize" )
	public void setSynchronize(final String v) {
		this.doSynchronize = Synchro.valueOf( v.toLowerCase() );
	}

}
