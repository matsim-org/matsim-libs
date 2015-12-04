/* *********************************************************************** *
 * project: org.matsim.*
 * ObjectAttributesBasedElevationProvider.java
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
package eu.eunoiaproject.elevation;

import org.matsim.api.core.v01.Id;
import org.matsim.utils.objectattributes.ObjectAttributes;

import java.util.ArrayList;
import java.util.Collection;

/**
 * @author thibautd
 */
public class ObjectAttributesBasedElevationProvider implements ElevationProvider<Id> {
	private final String attName;
	private final Collection<ObjectAttributes> attributes = new ArrayList<ObjectAttributes>();

	public ObjectAttributesBasedElevationProvider( final ObjectAttributes... attributes ) {
		this( "elevation" , attributes );
	}

	/**
	 * @param attName the name of the altitude attribute in the object attributes containers
	 * @param attributes the object attributes containers in which to search for
	 * altitudes. Several can be provided, in case several types of facilities
	 * are used (for instance activity facilitities and bike sharing stations).
	 * In case several containers are provided and information for a given Id can
	 * be found in more than one, an IllegalStateException will be thrown.
	 */
	public ObjectAttributesBasedElevationProvider(
			final String attName,
			final ObjectAttributes... attributes ) {
		this.attName = attName;
		for ( ObjectAttributes a : attributes ) this.attributes.add( a );
	}

	/**
	 * @throws IllegalStateException if elevation information cannot be found or
	 * is found in more than one ObjectAttributes container.
	 */
	@Override
	public double getAltitude(final Id id) {
		final FailingDouble val = new FailingDouble( id );
		for ( ObjectAttributes atts : attributes ) {
			val.set( (Double) atts.getAttribute( id.toString() , attName ) );
		}
		return val.get();
	}

	private static class FailingDouble {
		private final Id locationId;
		private boolean wasSet;
		private double value = Double.NaN;
		
		public FailingDouble( final Id locationId ) {
			this.locationId = locationId;
		}

		public void set( final Double v ) {
			if ( v == null ) return;
			if ( wasSet ) throw new IllegalStateException( "value found in several containers for ID "+locationId );
			wasSet = true;
			value = v;
		}

		public double get() {
			if ( !wasSet ) throw new IllegalStateException( "no value found for ID "+locationId );
			return value;
		}
	}
}

