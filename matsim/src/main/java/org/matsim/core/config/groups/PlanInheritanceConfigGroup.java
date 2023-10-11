/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

import java.util.Map;

import org.matsim.core.config.ReflectiveConfigGroup;

/**
 * @author awagner
 */
public final class PlanInheritanceConfigGroup extends ReflectiveConfigGroup {

	public static final String GROUP_NAME = "planInheritance";

	private static final String ENABLED = "enabled";

	private boolean enabled = false;

	public PlanInheritanceConfigGroup() {
		super(GROUP_NAME);
	}

	@Override
	public Map<String, String> getComments() {
		Map<String, String> comments = super.getComments();
		comments.put(ENABLED, "Specifies whether or not PlanInheritance Information should be tracked.");
		return comments;
	}

	
	@StringSetter( ENABLED )
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}
	
	
	@StringGetter( ENABLED )
	public boolean getEnabled() {
		return this.enabled;
	}
}
