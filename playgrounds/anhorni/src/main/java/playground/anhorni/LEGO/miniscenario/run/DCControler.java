/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.anhorni.LEGO.miniscenario.run;

import org.matsim.contrib.locationchoice.bestresponse.LocationChoiceInitializer;
import org.matsim.contrib.locationchoice.facilityload.FacilitiesLoadCalculator;
import org.matsim.core.controler.Controler;

public class DCControler extends Controler {
				
	public DCControler(final String[] args) {
		super(args);	
	}
	
	public static void main (final String[] args) { 
		DCControler controler = new DCControler(args);
		controler.setOverwriteFiles(true);
    	controler.run();
    }  
	
	protected void loadControlerListeners() {
		this.addControlerListener(new LocationChoiceInitializer());
		super.loadControlerListeners();
	}
}
