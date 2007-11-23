/* *********************************************************************** *
 * project: org.matsim.*
 * IdI.java
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

package org.matsim.utils.identifiers;

/**
 * Represents a unique identifier.
 */
public interface IdI {

    /**
     * This function must return a unique, non-<code>null</code> <code>String</code>
     * representation of this identifier. For more verbose stuff,
     * <code>toString()</code> should be used.
     *
     * @return a unique, non-<code>null</code> <code>String</code>-representation
     *         of this identifier
     */
    public String toString();

}