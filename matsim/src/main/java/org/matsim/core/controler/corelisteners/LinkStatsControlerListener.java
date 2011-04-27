/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package org.matsim.core.controler.corelisteners;

import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.controler.listener.IterationEndsListener;

/**
 * @author mrieser
 */
public class LinkStatsControlerListener implements AfterMobsimListener, IterationEndsListener {
	
	@Override
	public void notifyAfterMobsim(AfterMobsimEvent event) {
		Controler controler = event.getControler();
		int iteration = event.getIteration();
		
		if (((iteration % 10 == 0) && (iteration > event.getControler().getFirstIteration())) || (iteration % 10 >= 6)) {
			controler.getLinkStats().addData(controler.getVolumes(), controler.getTravelTimeCalculator());
		}

		if ((iteration % 10 == 0) && (iteration > event.getControler().getFirstIteration())) {
			controler.getLinkStats().writeFile(event.getControler().getControlerIO().getIterationFilename(iteration, Controler.FILENAME_LINKSTATS));
		}
	}

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		if ((event.getIteration() % 10 == 0) && (event.getIteration() > event.getControler().getFirstIteration())) {
			event.getControler().getLinkStats().reset();
		}
	}

}
