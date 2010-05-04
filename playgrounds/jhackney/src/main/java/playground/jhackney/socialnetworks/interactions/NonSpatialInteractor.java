/* *********************************************************************** *
 * project: org.matsim.*
 * NonSpatialInteractor.java
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

import java.util.Arrays;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.knowledges.Knowledges;

import playground.jhackney.SocNetConfigGroup;
import playground.jhackney.socialnetworks.socialnet.SocialNetEdge;
import playground.jhackney.socialnetworks.socialnet.SocialNetwork;


/**
 * This Interactor lets agents exchange knowledge.
 *
 * These are wrappers for the person knowledge exchange methods
 *
 * @author J.Hackney
 */
public class NonSpatialInteractor{
	private final Logger log = Logger.getLogger(NonSpatialInteractor.class);
	SocialNetwork net;
	Object links[];

	double proportionOfLinksToActivate;// [0.0,1.0]
	double fract_intro;//[0.0,1.0]

	int numInteractionsPerLink;// [0.0,1.0]

	PersonExchangeKnowledge pxk; // the actual workhorse

	public NonSpatialInteractor(SocialNetwork snet, Knowledges knowledges, SocNetConfigGroup snConfig) {
		this.net=snet;

		pxk = new PersonExchangeKnowledge(net, knowledges);
		proportionOfLinksToActivate = Double.parseDouble(snConfig.getFractNSInteract());
		numInteractionsPerLink = Integer.parseInt(snConfig.getSocNetNSInteractions());
		fract_intro=Double.parseDouble(snConfig.getFriendIntroProb());
	}

	public void exchangeGeographicKnowledge(String facType, int iteration) {
		this.log.info("  |Exchanging knowledge about "+facType+" activity");


		links = net.getLinks().toArray();
		final List<Object> list = Arrays.asList( links );


		java.util.Collections.shuffle(list, MatsimRandom.getRandom());
		links=list.toArray();

		int numPicks = (int) (proportionOfLinksToActivate * links.length);

//		Pick a random link
		for(int i=0;i<numPicks;i++){
//			int linkno = MatsimRandom.random.nextInt(this.links.length);
//			SocialNetEdge mySocialLink = (SocialNetEdge) links[linkno];
			SocialNetEdge mySocialLink = (SocialNetEdge) links[i];

//			Interact numInteractions times if chosen link is to be activated
			for (int k = 0; k < numInteractionsPerLink; k++) {

//				Exchange random knowledge if that is the algorithm to be used in interaction
				pxk.exchangeRandomFacilityKnowledge(mySocialLink, facType);

//				Else insert other interactions here
//				pxk.otherCoolInteraction(net,curLink, XXXX);
			}
		}
	}
	/**
	 * This interact method exchanges knowledge about a person in Ego's egonet with one
	 * of his friends: C is B's friend. B is A's friend. B "tells" C and A about each other.
	 * Closes triangles. It might be strange for people to say they know each other without
	 * having met face to face. However don't take it so literally. This model is an abstraction
	 * (consider these to be trips not observed). It is the same mechanism as in JinGirNew,
	 * which however uses a more direct and efficient "friends introducing friends" algorithm.
	 * Without persons being introduced to persons via friends of friends in this
	 * manner, it is not certain that enough triangles would form by spatial meeting. Essentially
	 * we introduce people and THEN see if the relationship can be supported by the geography.
	 * If not (i.e. if the people do not visit each other any more), the relationship disappears
	 * after a time, anyway.
	 *
	 * @author jhackney
	 * @param iteration
	 */
	public void exchangeSocialNetKnowledge(int iteration) {

		if(!(fract_intro>0)){
			this.log.info("No friends introduced");
			return;
		}

		this.log.info("FOF algorithm for Iteration "+iteration+". "+fract_intro*100.+"% of links will be tested.");
		this.log.info(" For each there is the opportunity to close "+numInteractionsPerLink+" triangles.");

		links = net.getLinks().toArray();
		final List<Object> list = Arrays.asList( links );
		java.util.Collections.shuffle(list, MatsimRandom.getRandom());
		links=list.toArray();


		int numPicks = (int) (fract_intro * links.length);

		for(int i=0;i<numPicks;i++){

//			Get a random social link
//			int rndInt1 = MatsimRandom.random.nextInt(Integer.MAX_VALUE);
//			int linkno = rndInt1 % links.length;
//			SocialNetEdge myLink = (SocialNetEdge) links[linkno];
			SocialNetEdge myLink = (SocialNetEdge) links[i];

			for (int j = 0; j < numInteractionsPerLink; j++) {

//				Random exchange of alters: note, results in adding to social network!
				pxk.randomlyIntroduceBtoCviaA(myLink, iteration);

//				Could be replaced with other means of choosing friends to introduce
//				pxk.introduceCoolestNFriends(net,myLink,iteration);
			}
		}
	}
}
