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

package org.matsim.api.core.v01;


/**
 * Represents a unique identifier.
 * 
 * Note that Ids should not contain any whitespace characters (spaces, tabs, newlines, ...), 
 * as this may lead to problems when Ids are written to file and read back in. 
 * 
 * This is essentially a c++ typedef, except that
 * typedefs don't exist in Java.
 */
public interface Id extends Comparable<Id> {

}