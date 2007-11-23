/* *********************************************************************** *
 * project: org.matsim.*
 * SocialEvent.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package playground.fabrice.secondloc.socialnet;

import java.util.Collection;
import java.util.Vector;

import org.matsim.gbl.Gbl;
import org.matsim.plans.Person;

import playground.fabrice.secondloc.CoolPlace;

public class SocialEvent {

	CoolPlace place;
	
	Vector<Person> attendees = new Vector<Person>();
	
	double probabilityToMeet;
	
	public SocialEvent( CoolPlace place ){
		this.place = place;
	}
	
	public Collection<Person> getAttendees(){
		return attendees;
	}
	
	public double getProbabilityToMeet(){
		return probabilityToMeet;
	}

	public void addAttendee(Person person) {
		attendees.add( person );
	}
	
	public Person getRandomInterlocutor( Person p1 ){
		int size = attendees.size();
		if( size == 1 )
			Gbl.errorMsg( new Exception("SocialEvent with only one lonely person"));
		Person p2 = attendees.get( Gbl.random.nextInt( size ) );
		while( p1.equals(p2))
			p2 = attendees.get( Gbl.random.nextInt( size ) );
		return p2;
	}
}
