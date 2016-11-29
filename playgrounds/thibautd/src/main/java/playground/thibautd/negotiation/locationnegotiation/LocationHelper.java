/* *********************************************************************** *
 * project: org.matsim.*
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
package playground.thibautd.negotiation.locationnegotiation;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;

/**
 * @author thibautd
 */
@Singleton
public class LocationHelper {
	private final Population population;
	private final ActivityFacilities facilities;
	private static final String HOME = "home";

	@Inject
	public LocationHelper( final Population population, final ActivityFacilities facilities ) {
		this.population = population;
		this.facilities = facilities;
	}

	public ActivityFacility getHomeLocation( final Person person ) {
		// use custom attributes instead of customizable, to avoid writing this to file.
		// could be replaced by a map if needed.
		ActivityFacility facility = (ActivityFacility) person.getCustomAttributes().get( "home location" );
		if ( facility != null ) return facility;

		final Activity homeActivity =
				person.getSelectedPlan().getPlanElements().stream()
						.filter( pe -> pe instanceof Activity )
						.map( pe -> (Activity) pe )
						// TODO make type configurable
						//.filter( a -> a.getType().equals( HOME ) )
						.findFirst()
						.orElseThrow( () -> new RuntimeException( "could not find home of "+person ) );

		facility = facilities.getFacilities().get( homeActivity.getFacilityId() );

		if ( facility == null ) {
			// need to create a dummy facility. happens for instance in the CH Scenario...
			facility = facilities.getFactory().createActivityFacility(
					homeActivity.getFacilityId(),
					homeActivity.getCoord(),
					homeActivity.getLinkId() );
		}

		person.getCustomAttributes().put( "home location" , facility );
		return facility;
	}

	public ActivityFacility getHomeLocation( final Id<Person> id ) {
		return getHomeLocation( population.getPersons().get( id ) );
	}
}
