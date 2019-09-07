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
public final class VehicleType implements Attributable {
	// yy should maybe the whole type be immutable? I guess that the question is how people use this.  If they have a vehicle, get the type, and then keep
	// a reference to the type, then replacing the type means that it will have another reference, and these users will not notice.  ???????

	private double width = 1.0;
	private double maxVelocity = Double.POSITIVE_INFINITY; // default: constrained only by the link speed
	private double length = 7.5;
	private double pcuEquivalents = 1.0;
	private double flowEfficiencyFactor = 1.0;
	private final EngineInformation engineInformation = new EngineInformation() ;
	private CostInformation costInformation = new CostInformation() ;
//	private FreightCapacity freightCapacity;
	private String description;
	private final VehicleCapacity capacity = new VehicleCapacity();
	private String networkMode = TransportMode.car ;

	private Id<VehicleType> id;

	private final Attributes attributes = new Attributes();

	public VehicleType( Id<VehicleType> typeId ) {
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
	public final void setAccessTime(double seconds) {
		VehicleUtils.setAccessTime(this, seconds);
	}

	/**
	 * @deprecated please use {@see VehicleUtils} instead.
	 */
	@Deprecated
	public final void setEgressTime(double seconds) {
		VehicleUtils.setEgressTime(this, seconds);
	}

	/**
	 * @deprecated please use {@see VehicleUtils} instead.
	 */
	@Deprecated
	public final VehicleUtils.DoorOperationMode getDoorOperationMode() {
		return VehicleUtils.getDoorOperationMode( this ) ;
	}

	/**
	 * @deprecated please use {@see VehicleUtils} instead.
	 */
	@Deprecated
	public final void setDoorOperationMode( VehicleUtils.DoorOperationMode mode ) {
		VehicleUtils.setDoorOperationMode( this, mode ) ;
	}

	public final double getPcuEquivalents() {
		return pcuEquivalents;
	}

	public final void setPcuEquivalents(double pcuEquivalents) {
		this.pcuEquivalents = pcuEquivalents;
	}

	public final double getFlowEfficiencyFactor() {
		return flowEfficiencyFactor;
	}

	public final void setFlowEfficiencyFactor(double flowEfficiencyFactor) {
		this.flowEfficiencyFactor = flowEfficiencyFactor;
	}

	@Override
	public final  Attributes getAttributes() {
		return attributes ;
	}

//	public final void setCapacity(VehicleCapacity capacity) {
//		this.capacity = capacity;
//	}

	public final void setDescription(String desc) {
		this.description = desc;
	}

//	public final void setEngineInformation( EngineInformation engineInformation ) {
//		this.engineInformation = engineInformation;
//	}

//	/**
//	 * @deprecated please use {@see VehicleUtils} -> setHBEFATechology instead.
//	 */
//	@Deprecated
//	public final void setFreightCapacity(FreightCapacity freightCapacity) {
//		this.capacity.setFreightCapacity( freightCapacity );
//	}

	public final void setCostInformation(CostInformation costInformation) {
		this.costInformation = costInformation;
	}

	public final void setLength(double length) {
		this.length = length;
	}

	public final void setMaximumVelocity(double meterPerSecond) {
		this.maxVelocity = meterPerSecond;
	}

	public final void setWidth(double width) {
		this.width = width;
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

//	/**
//	 * @deprecated please use {@see VehicleUtils} -> getHBEFATechology instead.
//	 */
//	@Deprecated
//	public final FreightCapacity getFreightCapacity() {
//		return this.capacity.getFreightCapacity();
//	}

    public final String getNetworkMode() {
        return networkMode;
    }

    public final void setNetworkMode(String networkMode) {
        this.networkMode = networkMode;
    }
}
