/* *********************************************************************** *
 * project: org.matsim.*
 * CoopersControler.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package org.matsim.withinday.coopers;

import org.matsim.utils.vis.netvis.NetVis;
import org.matsim.withinday.WithindayControler;

/**
 * @author dgrether
 */
public class CoopersControler extends WithindayControler {

	public CoopersControler(final String[] args) {
		super(args);
	}

	/* TODO [DG] please verify: In the original code, this method was called "setupIteration(int)".
	 * But I don't see the reason, why there is a need for a new factory in every iteration, thus
	 * I renamed it to "setup()". Iff it should be called every iteration, a custom ControlerListener
	 * has to be implemented that implements ControlerIterationStratsListener. -marcel/18jan2008
	 */
	@Override
	protected void setup() {
		super.setup();
		this.factory = new CoopersAgentLogicFactory(this.network, this.config.charyparNagelScoring(), this.trafficManagement.getVDSSigns());
	}

	@Override
	public void afterSimStep(final double time) {
		super.afterSimStep(time);
	}

/* TODO [DG] pleaes verify: The new Controler calls events.resetHandlers(int) at the *start*
 * of each iteration. Thus I assume that the following code is no longer required. Please
 * verify and remove it afterwards if so.   -marce/18.jan2008 */
//	@Override
//	protected void finishIteration(final int iteration) {
//		super.finishIteration(iteration);
//		this.events.resetHandlers(iteration);
//	}

	public static void main(final String[] args) {
		CoopersControler c = new CoopersControler(args);
		c.setOverwriteFiles(true);
		c.run();

		// Visulize
		String[] visargs = {"./output/ITERS/it.0/Snapshot"};
		NetVis.main(visargs);
	}

}
