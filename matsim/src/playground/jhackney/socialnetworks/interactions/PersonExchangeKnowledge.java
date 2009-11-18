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

package playground.jhackney.socialnetworks.interactions;

import java.util.List;

import org.matsim.api.core.v01.population.Person;
import org.matsim.core.facilities.ActivityOptionImpl;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.knowledges.KnowledgeImpl;
import org.matsim.knowledges.Knowledges;

import playground.jhackney.socialnetworks.mentalmap.MentalMap;
import playground.jhackney.socialnetworks.socialnet.EgoNet;
import playground.jhackney.socialnetworks.socialnet.SocialNetEdge;
import playground.jhackney.socialnetworks.socialnet.SocialNetwork;


public class PersonExchangeKnowledge {
	SocialNetwork net;
	private Knowledges knowledges;

	public PersonExchangeKnowledge(SocialNetwork snet, Knowledges kn) {
		this.net = snet;
		this.knowledges = kn;

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

		KnowledgeImpl k1 = this.knowledges.getKnowledgesByPersonId().get(p1.getId());
		KnowledgeImpl k2 = this.knowledges.getKnowledgesByPersonId().get(p2.getId());

//		Get a random facility (activity--> facility)
		//from Person 2's knowledge and add it to Person 1's
		List<ActivityOptionImpl> act2List = k2.getActivities(facType);
		if(act2List.size()>=1){
			ActivityOptionImpl activity2=act2List.get(MatsimRandom.getRandom().nextInt( act2List.size()));
			((MentalMap)p1.getCustomAttributes().get(MentalMap.NAME)).addActivity(activity2);
		}

//		If person2 has an edge pointed toward person1, let p1 share information with p2
		if(((EgoNet)p2.getCustomAttributes().get(EgoNet.NAME)).knows(p1)){
			List<ActivityOptionImpl> act1List = k1.getActivities(facType);
			if(act1List.size()>=1){
				ActivityOptionImpl activity1=act1List.get(MatsimRandom.getRandom().nextInt( act1List.size()));
				((MentalMap)p2.getCustomAttributes().get(MentalMap.NAME)).addActivity(activity1);
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

		Person newFriend = ((EgoNet)p1.getCustomAttributes().get(EgoNet.NAME)).getRandomPerson();
		if ((newFriend != null) && (p2 != null)) {
			net.makeSocialContact(newFriend, p2, iteration, "fof");
		}
	}
}
