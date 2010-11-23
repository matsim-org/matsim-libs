/* *********************************************************************** *
 * project: org.matsim.*
 * CottbusOriginalRunner
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
package playground.dgrether.signalsystems.cottbus;

import org.apache.log4j.Logger;
import org.matsim.core.api.experimental.events.AgentStuckEvent;
import org.matsim.core.api.experimental.events.handler.AgentStuckEventHandler;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.StartupListener;

import playground.dgrether.DgPaths;


/**
 * @author dgrether
 *
 */
public class CottbusOriginalRunner implements AgentStuckEventHandler, StartupListener{

	private static final Logger log = Logger.getLogger(CottbusOriginalRunner.class);
	private static final String config = DgPaths.STUDIESDG + "cottbus/originaldaten/config_dg.xml";
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Controler c = new Controler(config);
		c.setOverwriteFiles(true);
		c.addControlerListener(new CottbusOriginalRunner());
		c.run();
	}

	@Override
	public void handleEvent(AgentStuckEvent event) {
		log.error("stuck event for agent: " + event.getPersonId() + " on link " + event.getLinkId());
		
	}

	@Override
	public void reset(int iteration) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void notifyStartup(StartupEvent e) {
		e.getControler().getEvents().addHandler(this);
	}

}
