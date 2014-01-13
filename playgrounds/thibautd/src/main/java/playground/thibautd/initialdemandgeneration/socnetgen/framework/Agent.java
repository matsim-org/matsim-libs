/* *********************************************************************** *
 * project: org.matsim.*
 * Agent.java
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
package playground.thibautd.initialdemandgeneration.socnetgen.framework;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Identifiable;

/**
 * Represents an agent with id and (socio-demographic) caracteristics
 * @author thibautd
 */
public class Agent implements Identifiable {
	private final Id id;
	private final Map<String, Object> attributes = new HashMap<String, Object>();

	public Agent(final Id id) {
		this.id = id;
	}

	@Override
	public Id getId() {
		return id;
	}

	public void setAttribute(final String name, final Object value) {
		if ( value == null ) throw new NullPointerException();
		final Object old = attributes.put( name , value );
		if ( old != null ) {
			throw new IllegalStateException( "attribute "+name
					+" already had value "+old+" for agent "+id
					+": cannot set new value "+value );
		}
	}

	public <T extends Object> T getAttribute(final String name, final Class<? extends T> klass) {
		final Object value = attributes.get( name );

		if ( value == null ) {
			throw new IllegalStateException( "attribute "+name+" does not exist for agent "+id );
		}

		try{
			return klass.cast( value );
		}
		catch (ClassCastException e) {
			throw new RuntimeException( "attribute "+name+" for agent "+id
					+" is of type "+value.getClass()+" not "+klass , e );
		}
	}
}

