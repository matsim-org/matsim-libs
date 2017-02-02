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

package org.matsim.contrib.minibus.stats;

import java.io.File;
import java.util.HashMap;
import java.util.Set;
import java.util.TreeSet;

import javax.inject.Inject;

import org.apache.log4j.Logger;
import org.matsim.contrib.minibus.PConfigGroup;
import org.matsim.contrib.minibus.PConstants;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.controler.listener.StartupListener;

/**
 * GexfPStat stats for all lines. Each line is written separately to file
 *  
 * @author aneumann
 *
 */
final class Line2GexfPStat implements StartupListener, IterationEndsListener, ShutdownListener{
	
	private static final Logger log = Logger.getLogger(Line2GexfPStat.class);

    private CountPPaxHandler globalPaxHandler;
	private CountPVehHandler vehHandler;
	
	private final PConfigGroup pConfig;
	private final HashMap<String, SimpleGexfPStat> lineId2GexfPStat;
	private Set<String> lastLineIds;

	@Inject Line2GexfPStat(Config config){
		this.pConfig = ConfigUtils.addOrGetModule(config,PConfigGroup.class) ;
		
		this.lineId2GexfPStat = new HashMap<>();
		
		this.lastLineIds = new TreeSet<>();
		
		if (this.pConfig.getGexfInterval() > 0) {
			log.info("enabled");
		}
		
	}

	@Override
	public void notifyStartup(StartupEvent event) {
		if (this.pConfig.getGexfInterval() > 0) {
			this.globalPaxHandler = new CountPPaxHandler(this.pConfig.getPIdentifier());
			event.getServices().getEvents().addHandler(this.globalPaxHandler);

			this.vehHandler = new CountPVehHandler(this.pConfig.getPIdentifier());
			event.getServices().getEvents().addHandler(this.vehHandler);
		}
	}

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		if (this.pConfig.getGexfInterval() > 0) {
            String gexfOutputDir = event.getServices().getControlerIO().getOutputPath() + PConstants.statsOutputFolder + Line2GexfPStat.class.getSimpleName() + "/";
			try {
				new File(gexfOutputDir).mkdir();
			} catch (Exception e) {
				// TODO: handle exception
			}

			Set<String> currentLineIds = this.globalPaxHandler.getLineIds();

			for (String lineId : currentLineIds) {
				if (this.lineId2GexfPStat.get(lineId) == null) {
					// new line - create new gexf
					SimpleGexfPStat gexf = new SimpleGexfPStat(this.pConfig, lineId, gexfOutputDir);
                    gexf.notifyStartup(event.getServices().getScenario().getNetwork(), this.globalPaxHandler, this.vehHandler);
					this.lineId2GexfPStat.put(lineId, gexf);
				}

				// for all active gexf call
				this.lineId2GexfPStat.get(lineId).notifyIterationEnds(event);
			}
			
			for (String lineId : lastLineIds) {
				if (!currentLineIds.contains(lineId)) {
					// this line died - terminate
					this.lineId2GexfPStat.get(lineId).notifyShutdown(event.getIteration());
					this.lineId2GexfPStat.remove(lineId);
				}
			}
			
			this.lastLineIds = currentLineIds;
		}
	}

	@Override
	public void notifyShutdown(ShutdownEvent event) {
		// call shutdown for all remaining gexf
		for (SimpleGexfPStat gexf : this.lineId2GexfPStat.values()) {
			gexf.notifyShutdown(event.getServices().getConfig().controler().getLastIteration());
		}
		log.info("terminated the last " + this.lineId2GexfPStat.values().size() + " gexf handlers...");
	}
}