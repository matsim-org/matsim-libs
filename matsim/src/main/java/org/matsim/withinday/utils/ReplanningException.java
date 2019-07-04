
/* *********************************************************************** *
 * project: org.matsim.*
 * ReplanningException.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2019 by the members listed in the COPYING,        *
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

 package org.matsim.withinday.utils;

/**
 * For example when activity to be replanned is in the past.  So that BDI code can catch this separately from other exceptions
 * and possibly react to it.
 * 
 * @author kainagel
 */
public class ReplanningException extends RuntimeException {
	public ReplanningException() {
		super() ;
	}
	public ReplanningException( String msg ) {
		super( msg ) ;
	}
}