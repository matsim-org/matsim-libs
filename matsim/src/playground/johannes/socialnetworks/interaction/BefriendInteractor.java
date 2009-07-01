/* *********************************************************************** *
 * project: org.matsim.*
 * BefriendInteractor.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

/**
 * 
 */
package playground.johannes.socialnetworks.interaction;

import java.util.Random;

import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.population.PersonImpl;

import playground.johannes.socialnetworks.graph.social.Ego;
import playground.johannes.socialnetworks.graph.social.SocialNetwork;
import playground.johannes.socialnetworks.graph.social.SocialTie;

/**
 * @author illenberger
 *
 */
public class BefriendInteractor implements Interactor, IterationStartsListener {

	private SocialNetwork<PersonImpl> socialnet;
	
	private double tieProba;
	
	private Random random;
	
	private int currentIteration;
	
	public BefriendInteractor(SocialNetwork<PersonImpl> socialnet, double p, long randomSeed) {
		this.socialnet = socialnet;
		this.tieProba = p;
		random = new Random(randomSeed);
	}
	
	public void interact(PersonImpl p1, PersonImpl p2, double startTime, double endTime) {
		Ego<PersonImpl> e1 = socialnet.getEgo(p1);
		Ego<PersonImpl> e2 = socialnet.getEgo(p2);

		SocialTie tie = socialnet.getEdge(e1, e2);
		if(tie == null) {
			/*
			 * Create tie...
			 */
			if(random.nextDouble() <= tieProba)
				tie = socialnet.addEdge(e1, e2);
		} else {
			/*
			 * Reinforce tie...
			 */
			tie.use(currentIteration);
		}
	}

	public void notifyIterationStarts(IterationStartsEvent event) {
		currentIteration = event.getIteration();
	}

}
