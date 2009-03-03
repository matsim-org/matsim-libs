/* *********************************************************************** *
 * project: org.matsim.*
 * PersonExchangeKnowledge.java
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

import java.util.List;

import org.matsim.gbl.MatsimRandom;
import org.matsim.interfaces.core.v01.ActivityOption;
import org.matsim.interfaces.core.v01.Person;
import org.matsim.population.Knowledge;
import org.matsim.socialnetworks.socialnet.SocialNetEdge;
import org.matsim.socialnetworks.socialnet.SocialNetwork;


public class PersonExchangeKnowledge {
	SocialNetwork net;

	public PersonExchangeKnowledge(SocialNetwork snet) {
		this.net = snet;

	}
	/**
	 * This method lets agents exchange random knowledge about a Place, if they know each other.
	 * The direction of the social connection, as recorded in the EgoNet, is decisive for
	 * enabling this exchange. Be careful how you define the direction of links when you
	 * construct the network (SocialNetwork class)
	 * 
	 * @param curLink
	 * @param facType
	 */
	public void exchangeRandomFacilityKnowledge(SocialNetEdge curLink, String facType){
//		Pay attention to your definition of the direction of arrows in the social network!
		Person p2 = curLink.getPersonTo();
		Person p1 = curLink.getPersonFrom();

		Knowledge k1 = p1.getKnowledge();
		Knowledge k2 = p2.getKnowledge();

//		Get a random facility (activity--> facility)
		//from Person 2's knowledge and add it to Person 1's
		List<ActivityOption> act2List = k2.getActivities(facType);
		if(act2List.size()>=1){
			ActivityOption activity2=act2List.get(MatsimRandom.random.nextInt( act2List.size()));
			k1.getMentalMap().addActivity(activity2);
		}

//		If person2 has an edge pointed toward person1, let p1 share information with p2
		if(p2.getKnowledge().getEgoNet().knows(p1)){
			List<ActivityOption> act1List = k1.getActivities(facType);
			if(act1List.size()>=1){
				ActivityOption activity1=act1List.get(MatsimRandom.random.nextInt( act1List.size()));
				k2.getMentalMap().addActivity(activity1);
			}
		}
	}

	/**
	 * Takes a Link from p1 to p2. Introduces a friend of p1's to p2
	 * if one exists.
	 * 
	 * @param myLink
	 * @param iteration
	 */  
	public void randomlyIntroduceBtoCviaA(SocialNetEdge myLink, int iteration) {

		Person p1 = myLink.getPersonFrom();
		Person p2 = myLink.getPersonTo();
		Knowledge k1 = p1.getKnowledge();

		Person newFriend = k1.getEgoNet().getRandomPerson();
		if ((newFriend != null) && (p2 != null)) {
			net.makeSocialContact(newFriend, p2, iteration, "fof");
		}
	}
}
