/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package playground.jjoubert.roadpricing.senozon;

import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.gbl.Gbl;

/**
 * I wanted to make loadCoreListeners final and thus retrofitted this class so that the one test which covers it
 * still works.  A starting point for a better design is, in my view, in GautengControler.  kai, feb'13
 *
 */
public class SanralControler {
	
	Controler controler ;

	public SanralControler(String configFileName) {
//		super(configFileName);
//		this.addCoreControlerListener(new SanralRoadPricing());
//		// adding it as core listener will _always_ call it before the regular listeners
		
		throw new RuntimeException( Gbl.CONTROLER_IS_NOW_FINAL ) ;
	}

	public SanralControler(Config config) {
//		super(config);
//		this.addCoreControlerListener(new SanralRoadPricing());
//		// adding it as core listener will _always_ call it before the regular listeners

		throw new RuntimeException( Gbl.CONTROLER_IS_NOW_FINAL ) ;
	}


	// the following is what was there:
	
//	@Override
//	protected void loadCoreListeners() {
//		super.loadCoreListeners();
//		// add custom road pricing listener after all others, so it will be loaded first!
//		this.addControlerListener(new SanralRoadPricing());
//	}
	
	// for information: the calling sequence in AbstractController is as follows

//	protected final void run(Config config) {
//		loadCoreListeners(); // --> calls loadControlerListeners() 
//		this.controlerListenerManager.fireControlerStartupEvent();
//		checkConfigConsistencyAndWriteToLog(config, "config dump before iterations start" ) ;
//		prepareForSim();
//		doIterations(config.controler().getFirstIteration(), config.global().getRandomSeed());
//		shutdown(false);
//	}
	
}
