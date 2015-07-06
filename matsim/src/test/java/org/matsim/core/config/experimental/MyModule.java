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
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.config.ReflectiveConfigGroup;
import org.matsim.core.utils.geometry.CoordImpl;


/**
 * Demonstrate how to use ReflectiveModule to easily create typed config groups.
 * Please do not modify this class: it is used from unit tests!
 */
public class MyModule extends ReflectiveConfigGroup {
	public static final String GROUP_NAME = "testModule";

	// TODO: test for ALL primitive types
	private double doubleField = Double.NaN;

	// Object fields:
	// Id: string representation is toString
	private Id<Link> idField = null;
	// Coord: some conversion needed
	private Coord coordField = null;
	// enum: handled especially
	private MyEnum enumField = null;
	// field without null conversion
	private String nonNull = "some arbitrary default value.";

	public MyModule() {
		super( GROUP_NAME );
	}

	// /////////////////////////////////////////////////////////////////////
	// primitive type field: standard getter and setter suffice
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
	// id field: need for a special setter, normal getter suffice
	/**
	 * string representation of Id is result of
	 * toString: just annotate getter
	 */
	@StringGetter( "idField" )
	public Id<Link> getIdField() {
		return this.idField;
	}

	public void setIdField(Id<Link> idField) {
		this.idField = idField;
	}

	/**
	 * We need to do the conversion from string to Id
	 * ourselves.
	 * the annotated setter can be private to avoid polluting the
	 * interface: the user just sees the "typed" setter.
	 */
	@StringSetter( "idField" )
	private void setIdField(String s) {
		// Null handling needs to be done manually if conversion "by hand"
		this.idField = s == null ? null : Id.create( s, Link.class );
	}

	// /////////////////////////////////////////////////////////////////////
	// coord field: need for special getter and setter
	public Coord getCoordField() {
		return this.coordField;
	}

	public void setCoordField(Coord coordField) {
		this.coordField = coordField;
	}

	// we have to convert both ways here.
	// the annotated getter and setter can be private to avoid polluting the
	// interface: the user just sees the "typed" getter and setter.
	@StringGetter( "coordField" )
	private String getCoordFieldString() {
		// Null handling needs to be done manually if conversion "by hand"
		// Note that one *needs" to return a null pointer, not the "null"
		// String, which is reserved word.
		return this.coordField == null ? null : this.coordField.getX()+","+this.coordField.getY();
	}

	@StringSetter( "coordField" )
	private void setCoordField(String coordField) {
		if ( coordField == null ) {
			// Null handling needs to be done manually if conversion "by hand"
			this.coordField = null;
			return;
		}

		final String[] coords = coordField.split( "," );
		if ( coords.length != 2 ) throw new IllegalArgumentException( coordField );

		this.coordField = new CoordImpl(
				Double.parseDouble( coords[ 0 ] ),
				Double.parseDouble( coords[ 1 ] ) );
	}

	// /////////////////////////////////////////////////////////////////////////
	// Non-null string: standard setter and getter
	@StringGetter( "nonNullField" )
	@DoNotConvertNull
	public String getNonNull() {
		return nonNull;
	}

	@StringSetter( "nonNullField" )
	@DoNotConvertNull
	public void setNonNull( String nonNull ) {
		// in case the setter is called from user code, we need to check for nullity ourselves.
		if ( nonNull == null ) throw new IllegalArgumentException();
		this.nonNull = nonNull;
	}

	// XXX for test, should not appear in tutorial
	void setNonNullToNull( ) {
		this.nonNull = null;
	}

	// /////////////////////////////////////////////////////////////////////
	// enum: normal getter and setter suffice
	@StringGetter( "enumField" )
	public MyEnum getTestEnumField() {
		return this.enumField;
	}

	@StringSetter( "enumField" )
	public void setTestEnumField(final MyEnum enumField) {
		// no need to test for null: the parent class does it for us
		this.enumField = enumField;
	}
}
