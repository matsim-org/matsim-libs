/* *********************************************************************** *
 * project: org.matsim.*
 * Interactions.java
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

package playground.fabrice.secondloc;

import java.util.Collection;

import org.matsim.plans.Knowledge;
import org.matsim.plans.Person;
import org.matsim.plans.Plans;

import playground.fabrice.secondloc.socialnet.SocialEvent;
import playground.fabrice.secondloc.socialnet.SocialNetEdge;
import playground.fabrice.secondloc.socialnet.SocialNetz;

public class Interactions {

	public SocialNetz socnet;
	
	public Interactions( SocialNetz net ){
		this.socnet = net;
	}
	
	// Persons which are socially connected
	// exchange a piece of spatial information
	
	public void exchangeDualSpatialInformation( double probability ){
		for( SocialNetEdge edge : socnet.getLinks() ){
			if( probability < 1.0 )
				if( Math.random() > probability )
					continue;
			
			// the From person learns from the To person
			Knowledge k1 = edge.getPersonFrom().getKnowledge();
			Knowledge k2 = edge.getPersonTo().getKnowledge();
			
			CoolPlace cp1 = k2.map.getRandomCoolPlace();
			k1.hacks.learn( cp1 );
			
			if( socnet.isDirected())
				continue;			
			
			// the To person learns from the From person
			CoolPlace cp2 = k1.map.getRandomCoolPlace();
			k2.hacks.learn( cp2 );	
			
		}	
	}
	
	// Persons which are socially connected
	// exchange a piece of social information:
	// formation of triads: A and B become friends 
	// because they both know C
	
	public void makeFriendOfFriend( Plans plans, double probability ){	
		
		for( Person person : plans.getPersons().values() ){
			
			if( probability < 1.0 )
				if( Math.random() > probability )
					continue;
			
			Knowledge k0 = person.getKnowledge();
			Person friend1 = k0.context.getRandomPerson( person );
			Person friend2 = k0.context.getRandomPerson( person );
			if( (friend1==null) || (friend2==null) )
				continue;
			
			socnet.createSocialConnection( friend1, friend2 );
		}
	}
	
	// Persons which attend social events have
	// a chance to become acquainted
	
	public void makeFriendAtSocialEvent( Collection<SocialEvent> events, double probability ){
		for( SocialEvent event : events ){		
			if( event.getAttendees().size() > 1)
				for( Person person1 : event.getAttendees() ){

					if( probability < 1.0 )
						if( Math.random() > probability )
							continue;

					Person person2 = event.getRandomInterlocutor( person1 );

					socnet.createSocialConnection(person1, person2);

				}
		}
	}
}
