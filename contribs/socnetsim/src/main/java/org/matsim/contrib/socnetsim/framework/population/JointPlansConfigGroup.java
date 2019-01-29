/* *********************************************************************** *
 * project: org.matsim.*
 * JointPlansConfigGroup.java
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
package org.matsim.contrib.socnetsim.framework.population;

import org.matsim.core.config.ReflectiveConfigGroup;

/**
 * @author thibautd
 */
public class JointPlansConfigGroup extends ReflectiveConfigGroup {
	public final static String GROUP_NAME = "jointPlans";

	public JointPlansConfigGroup() {
		super( GROUP_NAME );
	}

	private String fileName = null;

	@StringSetter( "fileName" )
	public void setFileName( final String fileName ) {
		this.fileName = fileName;
	}

	@StringGetter( "fileName" )
	public String getFileName() {
		return fileName;
	}
}

