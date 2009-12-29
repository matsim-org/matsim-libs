/* *********************************************************************** *
 * project: org.matsim.*
 * RandomSocialNetwork.java
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

package playground.jhackney.deprecated;

import org.matsim.gbl.Gbl;
import org.matsim.population.Person;
import org.matsim.population.Population;

import playground.jhackney.socialnet.SocialNetwork;

/*
 * Constructs a random social network.
 * Parameters: kbar = average degree, numPersons = number of nodes.
 * No spatial dimension possible in this algorithm.
 * Undirected Erd�s/R�nyi random graph. Dorogovtsev and Mendes 2003.
 * To make this algorithm DIRECTED, double the number of links.
 */
public class RandomSocialNetwork extends SocialNetwork {
    // kbar is the average degree of the random graph.
    // It should be a config parameter, not hard-coded
    int kbar; // get from config

    Object[] personList;

    int numPersons;

    int numLinks;
    
    public RandomSocialNetwork(Population plans) {
	super(plans);
	setupIter=1;
	kbar = Integer.parseInt(Gbl.getConfig().socnetmodule().getSocNetKbar());
	System.out.println(" Links the Persons together in UNDIRECTED Erd�s/R�nyi random graph. Dorogovtsev and Mendes 2003.");
	personList = plans.getPersons().values().toArray();
	numPersons = personList.length;
	numLinks = (int) ((kbar * numPersons) / 2.);

	System.out.println(" kbar, numPersons, numLinks approximately= [" + kbar + ", " + numPersons + ", " + numLinks
		+ "]");
    }

    public void generateLinks(int iteration) {
	for (int i = 0; i < numLinks; i++) {
	    Person person1 = (Person) personList[Gbl.random.nextInt(personList.length)];
	    Person person2 = (Person) personList[Gbl.random.nextInt(personList.length)];
	    this.makeSocialContact(person1, person2, iteration);
//	    if(UNDIRECTED){
//	    this.generateUndirectedLink(person1, person2, iteration);
//	    }else this.generateDirectedLink(person1,person2, iteration);
	}
    }

    public void removeLinks() {
	// TODO Auto-generated method stub
	
    }
 }
