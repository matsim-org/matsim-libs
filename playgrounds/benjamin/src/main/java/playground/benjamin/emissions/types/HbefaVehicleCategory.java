/* *********************************************************************** *
 * project: org.matsim.*
 * HbefaVehicleCategory.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package playground.benjamin.emissions.types;

/**
 * @author benjamin
 *
 */
public enum HbefaVehicleCategory {

	PASSENGER_CAR("pass. car"), HEAVY_DUTY_VEHICLE("HDV");
	
	private String key;
	
	private HbefaVehicleCategory(String key) {
		this.key = key;
	}
	
	public String getText(){
		return key;
	}
}
