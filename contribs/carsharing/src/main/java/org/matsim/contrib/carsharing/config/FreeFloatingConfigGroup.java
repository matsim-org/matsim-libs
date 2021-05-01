/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

package org.matsim.contrib.carsharing.config;

import org.matsim.core.config.ReflectiveConfigGroup;
/** 
 * @author balac
 */

public class FreeFloatingConfigGroup extends ReflectiveConfigGroup {
	
	public static final String GROUP_NAME = "FreeFloating";	

	private String areasInputFile = null;
	
	private boolean useFreeFloating = false;	
	
	public FreeFloatingConfigGroup() {
		super(GROUP_NAME);
	}
		

	@StringGetter( "areasFreefloating" )
	public String getAreas() {
		return this.areasInputFile;
	}

	@StringSetter( "areasFreefloating" )
	public void setAreas(final String areasInputFile) {
		this.areasInputFile = areasInputFile;
	}
	
	@StringGetter( "useFreeFloating" )
	public boolean useFeeFreeFloating() {
		return this.useFreeFloating;
	}

	@StringSetter( "useFreeFloating" )
	public void setUseFeeFreeFloating(final boolean useFreeFloating) {
		this.useFreeFloating = useFreeFloating;
	}
	
}
