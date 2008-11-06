/* *********************************************************************** *
 * project: org.matsim.*
 * KmlNetworkWriter.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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
 *
 */
public interface BasicVehicleType {

	public void setDescription(String desc);

	public void setLength(double length);

	public void setWidth(double width);

	public void setMaximumVelocity(double meterPerSecond);

	public void setEngineInformation(BasicEngineInformation currentEngineInfo);

	public void setCapacity(BasicVehicleCapacity capacity);

	public double getWidth();

	public double getMaximumVelocity();
	
	public double getLength();
	
	public BasicEngineInformation getEngineInformation();
	
	public String getDescription();
	
	public BasicVehicleCapacity getCapacity();
	
	public String getTypeId();
}
