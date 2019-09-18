/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package org.matsim.vehicles;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.utils.objectattributes.attributable.Attributable;
import org.matsim.utils.objectattributes.attributable.Attributes;

/**
 * @author dgrether
 */
public final class VehicleType implements Attributable{
	// deliberately final, please do not change.
	// If something like inheritance is needed, please change this class to VehicleTypeImpl, and extract interface under previous name VehicleType.
	// And then use delegation.

	// yy should maybe the whole type be immutable? I guess that the question is how people use this.  If they have a vehicle, get the type, and then keep
	// a reference to the type, then replacing the type means that it will have another reference, and these users will not notice.  ???????

	private double width = 1.0;
	private double maxVelocity = Double.POSITIVE_INFINITY; // default: constrained only by the link speed
	private double length = 7.5;
	private double pcuEquivalents = 1.0;
	private double flowEfficiencyFactor = 1.0;
	private final EngineInformation engineInformation = new EngineInformation() ;
	private final CostInformation costInformation = new CostInformation() ;
	private String description;
	private final VehicleCapacity capacity = new VehicleCapacity();
	private String networkMode = TransportMode.car ;
	private Id<VehicleType> id;
	private final Attributes attributes = new Attributes();

	VehicleType( Id<VehicleType> typeId ) {
		this.id = typeId;
	}
	public final String getDescription() {
		return description;
	}
	public final VehicleCapacity getCapacity() {
		return capacity;
	}
	public final Id<VehicleType> getId() {
		return id;
	}
	public final double getPcuEquivalents() {
		return pcuEquivalents;
	}
	public final VehicleType setPcuEquivalents( double pcuEquivalents ) {
		this.pcuEquivalents = pcuEquivalents;
		return this;
	}
	public final double getFlowEfficiencyFactor() {
		return flowEfficiencyFactor;
	}
	public final VehicleType setFlowEfficiencyFactor( double flowEfficiencyFactor ) {
		this.flowEfficiencyFactor = flowEfficiencyFactor;
		return this;
	}
	@Override public final  Attributes getAttributes() {
		return attributes ;
	}
	public final VehicleType setDescription( String desc ) {
		this.description = desc;
		return this;
	}
	public final VehicleType setLength( double length ) {
		this.length = length;
		return this;
	}
	public final VehicleType setMaximumVelocity( double meterPerSecond ) {
		this.maxVelocity = meterPerSecond;
		return this;
	}
	public final VehicleType setWidth( double width ) {
		this.width = width;
		return this;
	}
	public final double getWidth() {
		return width;
	}
	public final double getMaximumVelocity() {
		return maxVelocity;
	}
	public final double getLength() {
		return length;
	}
	public final EngineInformation getEngineInformation() {
		return engineInformation;
	}
	public final CostInformation getCostInformation() {
		return costInformation;
	}
	public final String getNetworkMode() {
		return networkMode;
	}
	public final void setNetworkMode( String networkMode ) {
		this.networkMode = networkMode;
	}

	// the following are attributes that did not seem universal enough and thus were relegated to from-form Attributes for the time being.  kai/kai, sep'19
	/**
	 * @deprecated please use {@see VehicleUtils} instead.
	 */
	@Deprecated
	public final double getAccessTime() {
		return VehicleUtils.getAccessTime(this);
	}
	/**
	 * @deprecated please use {@see VehicleUtils} instead.
	 */
	@Deprecated
	public final double getEgressTime() {
		return VehicleUtils.getEgressTime(this);
	}
	/**
	 * @deprecated please use {@see VehicleUtils} instead.
	 */
	@Deprecated
	public final void setAccessTime( double seconds ) {
		VehicleUtils.setAccessTime(this, seconds);
	}
	/**
	 * @deprecated please use {@see VehicleUtils} instead.
	 */
	@Deprecated
	public final void setEgressTime( double seconds ) {
		VehicleUtils.setEgressTime(this, seconds);
	}
	/**
	 * @deprecated please use {@see VehicleUtils} instead.
	 */
	@Deprecated
	public final DoorOperationMode getDoorOperationMode() {
		return VehicleUtils.getDoorOperationMode( this ) ;
	}
	/**
	 * @deprecated please use {@see VehicleUtils} instead.
	 */
	@Deprecated
	public final void setDoorOperationMode( DoorOperationMode mode ) {
		VehicleUtils.setDoorOperationMode( this, mode ) ;
	}

	@Deprecated // refactoring device, please inline
	public VehicleType setCapacityWeightInTons( int i ){
		this.getCapacity().setWeightInTons( i ) ;
		return this ;
	}
	@Deprecated // refactoring device, please inline
	public VehicleType setFixCost( double perDay ){
		this.getCostInformation().setFixedCost( perDay ) ;
		return this ;
	}
	@Deprecated // refactoring device, please inline
	public VehicleType setCostPerDistanceUnit( double perMeter ){
		this.getCostInformation().setCostsPerMeter( perMeter ) ;
		return this ;
	}
	@Deprecated // refactoring device, please inline
	public VehicleType setCostPerTimeUnit( double perSecond ){
		this.getCostInformation().setCostsPerSecond( perSecond ) ;
		return this ;
	}
	@Deprecated // refactoring device, please inline
	public CostInformation getVehicleCostInformation() {
		return this.getCostInformation() ;
	}
	@Deprecated // refactoring device, please inline
	public int getCarrierVehicleCapacity() {
		return this.getCapacity().getOther().intValue() ;
	}

	public enum DoorOperationMode { serial, parallel }

}
