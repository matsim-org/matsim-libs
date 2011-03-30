/* *********************************************************************** *
 * project: org.matsim.*
 * MinimizeLeftTurnsControlerListener.java
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

/**
 * 
 */
package playground.yu.replanning.reRoute.tightTurnPenalty;

import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.listener.IterationStartsListener;

import playground.yu.travelCost.SingleReRouteSelectedControler;

/**
 * @author yu
 * 
 */
public class TightTurnPenaltyControlerListener implements
		IterationStartsListener {

	@Override
	public void notifyIterationStarts(IterationStartsEvent event) {
		Controler ctl = event.getControler();
		if (event.getIteration() > ctl.getFirstIteration()) {
			ctl
					.setLeastCostPathCalculatorFactory(new DijkstraWithTightTurnPenaltyFactory());
		}
	}

	public static void main(String[] args) {
		Controler controler = new SingleReRouteSelectedControler(args[0]);
		controler.addControlerListener(new TightTurnPenaltyControlerListener());
		controler.setWriteEventsInterval(0);
		controler.setOverwriteFiles(true);
		controler.run();
	}
}
