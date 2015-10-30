/* *********************************************************************** *
 * project: org.matsim.*
 * Composite.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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
package org.matsim.contrib.common.collections;

import java.util.ArrayList;
import java.util.List;

/**
 * Abstract base class for composite object structures.
 * 
 * @author illenberger
 * 
 */
public abstract class Composite<T> {

	protected List<T> components = new ArrayList<T>();

	/**
	 * Adds a component to the composite.
	 * 
	 * @param component
	 *            a component.
	 */
	public void addComponent(T component) {
		components.add(component);
	}

	/**
	 * Removes a component from the composite.
	 * 
	 * @param component
	 *            a component.
	 */
	public void removeComponent(T component) {
		components.remove(component);
	}
}
