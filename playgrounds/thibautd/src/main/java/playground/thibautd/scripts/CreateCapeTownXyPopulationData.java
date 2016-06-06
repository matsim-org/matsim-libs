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
package playground.thibautd.scripts;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.households.Household;
import org.matsim.households.Income;
import org.matsim.utils.objectattributes.ObjectAttributes;
import playground.ivt.utils.MoreIOUtils;
import playground.thibautd.utils.PopulationToCsvConverter;

import java.util.HashMap;
import java.util.Map;

import static playground.ivt.router.TripSoftCache.LocationType.coord;

/**
 * @author thibautd
 */
public class CreateCapeTownXyPopulationData {
	public static void main( String[] args ) {
		final String configFile = args[ 0 ];
		final String outputXy = args[ 1 ];

		MoreIOUtils.checkFile( outputXy );

		final Config config = ConfigUtils.loadConfig( configFile );

		final Scenario sc = ScenarioUtils.loadScenario( config );
		final ObjectAttributes attributes = sc.getPopulation().getPersonAttributes();

		final Map<Id<Person>, Id<Household>> person2household = new HashMap<>();
		for ( Household hh : sc.getHouseholds().getHouseholds().values() ) {
			for ( Id<Person> member : hh.getMemberIds() ) {
				person2household.put( member , hh.getId() );
			}
		}

		PopulationToCsvConverter.convert(
				sc.getPopulation(),
				outputXy,
				p -> {
					final Map<String,String> map = new HashMap<>();
					map.put( "ID" , p.getId().toString() );

					final Activity home = (Activity)
							p.getSelectedPlan().getPlanElements().stream()
								.filter( (pe) -> (pe instanceof Activity ) )
								.filter( (pe) -> ((Activity) pe).getType().equals( "h" ) )
								.findFirst()
								.get();

					map.put( "X" , ""+home.getCoord().getX() );
					map.put( "Y" , ""+home.getCoord().getY() );

					final Id<Household> hh = person2household.get( p.getId() );
					map.put( "HH_SIZE" ,
							""+sc.getHouseholds().getHouseholdAttributes().getAttribute(
									hh.toString(),
									"householdSize") );
					final Income income = sc.getHouseholds().getHouseholds().get( hh ).getIncome();
					map.put( "HH_INCOME",
							""+( income != null ? income.getIncome() : "-1" ) );

					map.put( "EMPLOYMENT", (String)
							attributes.getAttribute(
									p.getId().toString(),
									"employment" ) );

					map.put( "GENDER", (String)
							attributes.getAttribute(
									p.getId().toString(),
									"gender" ) );

					map.put( "BIRTH", (String)
							attributes.getAttribute(
									p.getId().toString(),
									"yearOfBirth" ) );

					return map;
				} );
	}

}
