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

package playground.fabrice.secondloc.socialnet;

import org.matsim.plans.Person;

public class SocialNetEdge {

	Person personFrom, personTo;
	int creationTime;
	int lastActivationTime;
	double strength=1.0;

	public SocialNetEdge( Person a1, Person a2){

		personFrom = a1;
		personTo = a2;
	}
	public Person getPersonFrom(){
		return personFrom;
	}
	public Person getPersonTo(){
		return personTo;
	}
	public int getCreationTime() {
		return creationTime;
	}
	public void setCreationTime(int creationTime) {
		this.creationTime = creationTime;
	}
	public int getLastActivationTime() {
		return lastActivationTime;
	}
	public void setLastActivationTime(int lastActivationTime) {
		this.lastActivationTime = lastActivationTime;
	}
	public double getStrength() {
		return strength;
	}
	public void setStrength(double strength) {
		this.strength = strength;
	}
}
