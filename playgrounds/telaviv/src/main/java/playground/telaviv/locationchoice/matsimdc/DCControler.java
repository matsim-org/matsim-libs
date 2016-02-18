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


import org.matsim.contrib.locationchoice.DestinationChoiceConfigGroup;
import org.matsim.contrib.locationchoice.bestresponse.DestinationChoiceBestResponseContext;
import org.matsim.contrib.locationchoice.bestresponse.DestinationChoiceInitializer;
import org.matsim.contrib.locationchoice.facilityload.FacilitiesLoadCalculator;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;

public class DCControler {

	private final Controler controler;
	private DestinationChoiceBestResponseContext dcContext;
	
	
	public DCControler(final String[] args) {
		this.controler = new Controler(args);
		
		DestinationChoiceConfigGroup dccg = new DestinationChoiceConfigGroup();
		this.controler.getConfig().addModule(dccg);
		
		controler.getConfig().controler().setOverwriteFileSetting(
				OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);
		/*
		 * would be muuuuch nicer to have this in DestinationChoiceInitializer, but startupListeners are called after corelisteners are called
		 * -> scoringFunctionFactory cannot be replaced
		 */
		this.dcContext = new DestinationChoiceBestResponseContext(controler.getScenario());
		/*
		 * add ScoringFunctionFactory to controler
		 *  in this way scoringFunction does not need to create new, identical k-vals by itself
		 */
		DCScoringFunctionFactory dcScoringFunctionFactory = new DCScoringFunctionFactory(controler.getConfig(), controler, this.dcContext);
		controler.setScoringFunctionFactory(dcScoringFunctionFactory);

		this.dcContext.init(); // this is an ugly hack, but I somehow need to get the scoring function + context into the controler

		controler.addControlerListener(new DestinationChoiceInitializer(this.dcContext));

		if (dccg.getRestraintFcnExp() > 0.0 &&
				dccg.getRestraintFcnFactor() > 0.0) {
			controler.addControlerListener(new FacilitiesLoadCalculator(this.dcContext.getFacilityPenalties()));
				}

//		super.loadControlerListeners();
	}


}
