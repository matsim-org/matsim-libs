/* *********************************************************************** *
 * project: org.matsim.*
 * ActivityBlackList.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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
package playground.thibautd.planomat.basic;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import playground.thibautd.planomat.api.ActivityWhiteList;

/**
 * @author thibautd
 */
public class ActivityBlackList implements ActivityWhiteList {
	private final Set<String> blackList = new HashSet<String>();

	/**
	 * Creates an empty list
	 */
	public ActivityBlackList() {}

	/**
	 * Create a list containing the specified elements
	 * @param blackList the elements to add to the list
	 */
	public ActivityBlackList(
			final Collection<String> blackList) {
		this.blackList.addAll( blackList );
	}

	@Override
	public boolean isModifiableType(final String type) {
		return !blackList.contains( type );
	}

	/**
	 * Adds a type to the list
	 * @param type the type to forbid
	 */
	public void addType(final String type) {
		blackList.add( type );
	}

	/**
	 * Removes a type from the list
	 * @param type the type not to allow anymore
	 * @return true if a element was actually removed, false otherwise
	 */
	public boolean removeType(final String type) {
		return blackList.remove( type );
	}

	/**
	 * Gives acces to an immutable view of the list.
	 * @return an immutable view of the set of allowed types
	 */
	public Collection<String> getList() {
		return Collections.unmodifiableSet( blackList );
	}

	/**
	 * Removes all elements.
	 */
	public void clear() {
		blackList.clear();
	}
}

