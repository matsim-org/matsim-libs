/* *********************************************************************** *
 * project: org.matsim.*
 * EventsHandlingWithCycleControler.java
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
package playground.yu.analysis;

import org.matsim.core.controler.Controler;
import org.matsim.core.controler.corelisteners.PlansDumping;
import org.matsim.core.controler.corelisteners.PlansReplanning;
import org.matsim.core.controler.corelisteners.PlansScoring;

/**
 * @author yu
 * 
 */
public class EventsHandlingWithCycleControler extends Controler {
	private int linkStatsCycle = 1;

	public EventsHandlingWithCycleControler(String configFilename,
			int linkStatsCycle) {
		super(configFilename);
		this.linkStatsCycle = linkStatsCycle;
	}

	/**
	 * @param args
	 */
	public EventsHandlingWithCycleControler(String configFilename) {
		super(configFilename);
	}

	@Override
	protected void loadCoreListeners() {
		/*
		 * The order how the listeners are added is very important! As
		 * dependencies between different listeners exist or listeners may read
		 * and write to common variables, the order is important. Example: The
		 * RoadPricing-Listener modifies the scoringFunctionFactory, which in
		 * turn is used by the PlansScoring-Listener. Note that the execution
		 * order is contrary to the order the listeners are added to the list.
		 */

		addCoreControlerListener(new CoreControlerListener());

		// the default handling of plans
		addCoreControlerListener(new PlansScoring());

		// load road pricing, if requested
		// if (this.config.scenario().isUseRoadpricing()) {
		// this.roadPricing = new RoadPricing();
		// this.addCoreControlerListener(this.roadPricing);
		// }

		addCoreControlerListener(new PlansReplanning());
		addCoreControlerListener(new PlansDumping());

		addCoreControlerListener(new EventsHandlingWithCycle(events,
				linkStatsCycle));
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Controler ctl = new EventsHandlingWithCycleControler(args[0]);
		ctl.setOverwriteFiles(true);
		ctl.run();
	}

}
