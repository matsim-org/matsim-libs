/* *********************************************************************** *
 * project: org.matsim.*
 * IdentifiableCollectionsUtils.java
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
package org.matsim.contrib.socnetsim.utils;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Identifiable;

import java.util.Collection;

/**
 * @author thibautd
 */
public class IdentifiableCollectionsUtils {

	public static boolean containsAll(
			final Collection<? extends Id> list,
			final Collection<? extends Identifiable> contained) {
		for ( Identifiable id : contained ) {
			if ( !list.contains( id.getId() ) ) return false;
		}
		return true;
	}

	public static boolean contains(
			final Collection<? extends Identifiable> list,
			final Id passengerId) {
		for ( Identifiable e : list ) {
			if ( e.getId().equals( passengerId ) ) return true;
		}
		return false;
	}

	public static void addAll(
			final Collection<Id> collectionToFill,
			final Collection<? extends Identifiable> passengers) {
		for ( Identifiable p : passengers ) collectionToFill.add( p.getId() );
	}


}

