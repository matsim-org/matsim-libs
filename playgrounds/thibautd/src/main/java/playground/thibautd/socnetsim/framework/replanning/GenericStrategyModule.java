/* *********************************************************************** *
 * project: org.matsim.*
 * GenericStrategyModule.java
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
package playground.thibautd.socnetsim.framework.replanning;

import java.util.Collection;

import org.matsim.core.replanning.ReplanningContext;

/**
 * @author thibautd
 */
public interface GenericStrategyModule<T> {
	public void handlePlans( ReplanningContext replanningContext , Collection<T> toHandle );
}

