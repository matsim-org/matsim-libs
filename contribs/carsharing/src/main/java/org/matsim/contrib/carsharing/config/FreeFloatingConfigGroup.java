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

import java.util.Map;

import org.matsim.core.config.ReflectiveConfigGroup;
/** 
 * @author balac
 */

public class FreeFloatingConfigGroup extends ReflectiveConfigGroup {
	
	public static final String GROUP_NAME = "FreeFloating";	
	
	private static final String PARAM_SEARCH_DISTANCE = "searchDistanceFreefloating";
	private static final String PARAM_AREAS = "areasFreefloating";
	private static final String PARAM_USE_FREEFLOATING = "useFreeFloating";

	private String areasInputFile = null;
	
	private boolean useFreeFloating = false;	
	
	private double searchDistance = 500.0;
	
	public FreeFloatingConfigGroup() {
		super(GROUP_NAME);
	}
	
	@StringGetter( PARAM_SEARCH_DISTANCE )
	public double getsearchDistance() {
		return this.searchDistance;
	}

	@StringSetter( PARAM_SEARCH_DISTANCE )
	public void setsearchDistance(final String searchDistance) {
		this.searchDistance = Double.parseDouble(searchDistance);
	}		

	@StringGetter( PARAM_AREAS )
	public String getAreas() {
		return this.areasInputFile;
	}

	@StringSetter( PARAM_AREAS )
	public void setAreas(final String areasInputFile) {
		this.areasInputFile = areasInputFile;
	}
	
	@StringGetter( PARAM_USE_FREEFLOATING )
	public boolean useFeeFreeFloating() {
		return this.useFreeFloating;
	}

	@StringSetter( PARAM_USE_FREEFLOATING )
	public void setUseFeeFreeFloating(final boolean useFreeFloating) {
		this.useFreeFloating = useFreeFloating;
	}
	
	 @Override
     public Map<String, String> getComments() {
         Map<String, String> map = super.getComments();
         map.put(PARAM_AREAS, "The path to the input file containg the service are of the freefloating service. If not defined the whole study area will be considered eligile for free-floating carsharing.");
         map.put(PARAM_SEARCH_DISTANCE, "Defines the search radius for free-floating vehicles. Default is 500 meters.");
         map.put(PARAM_USE_FREEFLOATING, "Defines if the free-floating carsharing should be a mode alternative. Default is false.");
          
         return map;
     }
	
}
