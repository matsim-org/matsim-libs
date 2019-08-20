
/* *********************************************************************** *
 * project: org.matsim.*
 * EnumConverter.java
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

 package org.matsim.utils.objectattributes.attributeconverters;

import org.matsim.utils.objectattributes.AttributeConverter;

/**
 * {@link AttributeConverter} for enum types.
 * @author thibautd
 */
public class EnumConverter<E extends Enum<E>> implements AttributeConverter<E> {
	private final Class<E> clazz;

	public EnumConverter( final Class<E> clazz ) {
		this.clazz = clazz;
	}

	@Override
	public E convert( final String value ) {
		return Enum.valueOf( clazz , value );
	}

	@Override
	public String convertToString( final Object o ) {
		if (o.getClass() != clazz) throw new IllegalArgumentException( "got "+o.getClass().getCanonicalName()+", expected "+clazz.getCanonicalName() );
		return ((Enum) o).name();
	}
}
