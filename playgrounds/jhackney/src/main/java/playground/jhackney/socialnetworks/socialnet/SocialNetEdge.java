/* *********************************************************************** *
 * project: org.matsim.*
 * SocialNetEdge.java
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

package playground.jhackney.socialnetworks.socialnet;

import org.matsim.api.core.v01.population.Person;

public class SocialNetEdge implements Comparable<SocialNetEdge>{

    private Person person1, person2;
    private int timeMade;
    private int timeLastUsed;
    private int timesMet=1;
    double strength=1.0;
    private String type;

    public SocialNetEdge(Person a1, Person a2) {

	person1 = a1;
	person2 = a2;
    }
    public Person getPersonFrom(){
	return person1;
    }
    public Person getPersonTo(){
	return person2;
    }
    public void setTimeMade(int i){
	this.timeMade=i;
    }
    public int getTimeMade(){
	return this.timeMade;
    }
    public void setTimeLastUsed(int i){
	this.timeLastUsed=i;
    }
    public int getTimeLastUsed(){
	return this.timeLastUsed;
    }
    public double getStrength() {
	return strength;
    }
    public void setStrength(double strength) {
	this.strength = strength;
    }
    public void setType(String type){
	this.type=type;
    }
    public String getType(){
	return type;
    }
    public void setNumberOfTimesMet(int timesMet){
    	this.timesMet=timesMet;
    }
    public void incrementNumberOfTimesMet(){
	timesMet++;
    }
    public int getTimesMet(){
	return timesMet;
    }
	public int compareTo(SocialNetEdge e) {
		// TODO Auto-generated method stub
		double eFrom=new Double(e.getPersonFrom().getId().toString()).doubleValue();
		double eTo=new Double(e.getPersonTo().getId().toString()).doubleValue();
		double thisFrom=new Double(this.getPersonFrom().getId().toString()).doubleValue();
		double thisTo=new Double(this.getPersonTo().getId().toString()).doubleValue();
		double mult=10000000.;
		if((eFrom*mult+eTo) > (thisFrom*mult+thisTo)){
			return 1;
		}else if((eFrom*mult+eTo) < (thisFrom*mult+thisTo)){
			return -1;
		}else{
		return 0;
		}
	}
    
}
