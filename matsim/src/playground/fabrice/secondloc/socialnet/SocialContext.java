/* *********************************************************************** *
 * project: org.matsim.*
 * SocialContext.java
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

import java.util.LinkedList;

import org.matsim.gbl.Gbl;
import org.matsim.plans.Person;

public class SocialContext {

	LinkedList<SocialNetEdge> socialConnections = new LinkedList<SocialNetEdge> ();

	public Person getRandomPerson( Person me ) {
		int size = socialConnections.size();
		if( size == 0 )
			return null;
		SocialNetEdge edge = socialConnections.get( Gbl.random.nextInt(size));
		if( edge.getPersonFrom().equals(me) )
			return edge.getPersonTo();
		return edge.getPersonFrom();
	}
	
	public void addConnection( SocialNetEdge edge ){
		socialConnections.add( edge );
	}
	
	public boolean knows( Person person ){
		// return true if person is already part of our social context
		for( SocialNetEdge edge : socialConnections ){
			if( edge.getPersonFrom().equals(person) )
				return true;
			if( edge.getPersonTo().equals(person))
				return true;
		}
		return false;
	}

}
