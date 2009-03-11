/* *********************************************************************** *
 * project: org.matsim.*
 * BasicAct.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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
package org.matsim.interfaces.basic.v01;

import java.util.List;

/**
 *
* @author dgrether
*
*/
public interface BasicPerson<T extends BasicPlan> extends Identifiable {

//	TODO [kai]: Would make more sense to be to have something like "getAttributes" and "getPlans".  Current version seems a bit
//	over-specified to me.  kai, feb09

	public void addPlan(final T plan);

	public List<T> getPlans();

	public void setId(final Id id);

}