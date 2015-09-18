/* *********************************************************************** *
 * project: org.matsim.*
 * RelevantCoordinates.java
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
package playground.thibautd.parknride.herbiespecific;

import org.matsim.api.core.v01.Coord;

/**
 * Defines coordinates of some landmarks in Zurich. Taken from the PT stop facilities.
 *
 * @author thibautd
 */
public class RelevantCoordinates {
	private RelevantCoordinates() {}

	public final static Coord HAUPTBAHNHOF = new Coord(683146.0, 247872.0);
	public final static Coord HARDBRUECKE = new Coord(681436.3, 248826.4);
	public final static Coord STADELHOFEN = new Coord(683841.6, 246706.6);
	public final static Coord SEEBACH = new Coord(683867.8, 252911.3);
	public final static Coord OERLIKON = new Coord(683446.7, 251749.7);
}
