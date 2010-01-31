/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package playground.kai.ptproject.qsim.interfaces;

import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.Queue;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Person;
import org.matsim.ptproject.qsim.DriverAgent;
import org.matsim.ptproject.qsim.PersonAgentI;

public class MobsimActivityFacility {
	final private static Logger log = Logger.getLogger( MobsimActivityFacility.class ) ; 
	
	private Queue<PersonAgentI> internalQueue = new PriorityQueue<PersonAgentI>(1, new DepartureTimeComparator() ) ;
	private Parking parking = null ;
	private BusStop busStop = null ;
	
	/**Adding a person, normally to do an activity. */
	void addPerson( PersonAgentI person ) {
		internalQueue.add( person ) ;
	}
	
	void selfUpdate() {
		PersonAgentI person = internalQueue.peek();
	    if ( person.getDepartureTime() <= now() ) {
	        internalQueue.remove();
	        if ( /* mode==car */ Math.random()<0.5 ) {
	        	parking.addDriver( (DriverAgent)person ) ;
	        } else if ( /* mode==pt */ Math.random()<0.5 ) {
	        	busStop.addPerson( person ) ;
	        } else /* teleport */ {
//	        	teleportation.addPerson( person ) ;
	        }
	    }
	}
	
	static double nextDepartureTime( Person pp ) {
		return 0. ; // dummy
	}
	static double now() {
		return 0. ;
	}
	private static class DepartureTimeComparator implements Comparator<PersonAgentI> {
		@Override
		public int compare(PersonAgentI o1, PersonAgentI o2) {
			if ( o1.getDepartureTime() < o2.getDepartureTime() ) {
				return -1 ;
			} else if ( o1.getDepartureTime() > o2.getDepartureTime() ) {
				return 1 ;
			} else if ( o1.getDepartureTime()==o2.getDepartureTime() ) {
				return 0 ;
			} else {
				log.warn( "something weird (departure time could not be compared; maybe NaN?)" ) ;
				return 0 ;
			}
		}
		
	}
}
