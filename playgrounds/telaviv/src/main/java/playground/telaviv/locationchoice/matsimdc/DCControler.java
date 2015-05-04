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
import org.matsim.contrib.locationchoice.bestresponse.DestinationChoiceInitializer;
import org.matsim.contrib.locationchoice.facilityload.FacilitiesLoadCalculator;
import org.matsim.core.controler.Controler;

public class DCControler extends Controler {
	
	private DestinationChoiceBestResponseContext dcContext;
	
	
	public DCControler(final String[] args) {
		super(args);	
		this.setOverwriteFiles(true);
		this.init();
	}
	 
	
	private void init() {
		/*
		 * would be muuuuch nicer to have this in DestinationChoiceInitializer, but startupListeners are called after corelisteners are called
		 * -> scoringFunctionFactory cannot be replaced
		 */
		this.dcContext = new DestinationChoiceBestResponseContext(super.getScenario());	
		/* 
		 * add ScoringFunctionFactory to controler
		 *  in this way scoringFunction does not need to create new, identical k-vals by itself    
		 */
  		DCScoringFunctionFactory dcScoringFunctionFactory = new DCScoringFunctionFactory(this.getConfig(), this, this.dcContext); 	
		super.setScoringFunctionFactory(dcScoringFunctionFactory);	
		
		this.loadMyControlerListeners();
	}
	
	private void loadMyControlerListeners() {
		this.dcContext.init(); // this is an ugly hack, but I somehow need to get the scoring function + context into the controler

		this.addControlerListener(new DestinationChoiceInitializer(this.dcContext));
		
		if (Double.parseDouble(super.getConfig().findParam("locationchoice", "restraintFcnExp")) > 0.0 &&
				Double.parseDouble(super.getConfig().findParam("locationchoice", "restraintFcnFactor")) > 0.0) {		
					this.addControlerListener(new FacilitiesLoadCalculator(this.dcContext.getFacilityPenalties()));
				}
		
//		super.loadControlerListeners();
	}
}
