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

package playground.jhackney.controler;

import org.apache.log4j.Logger;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.knowledges.Knowledges;

import playground.jhackney.SocNetConfigGroup;
import playground.jhackney.algorithms.InitializeKnowledge;

public class StandardControlerListener implements StartupListener {

	private Controler controler = null;
	private final Logger log = Logger.getLogger(StandardControlerListener.class);
	private Knowledges knowledges;

	public void notifyStartup(final StartupEvent event) {
		this.controler = event.getControler();
		this.knowledges = (controler.getScenario()).getKnowledges();
		// Complete the world to make sure that the layers all have relevant mapping rules
//		new WorldConnectLocations().run(Gbl.getWorld());

		this.log.info(" Initializing agent knowledge about geography ...");
		initializeKnowledge();
		this.log.info("... done");

	}
	protected void initializeKnowledge() {
		new InitializeKnowledge(this.controler.getPopulation(), this.controler.getFacilities(), this.knowledges, this.controler.getNetwork(), (SocNetConfigGroup) this.controler.getConfig().getModule(SocNetConfigGroup.GROUP_NAME));
	}
}
