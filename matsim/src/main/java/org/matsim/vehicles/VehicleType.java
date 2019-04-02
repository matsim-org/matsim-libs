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
public class VehicleType{

	private double width = 1.0;
	private double maxVelocity = Double.POSITIVE_INFINITY; // default: constrained only by the link speed
	private double length = 7.5;
    private double pcuEquivalents = 1.0;
    private double flowEfficiencyFactor = 1.0;
	private EngineInformation engineInformation;
	private CostInformation costInformation;
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

	public enum DoorOperationMode { serial, parallel }
	private DoorOperationMode doorOperationMode = DoorOperationMode.serial;

	private static final Attributes attributes = new Attributes();

	public VehicleType( Id<VehicleType> typeId ) {
		this.id = typeId;
	}

	public String getDescription() {
		return description;
	}

	public VehicleCapacity getCapacity() {
		return capacity;
	}

	public Id<VehicleType> getId() {
		return id;
	}

	/**
	 * @deprecated please use {@see VehicleUtils} instead.
	 */
	@Deprecated
	public double getAccessTime() {
		return VehicleUtils.getAccessTime(this);
	}

	/**
	 * @deprecated please use {@see VehicleUtils} instead.
	 */
	@Deprecated
	public double getEgressTime() {
		return VehicleUtils.getEgressTime(this);
	}

	/**
	 * @deprecated please use {@see VehicleUtils} instead.
	 */
	@Deprecated
	public void setAccessTime(double seconds) {
		VehicleUtils.setAccessTime(this, seconds);
	}

	/**
	 * @deprecated please use {@see VehicleUtils} instead.
	 */
	@Deprecated
	public void setEgressTime(double seconds) {
		VehicleUtils.setEgressTime(this, seconds);
	}

	/**
	 * @deprecated please use {@see VehicleUtils} instead.
	 */
	@Deprecated
	public DoorOperationMode getDoorOperationMode() {
		return VehicleUtils.getDoorOperationMode( this ) ;
	}

	/**
	 * @deprecated please use {@see VehicleUtils} instead.
	 */
	@Deprecated
	public void setDoorOperationMode(DoorOperationMode mode) {
		VehicleUtils.setDoorOperationMode( this, mode ) ;
	}

    public double getPcuEquivalents() {
        return pcuEquivalents;
    }

    public void setPcuEquivalents(double pcuEquivalents) {
        this.pcuEquivalents = pcuEquivalents;
    }

    public double getFlowEfficiencyFactor() {
        return flowEfficiencyFactor;
    }

    public void setFlowEfficiencyFactor(double flowEfficiencyFactor) {
        this.flowEfficiencyFactor = flowEfficiencyFactor;
    }

	public static Attributes getAttributes() {
		return attributes ;
    }

	public void setCapacity(VehicleCapacity capacity) {
		this.capacity = capacity;
	}

	public void setDescription(String desc) {
		this.description = desc;
	}

	public void setEngineInformation(EngineInformation engineInformation) {
		this.engineInformation = engineInformation;
	}

	public void setFreightCapacity(FreightCapacity freightCapacity) {
		this.freightCapacity = freightCapacity;
	}

	public void setCostInformation(CostInformation costInformation) {
		this.costInformation = costInformation;
	}

	public void setLength(double length) {
		this.length = length;
	}

	public void setMaximumVelocity(double meterPerSecond) {
		this.maxVelocity = meterPerSecond;
	}

	public void setWidth(double width) {
		this.width = width;
	}

	public double getWidth() {
		return width;
	}

	public double getMaximumVelocity() {
		return maxVelocity;
	}

	public double getLength() {
		return length;
	}

	public EngineInformation getEngineInformation() {
		return engineInformation;
	}

	public CostInformation getCostInformation() {
		return costInformation;
	}

	public FreightCapacity getFreightCapacity() {
		return freightCapacity;
	}
}
