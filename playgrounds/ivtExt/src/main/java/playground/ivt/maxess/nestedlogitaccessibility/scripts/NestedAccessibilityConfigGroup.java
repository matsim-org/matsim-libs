/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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
package playground.ivt.maxess.nestedlogitaccessibility.scripts;

import org.matsim.core.config.ReflectiveConfigGroup;

/**
 * @author thibautd
 */
public class NestedAccessibilityConfigGroup extends ReflectiveConfigGroup {
	public static final String GROUP_NAME = "nestedAccessibility";

	private String activityType = "leisure";
	private int choiceSetSize = 200;
	private int distanceBudget = 20 * 1000;

	public NestedAccessibilityConfigGroup( ) {
		super( GROUP_NAME );
	}

	@StringGetter( "activityType" )
	public String getActivityType() {
		return activityType;
	}

	@StringSetter( "activityType" )
	public void setActivityType( final String activityType ) {
		this.activityType = activityType;
	}

	@StringGetter( "choiceSetSize" )
	public int getChoiceSetSize() {
		return choiceSetSize;
	}

	@StringSetter( "choiceSetSize" )
	public void setChoiceSetSize( final int choiceSetSize ) {
		this.choiceSetSize = choiceSetSize;
	}

	@StringGetter( "distanceBudget" )
	public int getDistanceBudget() {
		return distanceBudget;
	}

	@StringSetter( "distanceBudget" )
	public void setDistanceBudget( final int distanceBudget ) {
		this.distanceBudget = distanceBudget;
	}
}
