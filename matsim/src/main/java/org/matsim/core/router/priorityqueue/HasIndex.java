/* *********************************************************************** *
 * project: org.matsim.*
 * HasIndex.java
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

package org.matsim.core.router.priorityqueue;

/**
 * An interface to mark classes that enumerate their objects. Each index
 * should be unique and can e.g. be used to lookup values in an array. This feature
 * is used in some classed due to performance reasons since a lookup in an array
 * is much faster than in a map.
 *
 * @see BinaryMinHeap
 *
 * @author cdobler
 */
@Deprecated // Id.index() should be used instead nowadays.
public interface HasIndex {

	public int getArrayIndex();
}
