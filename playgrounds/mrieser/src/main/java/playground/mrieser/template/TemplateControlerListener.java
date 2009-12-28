/* *********************************************************************** *
 * project: org.matsim.*
 * PluginControlerListener.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package playground.mrieser.template;

import org.apache.log4j.Logger;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.StartupListener;

public class TemplateControlerListener implements StartupListener, IterationEndsListener {

	private final static Logger log = Logger.getLogger(TemplateControlerListener.class);
	
	private final TemplateConfigGroup pluginConfig = new TemplateConfigGroup();
	private TemplateEventHandler eventHandler = null;
	
	public void notifyStartup(final StartupEvent event) {
		final Controler controler = event.getControler();

		// config
		controler.getConfig().addModule(TemplateConfigGroup.GROUP_NAME, this.pluginConfig); // too late here... but could be fixed in Config.java
		
		// event handler
		this.eventHandler = new TemplateEventHandler();
		controler.getEvents().addHandler(this.eventHandler);
		
		// plan strategy
		//// no possibility to pass values to the plan strategy. fix this somehow
		
	}

	public void notifyIterationEnds(IterationEndsEvent event) {
		log.info("number of counted events: " + this.eventHandler.getCount());
	}

}
