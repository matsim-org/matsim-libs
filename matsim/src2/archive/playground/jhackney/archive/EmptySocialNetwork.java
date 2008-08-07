/* *********************************************************************** *
 * project: org.matsim.*
 * EmptySocialNetwork.java
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

import org.matsim.population.Population;

import playground.jhackney.socialnet.SocialNetwork;

public class EmptySocialNetwork extends SocialNetwork{

    Object[] personList;

    int numPersons;

    int numLinks;

    public EmptySocialNetwork(Population plans) {
	super(plans);
	setupIter=1;

	personList = plans.getPersons().values().toArray();
	numPersons = personList.length;
	numLinks = 0;
    }

    public void generateLinks(int iteration) {
	int countMultiples = 0; // counts attempts made to initiate multiple

	System.out.println(" " + this.getClass() + " " + countMultiples
		+ " links were prevented from being added multiple times.");
	System.out.println(" " + this.getClass()
		+ ": kbar, numPersons, numLinks = ["
		+ (2. * (numLinks - countMultiples) / numPersons)
		+ ", " + numPersons + ", " + (numLinks - countMultiples) + "]");
    }

    public void removeLinks() {
	// TODO Auto-generated method stub

    }


}
