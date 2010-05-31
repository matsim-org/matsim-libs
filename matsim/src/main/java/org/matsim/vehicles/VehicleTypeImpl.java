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

/**
 * @author dgrether
 */
public class VehicleTypeImpl implements VehicleType {

	private double width = 1.0;
	private double maxVelocity = 1.0;
	private double length = 7.5;
	private EngineInformation engineInformation;
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
	
	private Id id;

	private DoorOperationMode doorOperationMode = DoorOperationMode.serial;
	
	public VehicleTypeImpl(Id typeId) {
		this.id = typeId;
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

	public String getDescription() {
		return description;
	}

	public VehicleCapacity getCapacity() {
		return capacity;
	}

	public Id getId() {
		return id;
	}

  @Override
  public double getAccessTime() {
    return this.accessTime;
  }

  @Override
  public double getEgressTime() {
    return this.egressTime;
  }

  @Override
  public void setAccessTime(double seconds) {
    this.accessTime = seconds;
  }

  @Override
  public void setEgressTime(double seconds) {
    this.egressTime = seconds;
  }

	@Override
	public DoorOperationMode getDoorOperationMode() {
		return this.doorOperationMode;
	}

	@Override
	public void setDoorOperationMode(DoorOperationMode mode) {
		this.doorOperationMode = mode;
	}

}
