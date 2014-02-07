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
package eu.eunoiaproject.bikesharing.qsim;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;

import eu.eunoiaproject.bikesharing.qsim.BikeSharingManager.BikeSharingManagerListener;
import eu.eunoiaproject.bikesharing.scenario.BikeSharingFacilities;
import eu.eunoiaproject.bikesharing.scenario.BikeSharingFacility;

import playground.thibautd.utils.MapUtils;

/**
 * Class responsible for adding and removing bikes from bike sharing facilities,
 * and notifying anybody who is interested.
 * @author thibautd
 */
public class BikeSharingManager {
	private final Map<Id, MutableStatefulBikeSharingFacility> facilities;
	private final Map<Id, List<MutableStatefulBikeSharingFacility>> facilitiesAtLinks;
	private final CompositeListener listener = new CompositeListener();

	public BikeSharingManager( final BikeSharingFacilities input ) {
		final Map<Id, MutableStatefulBikeSharingFacility> map =
			new LinkedHashMap<Id, MutableStatefulBikeSharingFacility>();
		this.facilities = Collections.unmodifiableMap( map );

		final Map<Id, List<MutableStatefulBikeSharingFacility>> linkMap =
			new LinkedHashMap<Id, List<MutableStatefulBikeSharingFacility>>();
		this.facilitiesAtLinks = Collections.unmodifiableMap( linkMap );


		for ( BikeSharingFacility f : input.getFacilities().values() ) {
			final MutableStatefulBikeSharingFacility facility =
				new MutableStatefulBikeSharingFacility( f );
			map.put( f.getId() , facility );
			MapUtils.getList(
					f.getLinkId(),
					linkMap ).add(
						facility );
		}
	}

	public void addListener( final BikeSharingManagerListener l ) {
		listener.addListener( l );
	}

	public Map<Id, ? extends StatefulBikeSharingFacility> getFacilities() {
		return facilities;
	}

	public Map<Id, ? extends Collection< ? extends StatefulBikeSharingFacility >> getFacilitiesAtLinks() {
		return facilitiesAtLinks;
	}

	public void takeBike( final Id facility ) {
		takeBikes( facility , 1 );
	}

	public void takeBikes( final Id facility , final int amount ) {
		final MutableStatefulBikeSharingFacility f = facilities.get( facility );
		f.takeBikes( amount );
		listener.handleChange( f );
	}

	public void putBike( final Id facility ) {
		putBikes( facility , 1 );
	}

	public void putBikes( final Id facility , final int amount ) {
		final MutableStatefulBikeSharingFacility f = facilities.get( facility );
		f.putBikes( amount );
		listener.handleChange( f );
	}

	public static interface BikeSharingManagerListener {
		public void handleChange( StatefulBikeSharingFacility facilityInNewState );
	}
}

class CompositeListener implements BikeSharingManagerListener {
	private final List<BikeSharingManagerListener> listeners = new ArrayList<BikeSharingManagerListener>();

	public void addListener( final BikeSharingManagerListener l ) {
		listeners.add( l );
	}

	@Override
	public void handleChange(final StatefulBikeSharingFacility f) {
		for ( BikeSharingManagerListener l : listeners ) l.handleChange( f );
	}

}

class MutableStatefulBikeSharingFacility implements StatefulBikeSharingFacility {
	private final BikeSharingFacility facility;

	private int numberOfBikes;

	public MutableStatefulBikeSharingFacility(
			final BikeSharingFacility facility) {
		this.facility = facility;
		this.numberOfBikes = facility.getInitialNumberOfBikes();
		if ( numberOfBikes < 0 ) throw new IllegalArgumentException( "negative initial number of bikes "+numberOfBikes );
	}

	@Override
	public boolean hasBikes() {
		return numberOfBikes > 0;
	}

	@Override
	public int getNumberOfBikes() {
		return numberOfBikes;
	}

	public void takeBikes(final int amount) {
		if ( amount < 0 ) throw new IllegalArgumentException( "negative amount "+amount );
		if ( amount > numberOfBikes ) throw new IllegalArgumentException( "cannot take "+amount+" bikes from station with "+numberOfBikes+" bikes." );
		numberOfBikes -= amount;
		assert numberOfBikes >= 0;
	}

	public void putBikes(final int amount) {
		if ( amount < 0 ) throw new IllegalArgumentException( "negative amount "+amount );
		if ( amount > getCapacity() - numberOfBikes ) throw new IllegalArgumentException( "cannot put "+amount+" bikes in station with "+numberOfBikes+" out of "+getCapacity()+" bikes." );
		numberOfBikes += amount;
		assert numberOfBikes <= getCapacity();
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
		return facility.getCapacity();
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

