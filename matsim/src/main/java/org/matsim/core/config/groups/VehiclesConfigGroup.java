/* *********************************************************************** *
 * project: org.matsim.*												   *
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
package org.matsim.core.config.groups;

import org.matsim.core.config.ReflectiveConfigGroup;

/**
 * @author nagel
 *
 */
public final class VehiclesConfigGroup extends ReflectiveConfigGroup {

	public static final String GROUP_NAME = "vehicles" ;
	
	private static final String INPUT_FILE = "vehiclesFile" ;

	private String inputFile = null ;

	public VehiclesConfigGroup() {
		super( GROUP_NAME );
	}
	
	@StringSetter(INPUT_FILE)
	public final void setVehiclesFile( String str ) {
		this.inputFile = str ;
	}
	@StringGetter(INPUT_FILE)
	public final String getVehiclesFile() {
		return this.inputFile ;
	}
	

}
