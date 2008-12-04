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
import org.matsim.facilities.Facilities;
import org.matsim.facilities.Facility;

public class RetailersControler extends Controler {
	
	private Retailers retailersToBeRelocated;
	private Facilities facilities;
	
	public RetailersControler(final String[] args) {
		super(args);
		
		this.facilities = super.getFacilities();
		this.retailersToBeRelocated = new Retailers();
		this.addControlerListener(new RetailersLocationListener(this.retailersToBeRelocated));		
	}

   
    public static void main (final String[] args) { 
    	RetailersControler controler = new RetailersControler(args);
    	controler.run();
    }
}
