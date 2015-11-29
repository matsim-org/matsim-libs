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

import eu.eunoiaproject.bikesharing.framework.scenario.BikeSharingConfigGroup;
import eu.eunoiaproject.bikesharing.framework.scenario.BikeSharingFacilities;
import eu.eunoiaproject.bikesharing.framework.scenario.BikeSharingFacility;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.collections.MapUtils;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Class responsible for adding and removing bikes from bike sharing facilities,
 * and notifying anybody who is interested.
 * @author thibautd
 */
public class BikeSharingManagerImpl implements BikeSharingManager {
	private final Map<Id, MutableStatefulBikeSharingFacility> facilities;
	private final Map<Id, List<MutableStatefulBikeSharingFacility>> facilitiesAtLinks;
	private final CompositeListener listener = new CompositeListener();

	public BikeSharingManagerImpl(
			final BikeSharingConfigGroup config,
			final BikeSharingFacilities input ) {
		final Map<Id, MutableStatefulBikeSharingFacility> map =
			new LinkedHashMap<Id, MutableStatefulBikeSharingFacility>();
		this.facilities = Collections.unmodifiableMap( map );

		final Map<Id, List<MutableStatefulBikeSharingFacility>> linkMap =
			new LinkedHashMap<Id, List<MutableStatefulBikeSharingFacility>>();
		this.facilitiesAtLinks = Collections.unmodifiableMap( linkMap );

		// this is used when sampling bikes. As the number of bikes per stations
		// is rather small, rounding by below for all stations results in a dramatic
		// loss in capacity: for instance, with 100 stations with 15 bikes, sampling
		// 10% results in an overall number of bikes of 100, instead of the ideal 150.
		// To cope with this, numbers are rounded up or down randomly, with a probability
		// proportional to their decimal part.
		// The RNG is always initialized with the same seed, so that number of bikes
		// and capacity of stations is stable between iterations.
		final Random random = new Random( 1234 );
		for ( BikeSharingFacility f : input.getFacilities().values() ) {
			final MutableStatefulBikeSharingFacility facility =
				new MutableStatefulBikeSharingFacility(
						random.nextDouble(),
						config,
						f );
			map.put( f.getId() , facility );
			MapUtils.getList(
					f.getLinkId(),
					linkMap ).add(
						facility );
		}
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
		final MutableStatefulBikeSharingFacility f = facilities.get( facility );
		f.takeBikes( amount );
		listener.handleChange( f );
	}

	@Override
	public void putBike( final Id facility ) {
		putBikes( facility , 1 );
	}

	@Override
	public void putBikes( final Id facility , final int amount ) {
		final MutableStatefulBikeSharingFacility f = facilities.get( facility );
		f.putBikes( amount );
		listener.handleChange( f );
	}
}

class MutableStatefulBikeSharingFacility implements StatefulBikeSharingFacility {
	private final BikeSharingFacility facility;

	private int numberOfBikes;
	private final int capacity;

	public MutableStatefulBikeSharingFacility(
			final double random,
			final BikeSharingConfigGroup config,
			final BikeSharingFacility facility) {
		this.facility = facility;
		this.numberOfBikes =
			round(
				facility.getInitialNumberOfBikes(),
				config.getInitialBikesRate(),
				random );
		this.capacity =
			round(
				facility.getCapacity(),
				config.getCapacityRate(),
				random );
		if ( numberOfBikes < 0 ) throw new IllegalArgumentException( "negative initial number of bikes "+numberOfBikes );
	}

	private static int round(
			final int value,
			final double rate,
			final double random) {
		if ( rate == 1 ) return value;
		
		final double rated = rate * value;
	
		final int integral = (int) rated;
		final double decimal = rated - integral;

		return random < decimal ? integral : integral + 1;
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

