/* *********************************************************************** *
 * project: org.matsim.*
 * KmlNetworkWriter.java
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
package playground.dgrether.cmcf;

import org.apache.log4j.Logger;
import org.matsim.config.Config;
import org.matsim.controler.Controler;
import org.matsim.controler.events.StartupEvent;
import org.matsim.controler.listener.StartupListener;
import org.matsim.run.OTFVis;
import org.matsim.trafficmonitoring.LinkSensorManager;
import org.matsim.trafficmonitoring.LinkTravelTimeCounter;


/**
 * @author dgrether
 *
 */
public class CMCFRunner {

	
	private static final Logger log = Logger.getLogger(CMCFRunner.class);
	
	public CMCFRunner() {
	
		final LinkSensorManager lsm = new LinkSensorManager();
		lsm.addLinkSensor("4");
		
		Controler controler = new Controler(CMCFScenarioGenerator.configOut);
		controler.setOverwriteFiles(true);
		controler.addControlerListener(new StartupListener() {
			public void notifyStartup(StartupEvent e) {
				e.getControler().getEvents().addHandler(lsm);
				LinkTravelTimeCounter.init(e.getControler().getEvents(), e.getControler().getNetwork().getLinks().size());
			}});
		
		controler.run();
		
		log.info("traffic on link 4: " + lsm.getLinkTraffic("4"));
		log.info("Last travel time on 4: " + LinkTravelTimeCounter.getInstance().getLastLinkTravelTime("4"));
		
		Config config = controler.getConfig();
		
		String[] args = {config.controler().getOutputDirectory() + "/ITERS/it." + config.controler().getLastIteration() + "/" + config.controler().getLastIteration() + ".otfvis.mvi"};
		
		OTFVis.main(args);
	}
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		new CMCFRunner();
		
	}

}
