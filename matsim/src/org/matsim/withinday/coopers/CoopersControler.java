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
 *
 */
public class CoopersControler extends WithindayControler {

	@Override
	protected void setupIteration(final int iteration) {
		super.setupIteration(iteration);
		this.factory = new CoopersAgentLogicFactory(this.network, this.config.charyparNagelScoring(), this.trafficManagement.getVDSSigns());
	}




	@Override
	public void afterSimStep(final double time) {
		super.afterSimStep(time);
	}

	@Override
	protected void finishIteration(final int iteration) {
		super.finishIteration(iteration);
		this.events.resetHandlers(iteration);
	}
	/**
	 * @param args
	 */
	public static void main(final String[] args) {
		CoopersControler c = new CoopersControler();
		c.setOverwriteFiles(true);
		c.run(args);

		// Visulize
		String[] visargs = {"./output/ITERS/it.0/Snapshot"};
		NetVis.main(visargs);
	}

}
