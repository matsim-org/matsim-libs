/* project: org.matsim.*
 * LCControler.java
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

package playground.ciarif.retailers;

import java.util.TreeMap;
import org.matsim.basic.v01.Id;
import org.matsim.controler.Controler;
import org.matsim.locationchoice.facilityload.FacilityPenalty;


public class RetailersControler extends Controler {
	
	private TreeMap<Id, NewRetailerLocation> newRetailersLocations;
	
	public RetailersControler(final String[] args) {
		super(args);
		
		this.newRetailersLocations = new TreeMap<Id, NewRetailerLocation>();
		this.addControlerListener(new RetailersLocationListener(this.newRetailersLocations));		
	}

   
    public static void main (final String[] args) { 
    	RetailersControler controler = new RetailersControler(args);
    	controler.run();
    }
}
