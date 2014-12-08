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

package org.matsim.contrib.locationchoice;

import org.matsim.contrib.locationchoice.bestresponse.DestinationChoiceBestResponseContext;
import org.matsim.contrib.locationchoice.bestresponse.DestinationChoiceInitializer;
import org.matsim.contrib.locationchoice.bestresponse.scoring.DCScoringFunctionFactory;
import org.matsim.contrib.locationchoice.facilityload.FacilitiesLoadCalculator;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;

@Deprecated // very old syntax; loot at LocationChoiceIntegrationTest instead. kai/mz, oct'14
public class DCControler extends Controler {
	
	private DestinationChoiceBestResponseContext dcContext;
	
				
	@Deprecated // very old syntax; loot at LocationChoiceIntegrationTest instead. kai/mz, oct'14
	public DCControler(final String[] args) {
		super(args);	
	}
	
	@Deprecated // very old syntax; loot at LocationChoiceIntegrationTest instead. kai/mz, oct'14
	public DCControler(final Config config) {
		super(config);	
	}
	
	@Deprecated // very old syntax; loot at LocationChoiceIntegrationTest instead. kai/mz, oct'14
	public static void main (final String[] args) { 
		
		Config config = ConfigUtils.loadConfig(args[0], new DestinationChoiceConfigGroup() ) ;
		
		DCControler controler = new DCControler(config);
		controler.setOverwriteFiles(true);
		controler.init();
    	controler.run();
    }  
	
	@Deprecated // very old syntax; loot at LocationChoiceIntegrationTest instead. kai/mz, oct'14
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
		
		if (!this.getConfig().findParam("locationchoice", "prefsFile").equals("null") &&
				!this.getConfig().facilities().getInputFile().equals("null")) {
			dcScoringFunctionFactory.setUsingConfigParamsForScoring(false);
		} else {
			dcScoringFunctionFactory.setUsingConfigParamsForScoring(true);
			log.info("external prefs are not used for scoring!");
		}		
	}
	
	@Deprecated // very old syntax; loot at LocationChoiceIntegrationTest instead. kai/mz, oct'14
	protected void loadControlerListeners() {
		this.dcContext.init(); // this is an ugly hack, but I somehow need to get the scoring function + context into the controler

		this.addControlerListener(new DestinationChoiceInitializer(this.dcContext));
		
		if (Double.parseDouble(this.getConfig().findParam("locationchoice", "restraintFcnExp")) > 0.0 &&
				Double.parseDouble(this.getConfig().findParam("locationchoice", "restraintFcnFactor")) > 0.0) {		
					this.addControlerListener(new FacilitiesLoadCalculator(this.dcContext.getFacilityPenalties()));
				}
		
		super.loadControlerListeners();
	}
}
