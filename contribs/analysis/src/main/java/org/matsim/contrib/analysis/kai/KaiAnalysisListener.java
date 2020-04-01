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

package org.matsim.contrib.analysis.kai;

import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.controler.listener.StartupListener;

/**
 * 
 * 
 * @author nagel
 *
 */
public class KaiAnalysisListener implements StartupListener, IterationEndsListener, ShutdownListener{
	// NOTE: My excel opens tab-separated txt files directly (from the command line).  It does not do this with comma-separated or semicolon-separated.
	// So tab-separated is the way to go. kai, sep'13
	
	public static final class Module extends AbstractModule {
		@Override public void install() {
			this.addControlerListenerBinding().to( KaiAnalysisListener.class );
		}
	}
	
	private KNAnalysisEventsHandler calcLegTimes = null ;
	
	// the default constructor can be injected even without annotation!  kai, may'18
	
	@Override
	public void notifyStartup(StartupEvent event) {
		
		this.calcLegTimes = new KNAnalysisEventsHandler( event.getServices().getScenario() ) ;
		event.getServices().getEvents().addHandler( this.calcLegTimes ) ;

	}

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		// moved this to iteration end since I also want to analyze population scores. kai, mar'14
		// (... which don't exist at shutdown time????)

		int iteration = event.getIteration() ;

		this.calcLegTimes.writeStats(event.getServices().getControlerIO().getIterationFilename(iteration, "stats_"));

	}

	@Override
	public void notifyShutdown( ShutdownEvent event ){

		this.calcLegTimes.writeStats(event.getServices().getControlerIO().getOutputFilename( "stats_") );

	}

}

