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
		this.idField = Id.create( s, Link.class );
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
	// enum: normal getter and setter suffice
	@StringGetter( "enumField" )
	public MyEnum getTestEnumField() {
		return this.enumField;
	}

	@StringSetter( "enumField" )
	public void setTestEnumField(final MyEnum enumField) {
		this.enumField = enumField;
	}
}
