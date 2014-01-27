/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

package org.matsim.core.config.experimental;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.experimental.ReflectiveModule.StringGetter;
import org.matsim.core.config.experimental.ReflectiveModule.StringSetter;
import org.matsim.core.utils.geometry.CoordImpl;

public class TestModule extends ReflectiveModule {
	public static final String GROUP_NAME = "testModule";

	// TODO: test for ALL primitive types
	private double doubleField = Double.NaN;

	// Object fields:
	// Id: string representation is toString
	private Id idField = null;
	// Coord: some conversion needed
	private Coord coordField = null;
	// enum: handled especially
	private TestEnum enumField = null;

	public TestModule() {
		super( GROUP_NAME );
	}

	// /////////////////////////////////////////////////////////////////////
	// double field
	@StringGetter( "doubleField" )
	public double getDoubleField() {
		return this.doubleField;
	}

	// there should be no restriction on return type of
	// setters
	@StringSetter( "doubleField" )
	public double setDoubleField(double doubleField) {
		final double old = this.doubleField;
		this.doubleField = doubleField;
		return old;
	}

	// /////////////////////////////////////////////////////////////////////
	// id field
	/**
	 * string representation of Id is result of
	 * toString: just annotate getter
	 */
	@StringGetter( "idField" )
	public Id getIdField() {
		return this.idField;
	}

	public void setIdField(Id idField) {
		this.idField = idField;
	}

	/**
	 * We need to do the conversion from string to Id
	 * ourselves. We do not want the user to access that:
	 * make private.
	 */
	@StringSetter( "idField" )
	private void setIdField(String s) {
		this.idField = new IdImpl( s );
	}

	// /////////////////////////////////////////////////////////////////////
	// coord field
	public Coord getCoordField() {
		return this.coordField;
	}

	public void setCoordField(Coord coordField) {
		this.coordField = coordField;
	}

	// we have to convert both ways here
	@StringGetter( "coordField" )
	private String getCoordFieldString() {
		return this.coordField.getX()+","+this.coordField.getY();
	}

	@StringSetter( "coordField" )
	private void setCoordField(String coordField) {
		final String[] coords = coordField.split( "," );
		if ( coords.length != 2 ) throw new IllegalArgumentException( coordField );

		this.coordField = new CoordImpl(
				Double.parseDouble( coords[ 0 ] ),
				Double.parseDouble( coords[ 1 ] ) );
	}

	// /////////////////////////////////////////////////////////////////////
	// enum
	@StringGetter( "enumField" )
	public TestEnum getTestEnumField() {
		return this.enumField;
	}

	@StringSetter( "enumField" )
	public void setTestEnumField(final TestEnum enumField) {
		this.enumField = enumField;
	}
}
