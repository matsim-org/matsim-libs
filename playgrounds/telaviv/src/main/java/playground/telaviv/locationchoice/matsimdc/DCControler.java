/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package playground.telaviv.locationchoice.matsimdc;

import org.matsim.contrib.locationchoice.bestresponse.DestinationChoiceBestResponseContext;

public class DCControler extends org.matsim.contrib.locationchoice.DCControler {
	
	private DestinationChoiceBestResponseContext dcContext;
				
	public DCControler(final String[] args) {
		super(args);	
	}
	
	public static void main (final String[] args) { 
		DCControler controler = new DCControler(args);
		controler.setOverwriteFiles(true);
		controler.init();
    	controler.run();
    }  
	
	private void init() {
		this.dcContext = new DestinationChoiceBestResponseContext(super.getScenario());	
  		DCScoringFunctionFactory dcScoringFunctionFactory = new DCScoringFunctionFactory(this.getConfig(), this, this.dcContext); 	
		super.setScoringFunctionFactory(dcScoringFunctionFactory);		
	}
	
	protected void loadControlerListeners() {
		super.loadControlerListeners();
	}
}
