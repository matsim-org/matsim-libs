/* *********************************************************************** *
 * project: kai
 * PersWirtVConvert.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package playground.kai.convert;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.api.internal.MatsimWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.utils.geometry.transformations.GeotoolsTransformation;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Time;

import playground.kai.urbansim.Utils;

/**
 * @author nagel
 *
 */
public class PersWirtVConvert {
	
	public static void main(String[] args) {
		
		Config config = ConfigUtils.createConfig() ;
		Scenario sc = org.matsim.core.scenario.ScenarioUtils.createScenario(config) ;
		Population pop = sc.getPopulation() ;
		
		try {
//			BufferedReader reader = IOUtils.getBufferedReader( "/Users/nagel/shared-svn/projects/personenwirtschaftsverkehr-bln/short.csv" );
			BufferedReader reader = IOUtils.getBufferedReader( "/Users/nagel/shared-svn/projects/personenwirtschaftsverkehr-bln/trips_all_attributes.csv" );

			String line = reader.readLine();
			Map<String,Integer> idxFromKey = Utils.createIdxFromKey( line, "," ) ;

			Id lastId = null ;
			Person newPerson = null ;
			Plan newPlan = null ;
			Activity act = null ;
			long cnt = 0 ;
			while ( (line=reader.readLine()) != null ) {
				String[] parts = line.split("[,\t\n]+");

				Coord fromCoord = createCoordFromPOINT(sc, parts[idxFromKey.get("\"source_geom\"")]);
				Coord toCoord = createCoordFromPOINT(sc, parts[idxFromKey.get("\"dest_geom\"")]);
				
				Id<Person> personId = Id.create( parts[idxFromKey.get("\"logbook_id\"")], Person.class ) ;
				if ( personId != lastId ) {
					lastId = personId ;
					newPerson = pop.getFactory().createPerson(personId) ;
					cnt++ ;
					if ( cnt%100==0 ) {
						pop.addPerson(newPerson) ; // not consistent with immutable object
					}
					
					newPlan = pop.getFactory().createPlan() ;
					newPerson.addPlan(newPlan) ; // not consistent with immutable object
					
					int sourceTypeIdx = Integer.parseInt( parts[idxFromKey.get("\"sourcetype\"")] ) ;
					String sourceType = locationTypeAsString(sourceTypeIdx);
					act = pop.getFactory().createActivityFromCoord("@"+sourceType, fromCoord ) ;
					newPlan.addActivity(act) ;
				}
				double tripStartTime = Time.parseTime( parts[idxFromKey.get("\"start_time\"")].replace('"', ' ').trim() ) ;
				if ( tripStartTime != Time.UNDEFINED_TIME ) {
					act.setEndTime( tripStartTime ) ;
					// trip start time is activity end time
					// this is either the first or the previous activity.  Last activity gets no end time.
				} else {
					act.setMaximumDuration(0.) ;
				}
				
				Leg leg = pop.getFactory().createLeg("car") ;
				newPlan.addLeg(leg) ;
				
				int purposeIdx         = Integer.parseInt( parts[idxFromKey.get("\"purpose\"")] ) ;
				String purpose = null ;
				switch ( purposeIdx ) {
				case 1 : purpose = "comm. pickup/deliv. of goods" ; break ;
				case 2 : purpose = "comm. activity" ; break ;
				case 3 : purpose = "comm. pickup/deliv. of persons" ; break ;
				case 4 : purpose = "comm. support activity" ; break ;
				case 5 : purpose = "return to base" ; break ;
				case 6 : purpose = "work" ; break ;
				case 7 : purpose = "education" ; break ;
				case 8 : purpose = "shopping" ; break ;
				case 9 : purpose = "leisure" ; break ;
				case 10: purpose = "priv. pickup/deliv. of persons" ; break ;
				case 11: purpose = "other priv. act." ; break ;
				case 12: purpose = "home" ; break ;
				case 21: purpose = "comm. unspec." ; break ;
				case 22: purpose = "priv. unspec." ; break ;
				default: purpose = "unknown activity key" ; break ;
				}
				
				int destinationTypeIdx = Integer.parseInt( parts[idxFromKey.get("\"destinationtype\"")] ) ;
				String destinationType = locationTypeAsString(destinationTypeIdx);
				
				act = pop.getFactory().createActivityFromCoord(purpose+"@"+destinationType, toCoord) ;
				newPlan.addActivity(act) ;
				
			}
			
			MatsimWriter popWriter = new PopulationWriter( pop, null ) ;
			popWriter.write("/Users/nagel/tmp/pop-1pct.xml.gz") ;
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		Logger.getLogger("dummy").info("done") ;
	}

	private static String locationTypeAsString(int destinationTypeIdx) {
		String destinationType = null ;
		switch ( destinationTypeIdx ) {
		case 1 : destinationType = "goods interchange location" ; break ;
		case 2 : destinationType = "warehouse" ; break ;
		case 3 : destinationType = "construction site" ; break ;
		case 4 : destinationType = "own company" ; break ;
		case 5 : destinationType = "other company" ; break ;
		case 6 : destinationType = "hh of customer" ; break ;
		case 7 : destinationType = "other comm. loc." ; break ;
		case 8 : destinationType = "priv. loc." ; break ;
		default: destinationType = "unknown location key" ;
		}
		return destinationType;
	}

	private static GeotoolsTransformation transformer = new GeotoolsTransformation("WGS84","DHDN_GK4") ;

	private static Coord createCoordFromPOINT(Scenario sc, String fromPoint) {
		String[] point = fromPoint.split("[() ]") ;
		Coord coord = new Coord(Double.parseDouble(point[1]), Double.parseDouble(point[2]));
		
		
		return transformer.transform(coord) ;
	}

}
