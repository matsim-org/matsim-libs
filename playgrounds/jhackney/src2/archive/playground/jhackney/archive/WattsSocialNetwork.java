/* *********************************************************************** *
 * project: org.matsim.*
 * WattsSocialNetwork.java
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
import java.util.Iterator;

import org.matsim.population.Population;

import playground.jhackney.socialnet.SocialNetEdge;
import playground.jhackney.socialnet.SocialNetwork;

public class WattsSocialNetwork extends SocialNetwork {
//	private String linkRemovalCondition_;
//	private String linkStrengthAlgorithm_;
	public WattsSocialNetwork(Population plans){
		super(plans);
		setupIter=2;
		// TODO test the parameters and construct a Watts (1999) small world
		// social
		// network with a link probability between agents and average degree (?)
		// Parameters p, z, distance between homes if "geo" param
		System.out.println(this.getClass()
				+ " is not written yet.");
	}
	public void generateLinks(int iteration) {
		// TODO Auto-generated method stub
		System.out.println("*** "+this.getClass()
				+ ".addLinks is not written yet.");			
	}

	@Override
	public ArrayList<SocialNetEdge> getLinks() {
		// TODO Auto-generated method stub
		return null;
	}

	public Iterator iterator() {
		// TODO Auto-generated method stub
		return null;
	}
	public void removeLinks() {
	    // TODO Auto-generated method stub
	    
	}

//	public void removeLinks() {
//		// TODO Auto-generated method stub
//
//		// Establish the link removal policy from config parameters and call
//		// method
//		linkRemovalCondition_ = Gbl.getConfig().findParam(Gbl.getConfig().SN,
//				Gbl.getConfig().SN_LINKREMOVALALGO);
//		System.out.println("*** "+this.getClass() + ".removeLinks() " + linkRemovalCondition_);		
//	}
//
//	public void updateLinkStrength() {
//		// TODO Auto-generated method stub
//		// Establish the link strength function from config parameters and call
//		// method (move this to a method that is called from each of the construction
//		// algorithms).
//		linkStrengthAlgorithm_ = Gbl.getConfig().findParam(Gbl.getConfig().SN,
//				Gbl.getConfig().SN_LINKSTRENGTHALGO);
//		System.out
//				.println("*** "+this.getClass() + ".updateLinkStrength() " + linkStrengthAlgorithm_);		
//	}

}
