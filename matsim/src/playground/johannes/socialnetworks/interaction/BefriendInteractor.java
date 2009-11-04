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

import org.matsim.api.core.v01.population.Person;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.listener.IterationStartsListener;

import playground.johannes.socialnetworks.graph.social.Ego;
import playground.johannes.socialnetworks.graph.social.SocialNetwork;
import playground.johannes.socialnetworks.graph.social.SocialNetworkBuilder;
import playground.johannes.socialnetworks.graph.social.SocialTie;

/**
 * @author illenberger
 *
 */
public class BefriendInteractor implements Interactor, IterationStartsListener {

	private SocialNetwork<Person> socialnet;
	
	private SocialNetworkBuilder<Person> builder;
	
	private double tieProba;
	
	private Random random;
	
	private int currentIteration;
	
	public BefriendInteractor(SocialNetwork<Person> socialnet, SocialNetworkBuilder<Person> builder, double p, long randomSeed) {
		this.socialnet = socialnet;
		this.builder = builder;
		this.tieProba = p;
		random = new Random(randomSeed);
	}
	
	public void interact(Person p1, Person p2, double startTime, double endTime) {
		Ego<Person> e1 = socialnet.getEgo(p1);
		Ego<Person> e2 = socialnet.getEgo(p2);

		SocialTie tie = socialnet.getEdge(e1, e2);
		if(tie == null) {
			/*
			 * Create tie...
			 */
			if(random.nextDouble() <= tieProba)
				tie = builder.addEdge(socialnet, e1, e2);
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
