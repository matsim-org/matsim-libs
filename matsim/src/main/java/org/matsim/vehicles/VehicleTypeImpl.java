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
import org.matsim.utils.objectattributes.attributable.Attributes;

/**
 * @author dgrether
 */
final class VehicleTypeImpl implements VehicleType{
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

	VehicleTypeImpl( Id<VehicleType> typeId ) {
		this.id = typeId;
	}
	@Override public final String getDescription() {
		return description;
	}
	@Override public final VehicleCapacity getCapacity() {
		return capacity;
	}
	@Override public final Id<VehicleType> getId() {
		return id;
	}
	@Override public final double getPcuEquivalents() {
		return pcuEquivalents;
	}
	@Override public final VehicleTypeImpl setPcuEquivalents( double pcuEquivalents ) {
		this.pcuEquivalents = pcuEquivalents;
		return this;
	}
	@Override public final double getFlowEfficiencyFactor() {
		return flowEfficiencyFactor;
	}
	@Override public final VehicleTypeImpl setFlowEfficiencyFactor( double flowEfficiencyFactor ) {
		this.flowEfficiencyFactor = flowEfficiencyFactor;
		return this;
	}
	@Override public final  Attributes getAttributes() {
		return attributes ;
	}
	@Override public final VehicleTypeImpl setDescription( String desc ) {
		this.description = desc;
		return this;
	}
	@Override public final VehicleTypeImpl setLength( double length ) {
		this.length = length;
		return this;
	}
	@Override public final VehicleTypeImpl setMaximumVelocity( double meterPerSecond ) {
		this.maxVelocity = meterPerSecond;
		return this;
	}
	@Override public final VehicleTypeImpl setWidth( double width ) {
		this.width = width;
		return this;
	}
	@Override public final double getWidth() {
		return width;
	}
	@Override public final double getMaximumVelocity() {
		return maxVelocity;
	}
	@Override public final double getLength() {
		return length;
	}
	@Override public final EngineInformation getEngineInformation() {
		return engineInformation;
	}
	@Override public final CostInformation getCostInformation() {
		return costInformation;
	}
	@Override public final String getNetworkMode() {
		return networkMode;
	}
	@Override public final void setNetworkMode( String networkMode ) {
		this.networkMode = networkMode;
	}

	/**
	 * @deprecated please use {@see VehicleUtils} instead.
	 */
	@Override @Deprecated
	public final double getAccessTime() {
		return VehicleUtils.getAccessTime(this);
	}
	/**
	 * @deprecated please use {@see VehicleUtils} instead.
	 */
	@Override @Deprecated
	public final double getEgressTime() {
		return VehicleUtils.getEgressTime(this);
	}
	/**
	 * @deprecated please use {@see VehicleUtils} instead.
	 */
	@Override @Deprecated
	public final void setAccessTime( double seconds ) {
		VehicleUtils.setAccessTime(this, seconds);
	}
	/**
	 * @deprecated please use {@see VehicleUtils} instead.
	 */
	@Override @Deprecated
	public final void setEgressTime( double seconds ) {
		VehicleUtils.setEgressTime(this, seconds);
	}
	/**
	 * @deprecated please use {@see VehicleUtils} instead.
	 */
	@Override @Deprecated
	public final DoorOperationMode getDoorOperationMode() {
		return VehicleUtils.getDoorOperationMode( this ) ;
	}
	/**
	 * @deprecated please use {@see VehicleUtils} instead.
	 */
	@Override @Deprecated
	public final void setDoorOperationMode( DoorOperationMode mode ) {
		VehicleUtils.setDoorOperationMode( this, mode ) ;
	}

	@Override @Deprecated // refactoring device, please inline
	public VehicleTypeImpl setCapacityWeightInTons( int i ){
		this.getCapacity().setWeightInTons( i ) ;
		return this ;
	}
	@Override @Deprecated // refactoring device, please inline
	public VehicleTypeImpl setFixCost( double perDay ){
		this.getCostInformation().setFixedCost( perDay ) ;
		return this ;
	}
	@Override @Deprecated // refactoring device, please inline
	public VehicleTypeImpl setCostPerDistanceUnit( double perMeter ){
		this.getCostInformation().setCostsPerMeter( perMeter ) ;
		return this ;
	}
	@Override @Deprecated // refactoring device, please inline
	public VehicleTypeImpl setCostPerTimeUnit( double perSecond ){
		this.getCostInformation().setCostsPerSecond( perSecond ) ;
		return this ;
	}

}
