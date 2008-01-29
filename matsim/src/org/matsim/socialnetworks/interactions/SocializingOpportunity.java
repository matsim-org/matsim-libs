/* *********************************************************************** *
 * project: org.matsim.*
 * SocializingOpportunity.java
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

package org.matsim.socialnetworks.interactions;

import java.util.HashMap;
import java.util.Vector;

import org.matsim.facilities.Activity;
import org.matsim.gbl.Gbl;
import org.matsim.plans.Act;
import org.matsim.plans.Person;

/**
 * SocializingOpportunity is an object encapsulating an activity type, a
 * particular facility (building), a group of people attending or frequenting,
 * and the timing of the opportunity.
 * @author jhackney, fmarchal
 *
 */
public class SocializingOpportunity {

    Activity activity;

    Vector<Person> attendees = new Vector<Person>();
    HashMap<Person,Double> arrivalTimes = new HashMap<Person,Double>();
    HashMap<Person,Double> departureTimes = new HashMap<Person,Double>();

    public SocializingOpportunity( Activity a ){	
	this.activity = a;
    }

    public Vector<Person> getAttendees(){
	return attendees;
    }

    public void addAttendee(Person person) {
	if(!attendees.contains(person)){
    	attendees.add( person );
	}
	Act myAct = person.getKnowledge().map.getAct(this.activity);
	arrivalTimes.put(person,myAct.getStartTime());
	departureTimes.put(person,myAct.getEndTime());
    }

    public double getArrivalTime(Person person){
	return arrivalTimes.get(person);
    }

    public double getDepartureTime(Person person){
	return departureTimes.get(person);
    }

    public Person getRandomInterlocutor( Person p1 ){
	int size = attendees.size();
	if( size == 1 )
	    Gbl.errorMsg( new Exception("SocialEvent with only one lonely person"));// JH should this be error?
	Person p2 = attendees.get( Gbl.random.nextInt( size ) );
	while( p1.equals(p2))
	    p2 = attendees.get( Gbl.random.nextInt( size ) );
	return p2;
    }

    public Vector<Person> getAttendeesInTimeWindow(Person p1, double StartTime, double EndTime){
	Vector<Person> peopleCopresent=new Vector<Person>();
	int size = attendees.size();
	for(int i=0; i<size;i++){
	    Person p2=attendees.get(i);
	    if(this.getArrivalTime(p2)<=StartTime && this.getDepartureTime(p2)>=EndTime && !p1.equals(p2)){
		if(p2!=null){		peopleCopresent.add(p2);}
	    }
	}
	return peopleCopresent;
    }
    
    public Person getRandomInterlocutorInTimeWindow(Person p1, double StartTime, double EndTime){
	Vector<Person> peopleCopresent=getAttendeesInTimeWindow(p1, StartTime, EndTime);
	Person p2=null;
	if(peopleCopresent!=null){
	int size = peopleCopresent.size();
	p2 = peopleCopresent.get( Gbl.random.nextInt( size ) );
	while( p1.equals(p2))
	    p2 = peopleCopresent.get( Gbl.random.nextInt( size ) );
	}
	return p2;
    }    

}
