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
import org.matsim.utils.objectattributes.attributable.Attributes;

/**
 * @author dgrether
 */
public class VehicleTypeImpl implements VehicleType {

	private double width = 1.0;
	private double maxVelocity = Double.POSITIVE_INFINITY; // default: constrained only by the link speed
	private double length = 7.5;
    private double pcuEquivalents = 1.0;
    private double flowEfficiencyFactor = 1.0;
	private EngineInformation engineInformation;
	private CostInformation costInformation;		//TODO: Needs to be created, kmt feb19
	private FreightCapacity freightCapacity;
	private String description;
	private VehicleCapacity capacity;
	
	/**
	 * default from xml schema
	 */
	private double accessTime = 1.0;
	
	/**
	 * default from xml schema
	 */
	private double egressTime = 1.0;

	private Id<VehicleType> id;

	private DoorOperationMode doorOperationMode = DoorOperationMode.serial;

	private final Attributes attributes = new Attributes();

	public VehicleTypeImpl(Id<VehicleType> typeId) {
		this.id = typeId;
	}



	@Override
	public String getDescription() {
		return description;
	}

	@Override
	public VehicleCapacity getCapacity() {
		return capacity;
	}

	@Override
	public Id<VehicleType> getId() {
		return id;
	}

	@Override
	public double getAccessTime() {
//		return this.accessTime;
		return VehicleUtils.getAccessTime(this);
	}

	@Override
	public double getEgressTime() {
//		return this.egressTime;
		return VehicleUtils.getEgressTime(this);
	}

	@Override
	public void setAccessTime(double seconds) {
//		this.accessTime = seconds;
		VehicleUtils.setAccessTime(this, seconds);
	}

	@Override
	public void setEgressTime(double seconds) {
//		this.egressTime = seconds;
		VehicleUtils.setEgressTime(this, seconds);
	}

	@Override
	public DoorOperationMode getDoorOperationMode() {
//		return this.doorOperationMode;
		return VehicleUtils.getDoorOperationMode( this ) ;
	}

	@Override
	public void setDoorOperationMode(DoorOperationMode mode) {
//		this.doorOperationMode = mode;
		VehicleUtils.setDoorOperationMode( this, mode ) ;
	}

    @Override
    public double getPcuEquivalents() {
        return pcuEquivalents;
    }

    @Override
    public void setPcuEquivalents(double pcuEquivalents) {
        this.pcuEquivalents = pcuEquivalents;
    }

    @Override
    public double getFlowEfficiencyFactor() {
        return flowEfficiencyFactor;
    }

    @Override
    public void setFlowEfficiencyFactor(double flowEfficiencyFactor) {
        this.flowEfficiencyFactor = flowEfficiencyFactor;
    }

	@Override
	public Attributes getAttributes() {
		return attributes ;
    }

	@Override
	public void setCapacity(VehicleCapacity capacity) {
		this.capacity = capacity;
	}

	@Override
	public void setDescription(String desc) {
		this.description = desc;
	}

	@Override
	public void setEngineInformation(EngineInformation engineInformation) {
		this.engineInformation = engineInformation;
	}

	@Override
	public void setFreightCapacity(FreightCapacity freightCapacity) { this.freightCapacity = freightCapacity; }

	@Override
	public void setCostInformation(CostInformation costInformation) {this.costInformation = costInformation; }

	@Override
	public void setLength(double length) {
		this.length = length;
	}

	@Override
	public void setMaximumVelocity(double meterPerSecond) {
		this.maxVelocity = meterPerSecond;
	}

	@Override
	public void setWidth(double width) {
		this.width = width;
	}

	@Override
	public double getWidth() {
		return width;
	}

	@Override
	public double getMaximumVelocity() {
		return maxVelocity;
	}

	@Override
	public double getLength() {
		return length;
	}

	@Override
	public EngineInformation getEngineInformation() {
		return engineInformation;
	}

	@Override
	public CostInformation getCostInformation() { return costInformation;	}

	@Override
	public FreightCapacity getFreightCapacity() { return freightCapacity;	}
}
