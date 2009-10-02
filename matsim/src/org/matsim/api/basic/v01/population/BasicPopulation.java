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
import java.util.Map;

import org.matsim.api.basic.v01.Id;
import org.matsim.core.api.internal.MatsimToplevelContainer;

/**
* @author dgrether
*/
public interface BasicPopulation<T extends BasicPerson> extends MatsimToplevelContainer, Serializable {

	public String getName();
	
	public void setName(String name);

	public Map<Id,? extends T> getPersons();
	
	public void addPerson(final T p); 
	// yyyy resurrected.  move to BasicPopulation once certain that this is a good idea  

	/** @deprecated use getBuilder() */
	public BasicPopulationFactory getPopulationBuilder();
	
	public BasicPopulationFactory getFactory() ;

}