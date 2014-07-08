/* *********************************************************************** *
 * project: org.matsim.*
 * CachingRoutingModuleWrapper.java
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
package playground.thibautd.router;

import java.util.ArrayList; import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.api.experimental.facilities.Facility;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.router.RoutingModule;
import org.matsim.core.router.StageActivityTypes;

/**
 * @author thibautd
 */
public class CachingRoutingModuleWrapper implements RoutingModule {
	public static enum LocationType {coord, link, facility;}
	private final boolean considerPerson;
	private final LocationType locationType;

	private final Map<Departure, List<? extends PlanElement>> map;
	private RoutingModule wrapped;

	public CachingRoutingModuleWrapper(
			final boolean considerPerson,
			final LocationType locationType,
			final int cacheSize,
			final RoutingModule wrapped) {
		this.considerPerson = considerPerson;
		this.locationType = locationType;
		this.map =
			new LinkedHashMap<Departure, List<? extends PlanElement>>( cacheSize + 1 , 0.75f , true ) {
				private static final long serialVersionUID = 1L;
				@Override
				protected boolean removeEldestEntry(final Map.Entry<Departure, List<? extends PlanElement>> eldest) {
					return size() >= cacheSize;
				}
			};
		this.wrapped = wrapped;
	}

	@Override
	public List<? extends PlanElement> calcRoute(
			final Facility fromFacility,
			final Facility toFacility,
			final double departureTime,
			final Person person) {
		final Departure departure = new Departure( person , fromFacility , toFacility );
		final List<? extends PlanElement> cached = map.get( departure );
		
		if ( cached != null ) return clone( cached );
		final List<? extends PlanElement> trip =
				wrapped.calcRoute(fromFacility, toFacility, departureTime,
				person);
		
		map.put( departure , clone( trip ) );
		
		return trip;
	}

	private static List<? extends PlanElement> clone(List<? extends PlanElement> trip) {
		final List<PlanElement> clone = new ArrayList<PlanElement>( trip.size() );

		for ( PlanElement pe : trip ) {
			if ( pe instanceof Leg ) clone.add( clone( (Leg) pe ) );
			else if ( pe instanceof Activity ) clone.add( clone( (Activity) pe ) );
			else throw new RuntimeException( pe.getClass().getName()+"???" );
		}

		return clone;
	}

	private static Leg clone( final Leg leg) {
		final Leg clone = new LegImpl( leg.getMode() );
		clone.setRoute( leg.getRoute().clone() );
		clone.setDepartureTime( leg.getDepartureTime() );
		clone.setTravelTime( leg.getTravelTime() );
		return clone;
	}

	private static Activity clone( final Activity act) {
		return new ActivityImpl( act );
	}

	@Override
	public StageActivityTypes getStageActivityTypes() {
		return wrapped.getStageActivityTypes();
	}

	private class Departure {
		private final Id personId;
		private final Facility origin;
		private final Facility destination;

		private Departure(
				final Person person,
				final Facility origin,
				final Facility destination) {
			this.personId = person.getId();
			this.origin = origin;
			this.destination = destination;
		}

		@Override
		public boolean equals(final Object o) {
			if ( !o.getClass().equals( getClass() ) ) return false;
			final Departure other = (Departure) o;

			if ( considerPerson && !other.personId.equals( personId ) ) return false;
			if ( !sameLocation( origin , other.origin ) ) return false;
			if ( !sameLocation( destination , other.destination ) ) return false;

			return true;
		}

		private boolean sameLocation(
				final Facility f1,
				final Facility f2) {
			switch ( locationType ) {
			case coord:
				return f1.getCoord().equals( f2.getCoord() );
			case facility:
				return f1.getId().equals( f2.getId() );
			case link:
				return f1.getLinkId().equals( f2.getLinkId() );
			default:
				throw new RuntimeException( ""+locationType );
			}
		}

		@Override
		public int hashCode() {
			int h = 0;
			if ( considerPerson ) h += personId.hashCode();

			switch ( locationType ) {
			case coord:
				h += origin.getCoord().hashCode();
				h += destination.getCoord().hashCode();
				break;
			case facility:
				h += origin.getId().hashCode();
				h += destination.getId().hashCode();
				break;
			case link:
				h += origin.getLinkId().hashCode();
				h += destination.getLinkId().hashCode();
				break;
			default:
				throw new RuntimeException( ""+locationType );
			}

			return h;
		}
	}}

