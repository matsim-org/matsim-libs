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

import org.matsim.controler.Controler;

public class RetailersControler {
	
    public static void main (final String[] args) { 
    	Controler controler = new Controler(args);
    	//controler.addControlerListener(new RetailersLocationListener("../../output/output_retailers.txt"));
    	controler.addControlerListener(new RetailersLocationListener("output/triangle/output_retailers.txt", "MaxLinkRetailerStrategy", 2)); //try to put it in the config
//    	controler.addControlerListener(new RetailersLocationListener()); //try to put it in the config
    	controler.run();
    }
}
