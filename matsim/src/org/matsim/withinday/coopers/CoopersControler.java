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

import org.matsim.config.Config;
import org.matsim.controler.events.StartupEvent;
import org.matsim.controler.listener.StartupListener;
import org.matsim.utils.vis.netvis.NetVis;
import org.matsim.withinday.WithindayControler;
/**
 * @author dgrether
 */
public class CoopersControler extends WithindayControler {

	public CoopersControler(final String[] args) {
		super(args);
		this.addControlerListener(new CoopersControlerListener());
	}

	public CoopersControler(String string) {
		this(new String[]{string});
	}

	public CoopersControler(Config config) {
		super(config);
		this.addControlerListener(new CoopersControlerListener());
	}

	public class CoopersControlerListener implements StartupListener {
		public void notifyStartup(StartupEvent e) {
			CoopersControler.this.factory = new CoopersAgentLogicFactory(CoopersControler.this.network, CoopersControler.this.config.charyparNagelScoring(), CoopersControler.this.trafficManagement.getVDSSigns());
		}
	}



	public static void main(final String[] args) {
		CoopersControler c = new CoopersControler(args);
		c.run();
		// Visualize
		String[] visargs = {"./output/ITERS/it.0/Snapshot"};
		NetVis.main(visargs);
	}
}
