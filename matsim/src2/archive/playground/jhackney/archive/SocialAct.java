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
import org.matsim.population.Act;
import org.matsim.population.Person;

/**
 * SocialAct is an object encompassing an activity type, a
 * particular facility (building), a group of people intending to attend or frequent,
 * and the intended timing of the opportunity.
 * Note that it has attributes of acts and activities
 * 
 * A SocialEvent should be written encompassing an activity type,
 * a particular facility (building), the group of people actually attending or frequenting,
 * and the actual timing of the opportunity.
 * This would have attributes of events and activities.
 * 
 * 
 * @author jhackney, fmarchal
 *
 */
public class SocialAct {

	Activity activity;

	Vector<Person> attendees = new Vector<Person>();
	HashMap<Person,Double> arrivalTimes = new HashMap<Person,Double>();
	HashMap<Person,Double> departureTimes = new HashMap<Person,Double>();

	public SocialAct( Activity a ){	
		this.activity = a;
	}

	public Vector<Person> getAttendees(){
		return attendees;
	}

	public void addAttendee(Person person) {
		if(!attendees.contains(person)){
			attendees.add( person );
		}
//		Act myAct = person.getKnowledge().getMentalMap().getAct(this.activity);

		Act myAct = person.getKnowledge().getMentalMap().getActFromActivity(person, this.activity);

		if(myAct==null){
			System.out.println("SocialAct: !!!Act is null");
			System.out.println(person.getId()+" "+this.activity.getType()+ " "+this.activity.getFacility().getId());
		}
		arrivalTimes.put(person,myAct.getStartTime());
		departureTimes.put(person,myAct.getEndTime());
	}

	public double getArrivalTime(Person person){
		return arrivalTimes.get(person);
	}

	public double getDepartureTime(Person person){
		return departureTimes.get(person);
	}

	public Person getRandomAttendee( Person p1 ){
		int size = attendees.size();
		if( size == 1 )
			Gbl.errorMsg( new Exception("SocialEvent with only one lonely person"));// JH should this be error?
		Person p2 = attendees.get( Gbl.random.nextInt( size ) );
		while( p1.equals(p2))
			p2 = attendees.get( Gbl.random.nextInt( size ) );
		return p2;
	}

	public Vector<Person> getAttendeesInTimeWindow(Person p1, double startTime, double endTime){
		Vector<Person> peopleCopresent=new Vector<Person>();
		int size = attendees.size();
		for(int i=0; i<size;i++){
			Person p2=attendees.get(i);
			if(this.getDepartureTime(p2)>=startTime && this.getArrivalTime(p2)<=endTime && !p1.equals(p2)){
				if(p2!=null){		peopleCopresent.add(p2);}
			}
		}
		return peopleCopresent;
	}

	public Person getRandomAttendeeInTimeWindow(Person p1, double startTime, double endTime){
		Vector<Person> peopleCopresent=getAttendeesInTimeWindow(p1, startTime, endTime);
		Person p2=null;
		if(peopleCopresent!=null && peopleCopresent.size()>0){
			int size = peopleCopresent.size();
			p2 = peopleCopresent.get( Gbl.random.nextInt( size ) );
			while( p1.equals(p2))
				p2 = peopleCopresent.get( Gbl.random.nextInt( size ) );
		}
		return p2;
	}

	public Person getAttendeeInTimeWindow(Person p1, double startTime,
			double endTime, int i) {
		// 
		Vector<Person> peopleCopresent=getAttendeesInTimeWindow(p1, startTime, endTime);
		Person p2=null;
		if(peopleCopresent!=null && peopleCopresent.size()>0){
			p2 = peopleCopresent.get( i );
		}
		return p2;
	}    

}
