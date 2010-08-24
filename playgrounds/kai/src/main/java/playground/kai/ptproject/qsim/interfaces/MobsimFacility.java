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
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.mobsim.framework.PersonAgent;
import org.matsim.vehicles.Vehicle;

public class MobsimFacility implements Updateable {
	final private static Logger log = Logger.getLogger( MobsimFacility.class ) ;
	
	MobsimLink mobsimLink = null ; // dummy.  Pointer cheaper than id!
	Mobsim mobsim = null ; // dummy

	/** data structure for parking needs to be searchable by vehicle id */
	private Map<Id,Vehicle> parking = new TreeMap<Id,Vehicle>() ;

	/** data structure for activities needs to be sorted by departure time */
	private Queue<PersonAgent> agentsAtActivities = new PriorityQueue<PersonAgent>(1, new DepartureTimeComparator() ) ;

	// buses somehow via the "feature" --???
	
	/** Plain "add" of a person, normally during initialization */
	void addPerson( PersonAgent person ) {
		agentsAtActivities.add( person ) ;
	}

	/**Receives the occupied vehicle.  In this situation, it contains at least a driver, and possibly passengers. */
	void addOccupiedVehicle( MobsimVehicle veh ) {}

	/**Receives an empty vehicle.  Normally during initialization. */
	void addEmptyVehicle( Vehicle veh ) {
		parking.put( veh.getId(), veh ) ;
	}

	public void update() {
		PersonAgent person = agentsAtActivities.peek();
		if ( person.getDepartureTime() <= now() ) {
			agentsAtActivities.remove();
			person.activityEnds( now() ) ;
			// call departure handler
			// how does the departure handler get access to the vehicle?
			// or to the bus stop?
			if ( person.getCurrentLeg().getMode().equals( TransportMode.car )) {
				Id vehId = new IdImpl("13") ; // dummy
				MobsimVehicle veh = (MobsimVehicle) parking.get( vehId ) ;
				veh.setDriver( person ) ;
				mobsimLink.addVehicleFromFacilityNormal(veh) ;
			} else if ( person.getCurrentLeg().getMode().equals( TransportMode.pt ) ) {
				// transit wait queue ... where?
				// Departure Handler ???
			} else {
				// teleport.  In principle, I could teleport directly to the destination facility ...
				// ... but I can't get the FacilityId.  And maybe we don't want that???
			}
		}
	}

	static double nextDepartureTime( Person pp ) {
		return 0. ; // dummy
	}
	static double now() {
		return 0. ;
	}

	private static class DepartureTimeComparator implements Comparator<PersonAgent> {
		@Override
		public int compare(PersonAgent o1, PersonAgent o2) {
			return 0 ; // dummy
		}

	}

}
