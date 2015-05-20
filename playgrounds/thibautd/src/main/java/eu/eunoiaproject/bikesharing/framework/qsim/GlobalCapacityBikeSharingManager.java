/* *********************************************************************** *
 * project: org.matsim.*
 * BikeSharingManager.java
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
package eu.eunoiaproject.bikesharing.framework.qsim;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;

import eu.eunoiaproject.bikesharing.framework.scenario.BikeSharingConfigGroup;
import eu.eunoiaproject.bikesharing.framework.scenario.BikeSharingFacilities;
import eu.eunoiaproject.bikesharing.framework.scenario.BikeSharingFacility;

import org.matsim.core.utils.collections.MapUtils;

/**
 * A {@link BikeSharingManager} meant to simulate the "best case" relocation strategy,
 * where available slots and bikes are defined system-wide: assume the operator is able
 * to relocate bikes fast enough to satisfy everybody. Reality should lie between this and
 * the case without any relocation.
 * 
 * @author thibautd
 */
public class GlobalCapacityBikeSharingManager implements BikeSharingManager {
	private final Map<Id, DummyStatefulBikeSharingFacility> facilities;
	private final Map<Id, List<DummyStatefulBikeSharingFacility>> facilitiesAtLinks;
	private final CompositeListener listener = new CompositeListener();
	
	private int capacity = 0;
	private int bikesAtStations = 0;

	public GlobalCapacityBikeSharingManager(
			final BikeSharingConfigGroup config,
			final BikeSharingFacilities input ) {
		final Map<Id, DummyStatefulBikeSharingFacility> map =
			new LinkedHashMap<Id, DummyStatefulBikeSharingFacility>();
		this.facilities = Collections.unmodifiableMap( map );

		final Map<Id, List<DummyStatefulBikeSharingFacility>> linkMap =
			new LinkedHashMap<Id, List<DummyStatefulBikeSharingFacility>>();
		this.facilitiesAtLinks = Collections.unmodifiableMap( linkMap );


		for ( BikeSharingFacility f : input.getFacilities().values() ) {
			final DummyStatefulBikeSharingFacility facility =
				new DummyStatefulBikeSharingFacility( f );
			map.put( f.getId() , facility );
			MapUtils.getList(
					f.getLinkId(),
					linkMap ).add(
						facility );
		}

		// XXX this is not equivalent to the number of bikes in the "detailed"
		// version!
		// However, this does look better (less rounding error)...
		this.capacity *= config.getCapacityRate();
		this.bikesAtStations *= config.getInitialBikesRate();
	}

	@Override
	public void addListener( final BikeSharingManagerListener l ) {
		listener.addListener( l );
	}

	@Override
	public Map<Id, ? extends StatefulBikeSharingFacility> getFacilities() {
		return facilities;
	}

	@Override
	public Map<Id, ? extends Collection< ? extends StatefulBikeSharingFacility >> getFacilitiesAtLinks() {
		return facilitiesAtLinks;
	}

	@Override
	public void takeBike( final Id facility ) {
		takeBikes( facility , 1 );
	}

	@Override
	public void takeBikes( final Id facility , final int amount ) {
		final DummyStatefulBikeSharingFacility f = facilities.get( facility );
		f.takeBikes( amount );
		listener.handleChange( f );
	}

	@Override
	public void putBike( final Id facility ) {
		putBikes( facility , 1 );
	}

	@Override
	public void putBikes( final Id facility , final int amount ) {
		final DummyStatefulBikeSharingFacility f = facilities.get( facility );
		f.putBikes( amount );
		listener.handleChange( f );
	}

	private class DummyStatefulBikeSharingFacility implements StatefulBikeSharingFacility {
		private final BikeSharingFacility facility;
	
		public DummyStatefulBikeSharingFacility(
				final BikeSharingFacility facility) {
			this.facility = facility;
			
			capacity += facility.getCapacity();
			bikesAtStations += facility.getInitialNumberOfBikes();
		}
	
		@Override
		public boolean hasBikes() {
			return bikesAtStations > 0;
		}
	
		@Override
		public int getNumberOfBikes() {
			return bikesAtStations;
		}
	
		public void takeBikes(final int amount) {
			if ( amount < 0 ) throw new IllegalArgumentException( "negative amount "+amount );
			if ( amount > bikesAtStations ) throw new IllegalArgumentException( "cannot take "+amount+" bikes from station with "+bikesAtStations+" bikes." );
			bikesAtStations -= amount;
			assert bikesAtStations >= 0;
		}
	
		public void putBikes(final int amount) {
			if ( amount < 0 ) throw new IllegalArgumentException( "negative amount "+amount );
			if ( amount > getCapacity() - bikesAtStations ) throw new IllegalArgumentException( "cannot put "+amount+" bikes in station with "+bikesAtStations+" out of "+getCapacity()+" bikes." );
			bikesAtStations += amount;
			assert bikesAtStations <= getCapacity();
		}
	
		// /////////////////////////////////////////////////////////////////////////
		// delegate
		@Override
		public Id getId() {
			return facility.getId();
		}
	
		@Override
		public Coord getCoord() {
			return facility.getCoord();
		}
	
		@Override
		public int getCapacity() {
			return capacity;
		}
	
		@Override
		public int getInitialNumberOfBikes() {
			return facility.getInitialNumberOfBikes();
		}
	
		@Override
		public Id getLinkId() {
			return facility.getLinkId();
		}
	
		@Override
		public Map<String, Object> getCustomAttributes() {
			throw new UnsupportedOperationException();
		}
	}
	
}
