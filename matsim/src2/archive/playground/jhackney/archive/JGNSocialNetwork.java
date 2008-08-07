/* *********************************************************************** *
 * project: org.matsim.*
 * JGNSocialNetwork.java
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

import java.util.ArrayList;

import org.matsim.population.Population;

import playground.jhackney.socialnet.SocialNetwork;

public class JGNSocialNetwork extends SocialNetwork {
    private String linkRemovalCondition_;

    private String linkStrengthAlgorithm_;

    public JGNSocialNetwork(Population plans) {
	super(plans);
	setupIter=10;
	// TODO test the parameters and construct a Jin Girvan Newman social
	// network with a link probability between agents of pBernoulli and a
	// probability of friend-of-friend interaction pFoF:
	// Parameter pBernoulli and p Friend of Friend, distance between homes
	// if "geo" param
	System.out.println(this.getClass() + " is not written yet.");
    }

    public void generateLinks(int iteration) {
	// TODO Auto-generated method stub
	System.out.println("*** " + this.getClass() + ".addLinks is not written yet.");
    }

    @Override
		public ArrayList getLinks() {
	// TODO Auto-generated method stub
	return null;
    }

    public void removeLinks() {
	// TODO Auto-generated method stub
	
    }

    // public void removeLinks() {
    // // TODO Auto-generated method stub
    // // Establish the link removal policy from config parameters and call
    // // method
    // linkRemovalCondition_ = Gbl.getConfig().findParam(Gbl.getConfig().SN,
    // Gbl.getConfig().SN_LINKREMOVALALGO);
    // System.out.println("*** "+this.getClass() + ".removeLinks() " +
        // linkRemovalCondition_);
    // }
    //


}
