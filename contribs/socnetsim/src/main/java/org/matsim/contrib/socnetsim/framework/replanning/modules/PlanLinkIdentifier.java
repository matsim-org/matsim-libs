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

package org.matsim.contrib.socnetsim.framework.replanning.modules;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.matsim.api.core.v01.population.Plan;

import com.google.inject.BindingAnnotation;

public interface PlanLinkIdentifier {
	@Retention( RetentionPolicy.RUNTIME )
	@BindingAnnotation
	@Target( { ElementType.FIELD, ElementType.LOCAL_VARIABLE, ElementType.PARAMETER } )
	@interface Strong {}

	@Retention( RetentionPolicy.RUNTIME )
	@BindingAnnotation
	@Target( { ElementType.FIELD, ElementType.LOCAL_VARIABLE, ElementType.PARAMETER } )
	@interface Weak {}


	boolean areLinked( Plan p1, Plan p2 );
}
