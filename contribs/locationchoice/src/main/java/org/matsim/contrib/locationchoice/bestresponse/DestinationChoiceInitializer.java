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
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.utils.objectattributes.ObjectAttributes;


/*
 * Listener for inclusion of bestreply lc, very similar to roadpricing
 * no further coding should be required 
 */
@Deprecated // (I think)
public class DestinationChoiceInitializer implements StartupListener {
	// I think that this was an attempt to bunde the frozen epsilon material.  But it was not used in most of the examples/test cases, so it was not visible.  I have now
	// achieved something similar by moving all ini into DestinationChoiceContext.  What I don't have there is the distance stats listeners!

	private DestinationChoiceContext dcContext;
	private static final Logger log = Logger.getLogger(DestinationChoiceInitializer.class);
	
	
	public DestinationChoiceInitializer(DestinationChoiceContext lcContext) {
		this.dcContext = lcContext;
	}
	

	@Override
	public void notifyStartup(StartupEvent event) {
		// yyyyyy I am fairly sure that this will now not work since I have moved the registration of the scenario elements into DestinationChoiceContext, and this here will
		// try to register them again.  kai, mar'19

		MatsimServices controler = event.getServices();
  				  		
  		// compute or read maxDCScore but do not add it to the context:
  		// context can then be given to scoring classes both during regular scoring and in pre-processing 
  		ReadOrComputeMaxDCScore computer = new ReadOrComputeMaxDCScore(dcContext);
        computer.readOrCreateMaxDCScore( dcContext.kValsAreRead() );
		ObjectAttributes personsMaxDCScoreUnscaled = computer.getPersonsMaxEpsUnscaled();
  		
  		for (String actType : this.dcContext.getFlexibleTypes()) {
  			controler.addControlerListener(new DistanceStats(controler.getConfig(), "best", actType, dcContext.getConverter(), TransportMode.car));
  			controler.addControlerListener(new DistanceStats(controler.getConfig(), "best", actType, dcContext.getConverter(), TransportMode.pt));
  			controler.addControlerListener(new DistanceStats(controler.getConfig(), "best", actType, dcContext.getConverter(), TransportMode.bike));
  			controler.addControlerListener(new DistanceStats(controler.getConfig(), "best", actType, dcContext.getConverter(), TransportMode.walk));
  			controler.addControlerListener(new DistanceStats(controler.getConfig(), "best", actType, dcContext.getConverter(), TransportMode.other));
  			controler.addControlerListener(new DistanceStats(controler.getConfig(), "best", actType, dcContext.getConverter(), TransportMode.ride));
  			controler.addControlerListener(new DistanceStats(controler.getConfig(), "best", actType, dcContext.getConverter(), TransportMode.transit_walk));
  		}		
		MaxDCScoreWrapper dcScore = new MaxDCScoreWrapper();
		dcScore.setPersonsMaxDCScoreUnscaled(personsMaxDCScoreUnscaled);
		controler.getScenario().addScenarioElement(DestinationChoiceContext.ELEMENT_NAME, dcContext);
		controler.getScenario().addScenarioElement(MaxDCScoreWrapper.ELEMENT_NAME, dcScore);
			
		log.info("dc initialized");
	}	
}
