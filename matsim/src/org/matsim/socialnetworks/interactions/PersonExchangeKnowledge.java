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

import org.matsim.facilities.Activity;
import org.matsim.gbl.Gbl;
import org.matsim.plans.Act;
import org.matsim.plans.Knowledge;
import org.matsim.plans.Person;
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
		List<Activity> act2List = k2.getActivities(facType);
		if(act2List.size()>=1){
			Activity activity2=act2List.get(Gbl.random.nextInt( act2List.size()));
			k1.map.addActivity(activity2);
		}

//		If person2 has an edge pointed toward person1, let p1 share information with p2
		if(p2.getKnowledge().egoNet.knows(p1)){
			List<Activity> act1List = k1.getActivities(facType);
			if(act1List.size()>=1){
				Activity activity1=act1List.get(Gbl.random.nextInt( act1List.size()));
				k2.map.addActivity(activity1);
			}
		}
	}

	/**
	 * This method is exactly the same as Fabrice's
	 * and the method in the old "knowledge"-based version.
	 * @param myLink
	 * @param iteration
	 */  
	public void randomlyIntroduceBtoCviaA(SocialNetEdge myLink, int iteration) {

		Person myEgo = myLink.getPersonFrom();
		Knowledge k0 = myEgo.getKnowledge();

		Person friend1 = k0.egoNet.getRandomPerson(myEgo);
		Person friend2 = k0.egoNet.getRandomPerson(myEgo);
		if ((friend1 != null) && (friend2 != null)) {

			net.makeSocialContact(friend1, friend2, iteration, "fof");
		}
	}
}
