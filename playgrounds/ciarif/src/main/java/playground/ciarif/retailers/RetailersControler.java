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

import org.matsim.core.controler.Controler;
import org.matsim.core.gbl.Gbl;

import playground.meisterk.kti.controler.KTIControler;

//public class RetailersControler extends Controler {
public class RetailersControler extends KTIControler {

	public RetailersControler(String[] args) {
		super(args);
		this.loadMyControlerListeners();
		throw new RuntimeException(Gbl.CREATE_ROUTING_ALGORITHM_WARNING_MESSAGE) ;
	}

	private void loadMyControlerListeners() {
//		super.loadControlerListeners();
		this.addControlerListener(new RetailersLocationListener());
	}
	
    public static void main (final String[] args) { 
    	Controler controler = new RetailersControler(args);
    	controler.run();
    }
}

