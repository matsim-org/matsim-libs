/* *********************************************************************** *
 * project: org.matsim.*
 * LocationMutatorwChoiceSetIncremental.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package playground.anhorni.locationchoice.depr;



import org.apache.log4j.Logger;
import org.matsim.controler.Controler;
import org.matsim.locationchoice.constrained.LocationMutatorwChoiceSet;
import org.matsim.locationchoice.constrained.SubChain;
import org.matsim.network.NetworkLayer;


public class LocationMutatorwChoiceSetIncremental extends LocationMutatorwChoiceSet {
	
	private static final Logger log = Logger.getLogger(LocationMutatorwChoiceSetIncremental.class);
	
	public LocationMutatorwChoiceSetIncremental(final NetworkLayer network, Controler controler) {
		super(network, controler);
	}
	
	@Override
	protected int handleSubChain(SubChain subChain, double speed, int trialNr) {
		log.info("handleSubChain");
		return 0;
	}
}
