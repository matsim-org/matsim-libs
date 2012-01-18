/* *********************************************************************** *
 * project: org.matsim.*
 * ActivityWhiteListImpl.java
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
 * Default implementation of a white list, which
 * simply stores a set of all modifiable types.
 *
 * @author thibautd
 */
public class ActivityWhiteListImpl implements ActivityWhiteList {
	private final Set<String> whiteList = new HashSet<String>();

	/**
	 * Creates an empty list
	 */
	public ActivityWhiteListImpl() {}

	/**
	 * Create a list containing the specified elements
	 * @param whiteList the elements to add to the list
	 */
	public ActivityWhiteListImpl(
			final Collection<String> whiteList) {
		this.whiteList.addAll( whiteList );
	}

	@Override
	public boolean isModifiableType(final String type) {
		return whiteList.contains( type );
	}

	/**
	 * Adds a type to the list
	 * @param type the type to allow
	 */
	public void addType(final String type) {
		whiteList.add( type );
	}

	/**
	 * Removes a type from the list
	 * @param type the type not to allow anymore
	 * @return true if a element was actually removed, false otherwise
	 */
	public boolean removeType(final String type) {
		return whiteList.remove( type );
	}

	/**
	 * Gives acces to an immutable view of the list.
	 * @return an immutable view of the set of allowed types
	 */
	public Collection<String> getList() {
		return Collections.unmodifiableSet( whiteList );
	}

	/**
	 * Removes all elements.
	 */
	public void clear() {
		whiteList.clear();
	}
}

