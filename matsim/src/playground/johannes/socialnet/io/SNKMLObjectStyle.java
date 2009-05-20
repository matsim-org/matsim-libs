/* *********************************************************************** *
 * project: org.matsim.*
 * SNKMLStyles.java
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

/**
 * 
 */
package playground.johannes.socialnet.io;

import java.util.List;

import net.opengis.kml._2.StyleType;

import org.matsim.api.basic.v01.population.BasicPerson;

import playground.johannes.socialnet.SocialNetwork;

/**
 * @author illenberger
 *
 */
public interface SNKMLObjectStyle<T, P extends BasicPerson<?>>{

	public List<StyleType> getObjectStyle(SocialNetwork<P> socialnet);
	
	public String getObjectSytleId(T object);
	
}
