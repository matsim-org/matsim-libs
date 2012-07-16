/* *********************************************************************** *
 * project: kai
 * MyControlerListener.java
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

package playground.kai.run;

import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.controler.listener.StartupListener;

/**
 * @author nagel
 *
 */
public class KaiAnalysisListener implements StartupListener, AfterMobsimListener {
	
	playground.kai.analysis.MyCalcLegTimes calcLegTimes = null ;
	
	@Override
	public void notifyStartup(StartupEvent event) {
		
		this.calcLegTimes = new playground.kai.analysis.MyCalcLegTimes( event.getControler().getScenario() ) ;
		event.getControler().getEvents().addHandler( this.calcLegTimes ) ;

	}

	@Override
	public void notifyAfterMobsim(AfterMobsimEvent event) {

		int iteration = event.getIteration() ;

		this.calcLegTimes.writeStats(event.getControler().getControlerIO().getIterationFilename(iteration, "stats_"));

//		Logger.getLogger(this.getClass()).info("[" + iteration + "] average trip leg duration is: " 
//				+ (int) this.calcLegTimes.getAverageTripDuration()
//				+ " seconds = " + Time.writeTime(this.calcLegTimes.getAverageTripDuration(), Time.TIMEFORMAT_HHMMSS));

		// trips are from "true" activity to "true" activity.  legs may also go
		// from/to ptInteraction activity.  This, in my opinion "legs" is the correct (matsim) term
		// kai, jul'11

	}

}

