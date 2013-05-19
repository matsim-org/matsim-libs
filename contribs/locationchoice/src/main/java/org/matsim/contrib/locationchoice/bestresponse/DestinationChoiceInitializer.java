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

package org.matsim.contrib.locationchoice.bestresponse;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.contrib.locationchoice.analysis.DistanceStats;
import org.matsim.contrib.locationchoice.bestresponse.preprocess.MaxDCScoreWrapper;
import org.matsim.contrib.locationchoice.bestresponse.preprocess.ReadOrComputeMaxDCScore;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.controler.Controler;
import org.matsim.utils.objectattributes.ObjectAttributes;


/*
 * Listener for inclusion of bestreply lc, very similar to roadpricing
 * no further coding should be required 
 */
public class DestinationChoiceInitializer implements StartupListener {
	private DestinationChoiceBestResponseContext dcContext;
	private ObjectAttributes personsMaxDCScoreUnscaled;
	private static final Logger log = Logger.getLogger(DestinationChoiceInitializer.class);
	
	
	public DestinationChoiceInitializer(DestinationChoiceBestResponseContext lcContext) {
		this.dcContext = lcContext;
	}
	

	@Override
	public void notifyStartup(StartupEvent event) {
		Controler controler = event.getControler();  		
  				  		
  		// compute or read maxDCScore but do not add it to the context:
  		// context can then be given to scoring classes both during regular scoring and in pre-processing 
  		ReadOrComputeMaxDCScore computer = new ReadOrComputeMaxDCScore(dcContext);
  		computer.readOrCreateMaxDCScore(controler, dcContext.kValsAreRead());
  		this.personsMaxDCScoreUnscaled = computer.getPersonsMaxEpsUnscaled();
  		 				
		controler.addControlerListener(new DistanceStats(controler.getConfig(), "best", "s", dcContext.getConverter(), TransportMode.car));
		controler.addControlerListener(new DistanceStats(controler.getConfig(), "best", "l", dcContext.getConverter(), TransportMode.car));
		controler.addControlerListener(new DistanceStats(controler.getConfig(), "best", "s", dcContext.getConverter(), TransportMode.pt));
		controler.addControlerListener(new DistanceStats(controler.getConfig(), "best", "l", dcContext.getConverter(), TransportMode.pt));
		controler.addControlerListener(new DistanceStats(controler.getConfig(), "best", "s", dcContext.getConverter(), TransportMode.bike));
		controler.addControlerListener(new DistanceStats(controler.getConfig(), "best", "l", dcContext.getConverter(), TransportMode.bike));
		controler.addControlerListener(new DistanceStats(controler.getConfig(), "best", "s", dcContext.getConverter(), TransportMode.walk));
		controler.addControlerListener(new DistanceStats(controler.getConfig(), "best", "l", dcContext.getConverter(), TransportMode.walk));
				
		MaxDCScoreWrapper dcScore = new MaxDCScoreWrapper();
		dcScore.setPersonsMaxDCScoreUnscaled(personsMaxDCScoreUnscaled);
		controler.getScenario().addScenarioElement(dcContext);
		controler.getScenario().addScenarioElement(dcScore);
			
		log.info("dc initialized");
	}	
}
