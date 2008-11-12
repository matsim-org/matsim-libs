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

package org.matsim.basic.v01;

/**
 * @author dgrether
 */
public class BasicVehicleTypeImpl implements BasicVehicleType {

	private double width = 1.0;
	private double maxVelocity = 1.0;
	private double length = 7.5;
	private BasicEngineInformation engineInformation;
	private String description;
	private BasicVehicleCapacity capacity;
	
	private String typeId;

	public BasicVehicleTypeImpl(String typeId) {
		this.typeId = typeId;
	}

	public void setCapacity(BasicVehicleCapacity capacity) {
		this.capacity = capacity;
	}

	public void setDescription(String desc) {
		this.description = desc;
	}

	public void setEngineInformation(BasicEngineInformation engineInformation) {
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

	
	public BasicEngineInformation getEngineInformation() {
		return engineInformation;
	}

	
	public String getDescription() {
		return description;
	}

	
	public BasicVehicleCapacity getCapacity() {
		return capacity;
	}

	
	public String getTypeId() {
		return typeId;
	}

}
