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
package org.matsim.basic.v01;

/**
*
* @author dgrether
*
*/

public interface BasicPopulation<T extends BasicPerson> {

	public String getName();
	
	public void setName(String name);
	
	public void addPerson(T person) throws Exception;

	public T getPerson(Id personId);

	public T getPerson(String personId);

	public void setRefLayer(String layer);
	
	public String getRefLayer();

}