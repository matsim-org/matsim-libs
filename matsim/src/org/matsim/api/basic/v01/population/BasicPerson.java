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
package org.matsim.api.basic.v01.population;

import java.io.Serializable;
import java.util.List;

import org.matsim.api.basic.v01.Id;
import org.matsim.api.basic.v01.Identifiable;

/**
 * @author dgrether
 */
public interface BasicPerson<T extends BasicPlan> extends Identifiable, Serializable {

	public List<? extends T> getPlans();

	public void setId(final Id id);

	public boolean addPlan(final T p);

}